package com.atguigu.common.to.mq;

import lombok.Data;

/**
 * @Author Yifan Wu
 * Date on 2021/3/22  22:00
 */
@Data
public class StockDetailTo {
    /**
     * id
     */
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;
    /**
     *
     */
    private Long wareId;
    /**
     * 1-锁定 2-解锁 3-扣减
     */
    private Integer lockStatus;
}
