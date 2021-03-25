package com.atguigu.common.to.mq;

import lombok.Data;

/**
 * @Author Yifan Wu
 * Date on 2021/3/22  22:01
 */
@Data
public class StockLockTo {
    /**
     * 任务号
     */
    private Long id ;

    /**
     * 任务详情
     */
    private StockDetailTo stockDetailTo;

}
