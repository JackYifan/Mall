package com.atguigu.gulimall.order.vo;

import lombok.Data;

/**
 * @Author Yifan Wu
 * Date on 2021/3/24  22:31
 */
@Data
public class PayVo {
    private String out_trade_no; // 商户订单号 必填
    private String subject; // 订单名称 必填
    private String total_amount;  // 付款金额 必填
    private String body; // 商品描述 可空
}
