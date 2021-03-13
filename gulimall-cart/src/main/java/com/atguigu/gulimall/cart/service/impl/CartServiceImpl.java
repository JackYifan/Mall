package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeign;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.to.UserInfoTO;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class CartServiceImpl implements CartService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String CART_PREFIX = "gulimall:cart:";

    @Autowired
    private ProductFeign productFeign;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 将num件skuId的商品加入购物车
     * 购物车中的信息用redis存储
     * 格式是 gulimall:cart:userId
     * 临时用户是 gulimall:cart:userkey
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //获取购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString()); //注意存储时的key,value都是String
        if(res==null){
            //购物车中新增新商品
            CartItem cartItem = new CartItem();
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //查询skuId的商品信息
                R r = productFeign.getSkuInfo(skuId);
                SkuInfoVO skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVO>() {
                });
                cartItem.setChecked(true);
                cartItem.setCount(num);
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setPrice(skuInfo.getPrice());
                cartItem.setSkuId(skuId);
                cartItem.setTitle(skuInfo.getSkuTitle());
            }, threadPoolExecutor);

            CompletableFuture<Void> getAttrTask = CompletableFuture.runAsync(() -> {
                //封装sku属性信息
                List<String> attrList = productFeign.getAttrList(skuId);
                cartItem.setSkuAttrs(attrList);
            }, threadPoolExecutor);

            CompletableFuture.allOf(getSkuInfoTask, getAttrTask).get();
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);
            return cartItem;
        }else{
            //购物车已有该商品，新增数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String item = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(item, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        UserInfoTO userInfoTO = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        if(userInfoTO.getUserId()==null){
            //临时用户
            String cartKey = CART_PREFIX+userInfoTO.getUserKey();
            List<CartItem> items = getItems(cartKey);
            cart.setItems(items);
        }else{
            //非临时用户需要合并临时用户的购物车
            String cartKey = CART_PREFIX+userInfoTO.getUserId();
            String tempKey = CART_PREFIX+userInfoTO.getUserKey();
            //获取临时购物车中的商品
            List<CartItem> tempItems = getItems(tempKey);
            if(!CollectionUtils.isEmpty(tempItems)){
                for(CartItem tempItem : tempItems){
                    //注意当用户购物车中商品skuId和临时购物车中相同时只增加数量，该逻辑在addToCart函数中实现
                    addToCart(tempItem.getSkuId(),tempItem.getCount());
                }
                //清空临时购物车
                clearCart(tempKey);
            }
            //获取用户购物车中的商品
            cart.setItems(getItems(cartKey));
        }
        return cart;

    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setChecked(checked == 1?true:false);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),s); //key相同会覆盖value
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    private void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    /**
     * 获取指定cartKey中的所有商品并封装成List
     * @param cartKey
     * @return
     */
    public List<CartItem> getItems(String cartKey){
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = cartOps.values();
        List<CartItem> itemList = new ArrayList<>();
        if(values!=null&&values.size()>0){
            for(Object value:values){
                String item =(String) value;
                CartItem cartItem = JSON.parseObject(item, CartItem.class);
                itemList.add(cartItem);
            }
        }
        return itemList;
    }

    private BoundHashOperations<String, Object, Object> getCartOps() {
        //计算cartKey
        UserInfoTO userInfoTO = CartInterceptor.threadLocal.get();
        String cartKey = cartKey=(userInfoTO.getUserId()!=null)?CART_PREFIX+userInfoTO.getUserId():CART_PREFIX+userInfoTO.getUserKey();
        //绑定需要操作的hash
        return redisTemplate.boundHashOps(cartKey);
    }
}
