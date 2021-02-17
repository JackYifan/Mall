package com.atguigu.common.to.es;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ESSkuModel {

    private Long skuId;

    private Long spuId;

    private String skuTitle;

    private BigDecimal skuPrice;

    private String skuImg;

    private Long saleCount;

    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private String  brandName;

    private String brandImg;

    private Long catalogId;

    private String catalogName;

    private List<Attr> attrs;


    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attr {

        private Long attrId;

        private String attrName;

        private String attrValue;

    }

}
