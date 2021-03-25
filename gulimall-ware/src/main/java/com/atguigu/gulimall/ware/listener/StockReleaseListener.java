package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Author Yifan Wu
 * Date on 2021/3/22  23:23
 */
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockTo stockLockTo, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.unlockStock(stockLockTo);
            //接收并不需要重新入队
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //拒绝并重新入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseMessage(OrderTo orderTo, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.unlockStock(orderTo);
            //接收并不需要重新入队
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //拒绝并重新入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
