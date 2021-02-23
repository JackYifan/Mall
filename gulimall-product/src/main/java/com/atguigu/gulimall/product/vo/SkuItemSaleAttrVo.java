package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 销售属性
 */
@Data
@ToString
public class SkuItemSaleAttrVo{
    //属性名
    private Long attrId;
    private String attrName;
    //属性值
    private List<AttrValueWithSkuIdVo> attrValues;
}
