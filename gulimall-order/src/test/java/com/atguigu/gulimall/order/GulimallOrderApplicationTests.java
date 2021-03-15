package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Test
    public void contextLoads() {
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
    }
    @Test
    public void createQueue() {
        Queue queue = new Queue("hello-java-queue",true,false,true);
        amqpAdmin.declareQueue(queue);
    }

    @Test
    public void createBinding() {
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE, "hello-java-exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void sendMsg(){
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setBillContent("hello");
        rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity);

    }



}
