package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        List<CategoryEntity> categories = categoryService.getLevel1Categories();
        model.addAttribute("categories",categories);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalog(){
        Map<String, List<Catalog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }


    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 读写锁测试
     * @return
     */
    @RequestMapping("/write")
    @ResponseBody
    public String write(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        //获取写锁
        RLock rLock = readWriteLock.writeLock();
        String s = "";
        try {
            //上锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(3000);
            redisTemplate.opsForValue().set("write-value",s);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }
        return s;
    }

    @RequestMapping("/read")
    @ResponseBody
    public String read(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        //获取写锁
        RLock rLock = readWriteLock.readLock();
        String s = "";
        try {
            //上锁
            rLock.lock();
            s = redisTemplate.opsForValue().get("write-value");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }
        return s;
    }

}
