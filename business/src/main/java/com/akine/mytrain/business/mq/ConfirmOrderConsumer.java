package com.akine.mytrain.business.mq;

import com.akine.mytrain.business.req.ConfirmOrderDoReq;
import com.akine.mytrain.business.service.ConfirmOrderService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(consumerGroup = "default", topic = "CONFIRM_ORDER")
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

    private static final Logger log = LoggerFactory.getLogger(ConfirmOrderConsumer.class);

    @Resource
    private ConfirmOrderService confirmOrderService;

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        ConfirmOrderDoReq req = JSON.parseObject(new String(body), ConfirmOrderDoReq.class);
        MDC.put("LOG_ID", req.getLogId());
        log.info("ROCKETMQ收到消息:{}", new String(body));
        try {
            confirmOrderService.doConfirm(req);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
