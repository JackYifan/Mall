package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.config.GulimallSessionConfig;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.to.UserInfoTO;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * 查询当前用户购物车中选中的商品
     * @return
     */
    @GetMapping("/currentUserCartItem")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItem(){
        return cartService.getCurrentUserCartItem();
    }



    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId")Long skuId,@RequestParam("num")Integer num){
        cartService.countItem(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    //deleteItem?skuId=33
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId")Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }




    /**
     * 勾选购物项
     * @param skuId
     * @param checked
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("checked") Integer checked){
        //更新redis中购物车中sku相应的item
        cartService.checkItem(skuId,checked);
        //重新查询,展示新数据
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    /**
     * 展示购物车中的所有信息
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }


    /**
     * 加入购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId")Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes){
        try {
            CartItem item = cartService.addToCart(skuId,num);
            redirectAttributes.addAttribute("skuId",skuId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 展示购物车数据，不进行增加操作
     * @return
     */
    @RequestMapping("/addToCartSuccess.html")
    public String successPage(@RequestParam("skuId") Long skuId,Model model){
        //查询购物车
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item",cartItem);
        return "success";

    }
}
