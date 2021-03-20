package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    /**
     * 展示订单信息并跳转结算确认页
     */
    @GetMapping("toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVO = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVO);
        return "confirm";
    }


    /**
     * 提交订单
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVO,
                              Model model,
                              RedirectAttributes redirectAttributes){
        try {
            SubmitResponseVo submitResponseVo = orderService.submitOrder(orderSubmitVO);
            if(submitResponseVo.getCode()==0) {
                model.addAttribute("submitOrderResponse", submitResponseVo);
                return "pay"; //提交成功返回付款页面
            } else{
                String msg = "下订单失败: ";
                switch (submitResponseVo.getCode()){
                    case 1 : msg += "订单信息过期, 请刷新后再次提交."; break;
                    case 2 : msg += "订单中的商品价格发生变化, 请刷新后再次提交."; break;
                    case 3 : msg += "库存锁定失败, 商品库存不足."; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade"; //提交失败重新渲染确认页，用户重新确认
            }
        } catch (NoStockException e) {
            String message = e.getMessage();
            redirectAttributes.addFlashAttribute("msg", message);
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
