package com.atguigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GulimallCorsConfiguration {

    /**
     * 将跨域的filter注册到容器中
     * @return
     */
    @Bean
    public CorsWebFilter corsWebFilter(){
        //CorsConfigurationSource
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //配置跨域
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");//请求头
        corsConfiguration.addAllowedMethod("*");//请求方式
        corsConfiguration.addAllowedOrigin("*");//请求来源
        corsConfiguration.setAllowCredentials(true);//允许携带cookie跨域
        //注册配置
        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }
}
