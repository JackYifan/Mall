package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.MemberAddressVO;
import com.atguigu.gulimall.order.vo.OrderConfirmVO;
import com.atguigu.gulimall.order.vo.OrderItemVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    WareFeignService wareFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 返回订单的所有数据
     * @return
     */
    @Override
    public OrderConfirmVO confirmOrder() throws ExecutionException, InterruptedException {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.threadLocal.get();
        OrderConfirmVO orderConfirmVO = new OrderConfirmVO();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //调用远程接口查询用户所有地址并封装
            List<MemberAddressVO> memberAddressVOList = memberFeignService.getAddress(memberResponseVo.getId());
            orderConfirmVO.setAddresses(memberAddressVOList);
        }, threadPoolExecutor);

        CompletableFuture<Void> getCartItem = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程调用查询所有选中的购物项
            List<OrderItemVO> items = cartFeignService.getCurrentUserCartItem();
            orderConfirmVO.setItems(items);
        }, threadPoolExecutor).thenRunAsync(()->{
            //查询商品是否有货
            List<OrderItemVO> items = orderConfirmVO.getItems();
            //批量查询
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            //远程调用查询库存
            R r = wareFeignService.hasStock(skuIds);
            List<SkuHasStockVo> skuHasStockVoList = r.getData(new TypeReference<List<SkuHasStockVo>>() {
            });
            Map<Long, Boolean> wareData = skuHasStockVoList.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::isHasStock));
            orderConfirmVO.setStocks(wareData);
        },threadPoolExecutor);
        //查询用户积分
        memberResponseVo.setIntegration(memberResponseVo.getIntegration());
        CompletableFuture.allOf(getAddress,getCartItem).get();
        //TODO 防重令牌
        return orderConfirmVO;

    }

}
