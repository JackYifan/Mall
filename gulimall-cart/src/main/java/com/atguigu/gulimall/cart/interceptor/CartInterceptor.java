package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.cart.to.UserInfoTO;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 拦截器，用于判断当前购物车是否是已登录的用户的
 */
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTO> threadLocal = new ThreadLocal<>();
    /**
     * 浏览器cookie中有user-key标识用户身份，且当临时用户用账号登录后user-key不变
     * 在拦截之前处理,拦截后的一系列操作在一个线程中，可以用ThreadLocal传递数据，一个线程中共享其中的数据
     * @param request 如果登录，用户信息会存储到session中，以redis的方式
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTO userInfoTO = new UserInfoTO();//当前用户信息，已登录直接赋值，未登录就新建
        HttpSession session = request.getSession();
        MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        //如果已登录设置userId
        if(memberResponseVo!=null){
            userInfoTO.setUserId(memberResponseVo.getId());
        }
        //封装浏览器中保存的user-key
        Cookie[] cookies = request.getCookies();
        if(cookies!=null&&cookies.length>0){
            for(Cookie cookie:cookies){
                if(CartConstant.TEMP_USER_COOKIE_NAME.equals(cookie.getName())){
                    userInfoTO.setUserKey(cookie.getValue());
                    userInfoTO.setTempUser(true);
                }
            }
        }
        //当前没有user-key就分配
        if(StringUtils.isEmpty(userInfoTO.getUserKey())){
            //没有临时用户，创建临时用户
            userInfoTO.setUserKey(UUID.randomUUID().toString());
            //没有cookie的在设置号user-key在postHandler中添加cookie
        }
        threadLocal.set(userInfoTO);
        return true;
    }

    /**
     * 业务执行完成后保存cookie
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //同一线程共享数据
        UserInfoTO userInfoTO = threadLocal.get();
        if(!userInfoTO.isTempUser()){
            //临时用户是有cookie的，不用添加从而延长cookie的时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME,userInfoTO.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
