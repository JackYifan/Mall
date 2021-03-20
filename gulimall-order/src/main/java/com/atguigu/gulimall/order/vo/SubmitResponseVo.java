package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * @Author Yifan Wu
 * Date on 2021/3/18  10:31
 */
@Data
public class SubmitResponseVo {
    private OrderEntity orderEntity;
    /**
     * 状态码 0表示成功
     */
    private Integer code;
}
