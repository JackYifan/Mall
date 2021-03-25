package com.atguigu.gulimall.member.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Author Yifan Wu
 * Date on 2021/3/24  23:31
 */
@Controller
public class MemberWebController {

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(){
        return "orderList";
    }
}
