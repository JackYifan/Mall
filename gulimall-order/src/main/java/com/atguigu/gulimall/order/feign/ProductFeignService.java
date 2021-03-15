package com.atguigu.gulimall.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * @Author Yifan Wu
 * Date on 2021/3/15  14:34
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {


}
