package com.atguigu.gulimall.ware.exception;

/**
 * @Author Yifan Wu
 * Date on 2021/3/20  11:28
 */
public class NoStockException extends RuntimeException{
    private Long skuId;
    public NoStockException(Long skuId){
        super("商品id:"+skuId+"没有足够的库存了"); //使用父类构造器设置异常信息
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
