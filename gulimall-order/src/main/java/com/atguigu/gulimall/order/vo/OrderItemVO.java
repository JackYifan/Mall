package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVO {
    private Long skuId;
    /**
     * 是否被选中
     */
    private Boolean checked;

    private String title;

    private String image;

    private List<String> skuAttrs;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    private BigDecimal weight;
}
