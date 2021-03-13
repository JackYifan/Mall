package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService ;

    /**
     * session中有用户信息直接返回首页
     * @param session
     * @return
     */
    @GetMapping("/login.html")
    public String login(HttpSession session){
        if(session.getAttribute(AuthServerConstant.LOGIN_USER)!=null){
            return "redirect:http://gulimall.com";
        }else{
            return "login";
        }

    }


    @ResponseBody
    @GetMapping("/sms/send")
    public R sendCode(String phone){

        //接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone);
        if(StringUtils.isNotEmpty(redisCode)){
            long time = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-time < 60000){
                //相同手机号发送验证码的间隔小于一分钟
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //验证码验证
        //redis中存储 sms:code:phoneNumber : code
        //5位验证码 加上添加到redis时的系统时间防刷
        String userCode = UUID.randomUUID().toString().substring(0,5);
        String code = userCode+"_"+System.currentTimeMillis();
        //存储验证码到redis并设置过期时间
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,code,10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone,userCode);
        return R.ok();


    }


    @PostMapping("/reg")
    public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        //校验vo
        if(result.hasErrors()){
            //使用map封装校验出的错误 <field,message>
            Map<String,String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField,FieldError::getDefaultMessage));
            //校验出错，重定向到注册页并显示错误
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        //验证验证码
        String code = vo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(StringUtils.isNotEmpty(redisCode)){
            //分离出redisCode中的验证码
            if(code.equals(redisCode.split("_")[0])){
                //验证通过，删除令牌
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //调用远程方法进行注册
                R registerResult = memberFeignService.register(vo);
                if(registerResult.getCode()==0){
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                }else{
                    //远程服务捕获到各种异常并返回错误信息
                    Map<String,String> errors = new HashMap<>();
                    errors.put("msg",registerResult.getData("msg",new TypeReference<String>(){}));//将data中的数据转为String
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/register.html";

                }

            }else{
                //验证码错误
                Map<String,String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/register.html";
            }
        }else{
            //redis中没有验证码,返回错误信息
            Map<String,String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }


    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        //传递的是k-v不是json
        //远程调用验证账号密码是否正确
        R loginResult = memberFeignService.login(vo);
        if(loginResult.getCode()==0){
            //将用户信息存到session中
            session.setAttribute(AuthServerConstant.LOGIN_USER,loginResult.getData(new TypeReference<MemberResponseVo>(){}));
            //正确返回商城首页
            return "redirect:http://gulimall.com";
        }else{
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",loginResult.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }


    }


}
