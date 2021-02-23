package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //Sku基本信息
    private SkuInfoEntity info;
    private boolean hasStock = true;

    //图片信息
    private List<SkuImagesEntity> images;
    //销售属性
    private List<SkuItemSaleAttrVo> saleAttr;

    //介绍信息
    private SpuInfoDescEntity desp;

    //分组信息
    private List<SpuItemAttrGroupVo> groupAttrs;


}
