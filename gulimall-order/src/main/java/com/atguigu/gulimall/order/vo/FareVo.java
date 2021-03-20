package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author Yifan Wu
 * Date on 2021/3/20  13:55
 */
@Data
public class FareVo {

    private MemberAddressVo memberAddressVO;

    private BigDecimal fare;
}
