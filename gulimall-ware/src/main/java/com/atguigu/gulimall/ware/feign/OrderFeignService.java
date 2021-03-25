package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Author Yifan Wu
 * Date on 2021/3/22  23:15
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {
    @GetMapping("/order/order/status/{orderSn}")
    public R getOrderStatus(@PathVariable("orderSn") String orderSn);
}
