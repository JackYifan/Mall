package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.enume.OrderStatusEnum;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;



@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    ThreadLocal<OrderSubmitVo> confirmThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

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
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.threadLocal.get(); //从拦截器中提取当前登录用户信息
        OrderConfirmVo orderConfirmVO = new OrderConfirmVo();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //调用远程接口查询用户所有地址并封装
            List<MemberAddressVo> memberAddressVoList = memberFeignService.getAddress(memberResponseVo.getId());
            orderConfirmVO.setAddresses(memberAddressVoList);
        }, threadPoolExecutor);

        CompletableFuture<Void> getCartItem = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程调用查询所有选中的购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItem();
            orderConfirmVO.setItems(items);
        }, threadPoolExecutor).thenRunAsync(()->{
            //查询商品是否有货
            List<OrderItemVo> items = orderConfirmVO.getItems();
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
        //防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");//构造令牌
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberResponseVo.getId(),token,30, TimeUnit.MINUTES); //在redis中存放token
        orderConfirmVO.setOrderToken(token); //将token返回浏览器

        CompletableFuture.allOf(getAddress,getCartItem).get(); //等待异步任务完成
        return orderConfirmVO;

    }

    /**
     * 创建订单，验证令牌，验价格，锁库存
     * @param orderSubmitVO
     * @return
     */
    @Transactional
    @Override
    public SubmitResponseVo submitOrder(OrderSubmitVo orderSubmitVO) {
        confirmThreadLocal.set(orderSubmitVO);
        SubmitResponseVo responseVo = new SubmitResponseVo();
        responseVo.setCode(0);
        //验令牌
        String token = orderSubmitVO.getOrderToken();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.threadLocal.get();
        String key = OrderConstant.USER_ORDER_TOKEN_PREFIX+ memberResponseVo.getId();
        // 如果返回0表示该脚本执行失败
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        //验证令牌是否符合并删除令牌的原子操作
        Long executeResult = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(key, token), token);
        if(executeResult!=null&&executeResult==0){
            //执行失败
            responseVo.setCode(1);
            return responseVo;
        }
        //创建订单
        OrderCreateTo order = createOrder();
        //验价
        BigDecimal payAmount = order.getOrderEntity().getPayAmount();
        BigDecimal payPrice = orderSubmitVO.getPayPrice();
        if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
            //保存订单
            saveOrder(order);
            //TODO 锁定库存
            //设置订单号
            WareStockLockVo wareStockLockVo = new WareStockLockVo();
            wareStockLockVo.setOrderSn(order.getOrderEntity().getOrderSn());
            //设置所有商品信息
            List<OrderItemEntity> orderItems = order.getOrderItems();
            List<OrderItemVo> orderItemVoList = orderItems.stream().map(item -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setSkuId(item.getSkuId());
                orderItemVo.setCount(item.getSkuQuantity());
                return orderItemVo;
            }).collect(Collectors.toList());
            wareStockLockVo.setOrderItemVos(orderItemVoList);
            R r = wareFeignService.lockStock(wareStockLockVo);
            if(r.getCode()==0){
                //锁定库存成功
                responseVo.setOrderEntity(order.getOrderEntity());
                //出异常会回滚
                //TODO　将订单创建成功的消息发到MQ中
                rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrderEntity());
                return responseVo;
            }else{
                //锁定失败
                responseVo.setCode(3);
                return responseVo;
            }
        }else{
            responseVo.setCode(2);
            return responseVo;
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    /**
     * 关闭订单
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //查询当前订单的状态
        OrderEntity latestOrder = this.getById(orderEntity.getId());
        if(latestOrder.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            //订单只有在新建状态才能关闭，否则可能重复关闭
            OrderEntity entity = new OrderEntity();
            entity.setId(orderEntity.getId());
            entity.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(entity);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(latestOrder, orderTo);
            //主动发给order-event-exchange解锁库存，若没有发送可能因为网络卡顿，2分钟后的被动解锁因为订单状态并不是Cancel而不能解锁库存
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other", orderTo);
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        String total = orderEntity.getTotalAmount().setScale(2, BigDecimal.ROUND_UP).toString();
        payVo.setTotal_amount(total);
        payVo.setOut_trade_no(orderSn);
        OrderItemEntity orderItem = orderItemService.getOne(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        payVo.setSubject(orderItem.getSkuName());
        payVo.setBody(orderItem.getSkuAttrsVals());
        return payVo;
    }

    /**
     * 查询当前登录用户的所有订单
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.threadLocal.get();//获取用户信息
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberResponseVo.getId()).orderByDesc("id")
        ); //获取分页的订单信息
        List<OrderEntity> orderEntityList = page.getRecords().stream().map(orderEntity -> {
            List<OrderItemEntity> itemEntityList = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderEntity.getOrderSn()));
            orderEntity.setItemEntities(itemEntityList);
            return orderEntity;
        }).collect(Collectors.toList());//为每个订单封装商品信息
        page.setRecords(orderEntityList);
        return new PageUtils(page);
    }

    /**
     * 根据支付宝返回的信息保存支付信息
     * @param vo
     * @return
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存支付信息
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no()); //交易id
        infoEntity.setOrderSn(vo.getOut_trade_no()); //订单id
        infoEntity.setPaymentStatus(vo.getTrade_status()); //支付状态
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);
        //修改订单状态 交易成功或者交易结束
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 保存订单中的所有数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        //保存订单信息
        OrderEntity orderEntity = order.getOrderEntity();
        orderEntity.setModifyTime(new Date());
        this.baseMapper.insert(orderEntity);
        //保存订单所有商品信息
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo order = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId(); //根据时间生成订单号
        OrderEntity orderEntity = buildOrder(orderSn); //创建订单
        order.setOrderEntity(orderEntity);
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn); //获取订单商品信息
        order.setOrderItems(orderItemEntities);
        // 验价
        if(!CollectionUtils.isEmpty(orderItemEntities)){
            compute(orderEntity,orderItemEntities);
        }

        return order;
    }

    /**
     * 计算所有商品总价等信息，利用每一项商品的信息求和
     * @param orderEntity 订单信息
     * @param itemEntities 订单中所有商品信息
     */
    private void compute(OrderEntity orderEntity,List<OrderItemEntity> itemEntities){
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");

        for(OrderItemEntity entity:itemEntities){
            total = total.add(entity.getRealAmount());
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));//应付金额加上运费，默认为0待扩展
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);
    }


    /**
     * 创建订单
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.threadLocal.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        OrderSubmitVo orderSubmitVo = confirmThreadLocal.get();
        orderEntity.setMemberId(memberResponseVo.getId());
        R getFareResult = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareVo = getFareResult.getData(new TypeReference<FareVo>() {
        });
        // 设置运费信息
        orderEntity.setFreightAmount(fareVo.getFare());
        // 设置收货人信息
        orderEntity.setReceiverCity(fareVo.getMemberAddressVO().getCity());
        orderEntity.setReceiverDetailAddress(fareVo.getMemberAddressVO().getDetailAddress());
        orderEntity.setReceiverName(fareVo.getMemberAddressVO().getName());
        orderEntity.setReceiverPhone(fareVo.getMemberAddressVO().getPhone());
        orderEntity.setReceiverPostCode(fareVo.getMemberAddressVO().getPostCode());
        orderEntity.setReceiverProvince(fareVo.getMemberAddressVO().getProvince());
        orderEntity.setReceiverRegion(fareVo.getMemberAddressVO().getRegion());

        // 设置订单的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        return orderEntity;
    }

    /**
     * 获取当前所有订单项
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItem = cartFeignService.getCurrentUserCartItem();
        if(!CollectionUtils.isEmpty(currentUserCartItem)){
            List<OrderItemEntity> itemEntities = currentUserCartItem.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 将cartItem封装为OrderItemEntity
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //spu信息
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setCategoryId(data.getCatalogId());
        //商品sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttrs = StringUtils.collectionToDelimitedString(cartItem.getSkuAttrs(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrs);
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        //TODO 优惠信息待扩展
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        // 当前订单项的实际金额
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        // 总额减去各种优惠后的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount()).subtract(orderItemEntity.getPromotionAmount());
        orderItemEntity.setRealAmount(subtract); //每件商品的总价
        return orderItemEntity;
    }

}
