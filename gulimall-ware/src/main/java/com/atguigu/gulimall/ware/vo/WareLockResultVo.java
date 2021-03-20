package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * @Author Yifan Wu
 * Date on 2021/3/20  10:59
 */
@Data
public class WareLockResultVo {
    /**
     * 锁定商品的skuId
     */
    private Long skuId;
    /**
     * 锁定的数量
     */
    private Integer num;

    /**
     * 是否锁定成功
     */
    private Boolean locked;
}
