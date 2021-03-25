package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Yifan Wu
 * Date on 2021/3/23  15:10
 */
@Configuration
public class OrderMQConfig {

    /**
     * 延时队列，过期后发送到order-event-exchange，使用路由键order.release.order
     * @return
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        arguments.put("x-message-ttl", 60000);
        return new Queue("order.delay.queue", true, false, false, arguments);
    }

    /**
     * 取消订单队列
     * @return
     */
    @Bean
    public Queue orderReleaseOrderQueue() {
        return new Queue("order.release.order.queue", true, false, false);
    }

    /**
     * 交换机
     * @return
     */
    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("order-event-exchange", true, false);
    }

    /**
     * 创建订单后路由到延时队列
     * @return
     */
    @Bean
    public Binding orderCreateOrderBinding() {
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.create.order", new HashMap<>());
    }

    /**
     * 取消订单的绑定关系
     * @return
     */
    @Bean
    public Binding orderReleaseOrderBinding() {
        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.release.order", new HashMap<>());
    }

    /**
     * 订单取消主动发消息解锁库存
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding() {
        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.release.other.#", new HashMap<>());
    }

}
