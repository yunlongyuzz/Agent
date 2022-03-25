package com.hbicc.cloud.agent.output;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hbicc.cloud.agent.utils.WriterUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class MyMqttClient {

    @Value("${spring.mqtt.client-id}")
    private String clientid;

    @Value("${spring.mqtt.host}")
    private String host;

    @Value("${spring.mqtt.topic-out}")
    private String topicOut;

    @Value("${spring.mqtt.port}")
    private Integer port;

    public MqttClient mqttClient = null;
    private MemoryPersistence memoryPersistence = null;
    private MqttConnectOptions mqttConnectOptions = null;

    public static MyMqttClient myMqttClient = null;



    /**
     * 初始化
     */
    @PostConstruct
    public void init() throws InterruptedException {
        myMqttClient = this;

        log.info("开启MQTT监听服务");
        WriterUtil.WriterIO("开启MQTT监听服务");
        // 初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        if (null == mqttConnectOptions) {
            log.info("mqttConnectOptions对象为空");
            WriterUtil.WriterIO("mqttConnectOptions对象为空");
            return;
        }
        // true可以安全地使用内存持久性作为客户端断开连接时清除的所有状态
        mqttConnectOptions.setCleanSession(true);
        // 设置连接超时
        mqttConnectOptions.setConnectionTimeout(30);
        // 设置持久化方式
        memoryPersistence = new MemoryPersistence();
        // 设置自动重连
        // mqttConnectOptions.setAutomaticReconnect(true);

        if (null == memoryPersistence) {
            log.info("memoryPersistence对象为空!");
            WriterUtil.WriterIO("memoryPersistence对象为空!");
            return;
        }

        if (null == mqttClient) {
            try {
                mqttClient = new MqttClient(String.format("tcp://%s:%d", host, port), clientid, memoryPersistence);
            } catch (MqttException e) {
                e.printStackTrace();
                return;
            }
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.error("MQTT 连接成功！！！");
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    log.error("MQTT 断开连接");
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    log.info("messageArrived: {}, {}", s, new String(mqttMessage.getPayload()));
                    WriterUtil.WriterIO("messageArrived: {}, {}", s, new String(mqttMessage.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            // subTopic(topicIn);
        }

        if (null == mqttClient) {
            log.info("mqttClient对象为空");
            WriterUtil.WriterIO("mqttClient对象为空");
            return;
        }
    }

    /**
     * 连接
     */
    public static void doConnect() {
        if (null == myMqttClient.mqttConnectOptions) {
            log.info("mqttConnectOptions对象为空");
            WriterUtil.WriterIO("mqttConnectOptions对象为空");
            return;
        }
        if (null == myMqttClient.memoryPersistence) {
            log.info("memoryPersistence对象为空");
            WriterUtil.WriterIO("memoryPersistence对象为空");
            return;
        }
        if (null == myMqttClient.mqttClient) {
            log.info("mqttClient对象为空");
            WriterUtil.WriterIO("mqttClient对象为空");
            return;
        }
        if (myMqttClient.mqttClient.isConnected()) {
            log.info("MQTT已经连接,无需再连!");
            WriterUtil.WriterIO("MQTT已经连接,无需再连!");
            return;
        }
        try {
            myMqttClient.mqttClient.connect(myMqttClient.mqttConnectOptions);
        } catch (MqttException e) {
            // e.printStackTrace();
            log.info("MQTT连接失败，尝试重连!");
            WriterUtil.WriterIO("MQTT连接失败，尝试重连!");
            return;
        }
    }

    /**
     * 关闭连接
     */
    public static void closeConnect() {
        // 关闭存储方式
        if (null != myMqttClient.memoryPersistence) {
            try {
                myMqttClient.memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                e.printStackTrace();
            }
        } else {
            log.info("memoryPersistence is null");
            WriterUtil.WriterIO("memoryPersistence is null");
        }

        // 关闭连接
        if (null != myMqttClient.mqttClient) {
            if (myMqttClient.mqttClient.isConnected()) {
                try {
                    myMqttClient.mqttClient.disconnect();
                    myMqttClient.mqttClient.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                log.info("mqttClient is not connect");
                WriterUtil.WriterIO("mqttClient is not connect");
            }
        } else {
            log.info("mqttClient is null");
            WriterUtil.WriterIO("mqttClient is null");
        }
    }

    public static void pub(String message){
        publishMessage(myMqttClient.topicOut, message, 0);
    }

    /**
     * 发布消息
     *
     * @param pubTopic
     * @param message
     * @param qos
     */
    public static void publishMessage(String pubTopic, String message, int qos) {
        if (null != myMqttClient.mqttClient && !myMqttClient.mqttClient.isConnected()) {
            doConnect();
        }
        if (null != myMqttClient.mqttClient && myMqttClient.mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = myMqttClient.mqttClient.getTopic(pubTopic);

            if (null != topic) {
                try {
                    // MqttDeliveryToken publish = topic.publish(mqttMessage);
                    topic.publish(mqttMessage);
                    // if (!publish.isComplete()) {
                    /* log.info("发布MQTT信息成功!"); */
                    // }
                } catch (MqttException e) {
                    /* log.info("发布MQTT信息错误!"); */
                    e.printStackTrace();
                }
            } else {
                /* log.info("发布MQTT信息错误, topic为空!"); */
            }
        } else {
            /* log.info("发布MQTT信息错误!"); */
        }
    }

    /**
     * 订阅主题
     *
     * @param topic
     */
    public static void subTopic(String topic) {
        if (null != myMqttClient.mqttClient && !myMqttClient.mqttClient.isConnected()) {
            doConnect();
        }
        if (null != myMqttClient.mqttClient && myMqttClient.mqttClient.isConnected()) {
            try {
                myMqttClient.mqttClient.subscribe(topic, 1);
            } catch (MqttException e) {
                log.info("subTopic: subTopic error");
                WriterUtil.WriterIO("subTopic: subTopic error");
                e.printStackTrace();
            }
        } else {
            log.info("subTopic: mqttClient is error");
            WriterUtil.WriterIO("subTopic: mqttClient is error");
        }
    }

    /**
     * 清空主题
     *
     * @param topic
     */
    public static void cleanTopic(String topic) {
        if (null != myMqttClient.mqttClient && !myMqttClient.mqttClient.isConnected()) {
            doConnect();
        }
        if (null != myMqttClient.mqttClient && !myMqttClient.mqttClient.isConnected()) {
            try {
                myMqttClient.mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                log.info("cleanTopic: cleanTopic error");
                WriterUtil.WriterIO("cleanTopic: cleanTopic error");
                e.printStackTrace();
            }
        } else {
            log.info("cleanTopic mqttClient is error");
            WriterUtil.WriterIO("cleanTopic mqttClient is error");
        }
    }

    @PreDestroy
    public void destory() throws InterruptedException {
        mqttConnectOptions = null;
        memoryPersistence = null;
        mqttClient = null;
        log.info("关闭MQTT监听服务");
        WriterUtil.WriterIO("关闭MQTT监听服务");
    }
}
