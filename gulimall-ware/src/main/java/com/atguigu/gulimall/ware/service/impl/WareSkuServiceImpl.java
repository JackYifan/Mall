package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.exception.NoStockException;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.WareLockResultVo;
import com.atguigu.gulimall.ware.vo.WareStockLockVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(StringUtils.isNotEmpty(skuId)){
            queryWrapper.eq("sku_id",skuId);
        }
        String wareId = (String) params.get("wareId");
        if(StringUtils.isNotEmpty(wareId)){
            queryWrapper.eq("ware_id",wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 将获取的商品加入到库存中
     * @param skuId 商品id
     * @param wareId 仓库id
     * @param skuNum 要增加的库存量
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //先查询是否已经存在，若不存在新增，若存在则更新
        Integer count = wareSkuDao.selectCount(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(count==0){
            //新增
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //远程查询sku的名字 失败抛出异常不回滚 TODO
            try{
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode()==0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            wareSkuDao.insert(wareSkuEntity);
        }else{
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> hasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            //查询是否还有库存
            Long count = baseMapper.hasStock(skuId);
            skuHasStockVo.setHasStock(count==null?false:count>0);
            skuHasStockVo.setSkuId(skuId);
            return skuHasStockVo;
        }).collect(Collectors.toList());
        return collect;

    }

    /**
     * 锁定库存
     * @param wareStockLockVo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean lockStock(WareStockLockVo wareStockLockVo) {
        //TODO
        List<OrderItemVo> orderItemVos = wareStockLockVo.getOrderItemVos();
        List<WareHasStock> wareHasStockList = orderItemVos.stream().map(item -> {
            WareHasStock wareHasStock = new WareHasStock();
            wareHasStock.setSkuId(item.getSkuId());
            wareHasStock.setCount(item.getCount());
            //查询那个仓库下有库存
            List<Long> wareHasStockIdList = baseMapper.selectWareHasStock(item.getSkuId(), item.getCount());
            wareHasStock.setWareId(wareHasStockIdList);
            return wareHasStock;
        }).collect(Collectors.toList());
        for (WareHasStock wareHasStock : wareHasStockList) {
            Long skuId = wareHasStock.getSkuId();
            List<Long> wareIdList = wareHasStock.getWareId();
            Boolean hasLocked = false;
            if(CollectionUtils.isEmpty(wareIdList)){
                throw new NoStockException(skuId); //抛出运行时异常使事务回滚
            }
            for (Long wareId : wareIdList) {
                Long result = wareSkuDao.lockStock(skuId,wareId, wareHasStock.getCount()); //更新操作成功则返回更新的数量
                if(result!=0){
                    //库存保存成功
                    hasLocked = true;
                    break;
                }
            }
            if(!hasLocked){
                throw new NoStockException(skuId);
            }
        }
        return true;


    }

    @Data
    class WareHasStock {
        /**
         * 商品sku
         */
        private Long skuId;

        /**
         * 需要锁定的商品数量
         */
        private Integer count;

        /**
         * 拥有该商品的仓库id
         */
        private List<Long> wareId;

    }
}
