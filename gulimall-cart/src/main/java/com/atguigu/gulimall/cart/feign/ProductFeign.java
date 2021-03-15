package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;


/**
 * 获取商品信息的feign
 */
@FeignClient("gulimall-product")
public interface ProductFeign {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R getSkuInfo(@PathVariable("skuId") Long skuId);

    @RequestMapping("/product/skusaleattrvalue/getAttrList/{skuId}")
    public List<String> getAttrList(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/skuinfo/{skuId}/price")
    public BigDecimal getPrice(@PathVariable("skuId")Long skuId);
}
