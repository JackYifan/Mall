package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author Yifan Wu
 * Date on 2021/3/18  10:27
 */
@Data
public class OrderSubmitVo {
    private Long addrId;

    private Integer payType; // 支付方式

    private String orderToken; //令牌

    private BigDecimal payPrice; // 应付价格 验价

    private String note; // 订单备注
}
