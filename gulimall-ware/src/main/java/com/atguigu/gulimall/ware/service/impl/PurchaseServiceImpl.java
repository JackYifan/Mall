package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceiveList(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status",0).or().eq("status",1);
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);

    }

    /**
     * 合并采购需求
     * 合并到一个采购单中
     * @param mergeVo
     */
    @Transactional
    @Override
    public void merge(MergeVo mergeVo) {

        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId==null){
            //如果没有指定采购订单需要创建
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseService.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        // TODO 如果有采购单号需要确认状态
        Long finalPurchaseId = purchaseId;//采购单号
        //批量更新
        List<Long> items = mergeVo.getItems();
        List<PurchaseDetailEntity> purchaseDetailEntities = items.stream().map(item -> {
            //所有需求id
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            detailEntity.setId(item);
            detailEntity.setPurchaseId(finalPurchaseId);
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return detailEntity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetailEntities);
        //更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }

    @Override
    public void receive(List<Long> purchaseIds) {

        //修改采购单
        List<PurchaseEntity> purchaseEntities = purchaseIds.stream().map((purchaseId) -> {
            //查出所有采购单
            PurchaseEntity purchaseEntity = this.getById(purchaseId);
            return purchaseEntity;
        }).filter(entity -> {
            //判断状态为创建或者分配的状态才能接收
            if (entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            //修改所有符合条件的entity的status
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());
        purchaseService.updateBatchById(purchaseEntities);

        //修改需求的状态

        purchaseEntities.forEach(item->{
            //获取该采购单中的所有需求项
            List<PurchaseDetailEntity> purchaseDetailEntities =  purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = purchaseDetailEntities.stream().map(entity -> {
                PurchaseDetailEntity newEntity = new PurchaseDetailEntity();
                newEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                newEntity.setId(entity.getId());
                return newEntity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo vo) {
        //改变采购单的状态,需要判断所有需求后判断
        Boolean flag = true;
        //改变所有采购项
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        List<PurchaseItemDoneVo> items = vo.getItems();
        for (PurchaseItemDoneVo item : items) {
            //封装成PurchaseDetailEntity
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();

            if(item.getStatus()==WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                //未完成采购
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //采购成功的入库，更新库存
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());//获取采购信息
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());

            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        //批量更新提高效率
        purchaseDetailService.updateBatchById(updates);

        //根据flag修改采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(vo.getId());
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);




    }

}
