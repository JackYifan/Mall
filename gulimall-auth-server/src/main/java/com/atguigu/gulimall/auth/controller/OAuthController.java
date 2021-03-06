package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class OAuthController {

    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * 跳转到该接口并增加code参数
     * 利用code换取accessToken
     * @return
     */
    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code) throws Exception {
        //&grant_type=&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        Map<String,String> querys = new HashMap<>();
        querys.put("client_id","4148058448");
        querys.put("client_secret","12d961f7034252dd81a1ff48093125e0");
        querys.put("grant_type","authorization_code");
        querys.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/success");
        querys.put("code",code);
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com",
                "/oauth2/access_token",
                "post",
                new HashMap<>(),
                querys,
                new HashMap<>());
        if(response.getStatusLine().getStatusCode()==200){
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //远程调用,关联会员账号
            R result = memberFeignService.oauthLogin(socialUser);
            if(result.getCode()==0){
                MemberResponseVo data = result.getData("data", new TypeReference<MemberResponseVo>() {
                });
                log.info("登录成功：用户{}",data.toString());
                return "redirect:http://gulimall.com";
            }else{
                //微博登录失败
                return "redirect:http://auth.gulimall.com/login.html";
            }

        }else{
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

}
