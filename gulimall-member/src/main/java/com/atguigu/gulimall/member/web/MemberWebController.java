package com.atguigu.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Yifan Wu
 * Date on 2021/3/24  23:31
 */
@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum, Model model){
        Map<String,Object> page = new HashMap<>();
        page.put("page",String.valueOf(pageNum));
        R result = orderFeignService.listWithItem(page);

        System.out.println(JSON.toJSONString(result));


        model.addAttribute("orders",result);
        return "orderList";
    }
}
