package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author Yifan Wu
 * Date on 2021/3/20  10:52
 */
@Data
public class WareStockLockVo {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 需要锁定的商品列表
     */
    private List<OrderItemVo> orderItemVos;
}
