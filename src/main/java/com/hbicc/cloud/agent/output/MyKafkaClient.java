package com.hbicc.cloud.agent.output;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hbicc.cloud.agent.utils.WriterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MyKafkaClient {
    @Value("${spring.mqtt.topic-out}")
    private String topicOut;

    @Autowired
    private static KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public void setKafkaTemplate(KafkaTemplate<String, Object> kafkaTemplate) {
        MyKafkaClient.kafkaTemplate = kafkaTemplate;
    }

    private static String topic;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() throws InterruptedException {
        topic = this.topicOut;
    }

    @PreDestroy
    public void destory() throws InterruptedException {

    }

    public static KafkaTemplate<String, Object> kafka() {
        return MyKafkaClient.kafkaTemplate;
    }

    public static void pub(Object msg) {
        kafkaTemplate.send(topic, msg).addCallback(success -> {
            // 消息发送到的topic
            String topic = success.getRecordMetadata().topic();
            // 消息发送到的分区
            int partition = success.getRecordMetadata().partition();
            // 消息在分区内的offset
            long offset = success.getRecordMetadata().offset();
            log.info("转发到kafka成功:" + topic + "-" + partition + "-" + offset);
            WriterUtil.WriterIO("转发到kafka成功:" + topic + "-" + partition + "-" + offset);
        }, failure -> {
            log.info("转发到kafka失败:" + failure.getMessage());
            WriterUtil.WriterIO("转发到kafka失败:" + failure.getMessage());

        });
    }
}
