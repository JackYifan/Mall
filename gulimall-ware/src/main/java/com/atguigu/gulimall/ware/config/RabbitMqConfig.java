package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Yifan Wu
 * Date on 2021/3/22  20:26
 */
@Configuration
public class RabbitMqConfig {

    /**
     * Json序列化器
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }





    /**
     * 主题交换机
     * @return
     */
    @Bean
    public Exchange stockEventExchange(){
        return new TopicExchange("stock-event-exchange",true,false);
    }

    /**
     * 释放锁定的内存的队列
     * @return
     */
    @Bean
    public Queue stockReleaseStockQueue(){
        //Exclusive (used by only one connection and the queue will be deleted when that connection closes)
        return new Queue("stock.release.stock.queue",true,false,false);
    }

    /**
     * 延迟队列,消息过期后发送到交换机
     * @return
     */
    @Bean
    public Queue stockDelayQueue(){
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange","stock-event-exchange"); //过期后发送的路由器名字
        args.put("x-dead-letter-routing-key","stock.release"); //发送的路由键
        args.put("x-message-ttl",120000); //time-to-live
        return new Queue("stock.delay.queue",true,false,false,args);
    }


    /**
     * 绑定队列和交换机
     * @return
     */
    @Bean
    public Binding stockReleaseBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

    /**
     * 绑定队列和交换机
     * @return
     */
    @Bean
    public Binding stockLockedBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }




}
