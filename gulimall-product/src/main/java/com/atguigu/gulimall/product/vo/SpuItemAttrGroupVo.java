package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpuItemAttrGroupVo {
    //分组名
    private String groupName;
    //该分组下的所有属性，包括属性名和属性值
    private List<Attr> attrs;

}
