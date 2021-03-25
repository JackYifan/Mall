package com.atguigu.gulimall.order.to;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author Yifan Wu
 * Date on 2021/3/18  10:54
 */
@Data
public class OrderCreateTo {
    /**
     * 订单信息
     */
    private OrderEntity orderEntity;

    /**
     * 商品信息列表
     */
    private List<OrderItemEntity> orderItems;

    /**
     * 支付价格
     */
    private BigDecimal payPrice;

    /**
     * 优惠
     */
    private BigDecimal fare;

}
