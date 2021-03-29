package com.atguigu.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author Yifan Wu
 * Date on 2021/3/15  14:08
 */
@Configuration
public class FeignConfig {

    /**
     * 在Spring容器中加入requestInterceptor
     * @return
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor(){
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //通过request上下文获得所有request信息
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(!ObjectUtils.isEmpty(requestAttributes)){
                    //每次对象操作前都需要判断非空
                    HttpServletRequest request = requestAttributes.getRequest();
                    if(!ObjectUtils.isEmpty(request)){
                        //从请求头中获取cookie
                        String cookie = request.getHeader("Cookie");
                        //在新请求的请求头中设置cookie
                        requestTemplate.header("Cookie",cookie);
                    }
                }
            }
        };
    }

}
