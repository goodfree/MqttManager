## MqttManager

### 使用

Gradle

```
implementation 'andy.github.mqtt:MqttManager:1.0.0'
```

Maven

```
<dependency>
	<groupId>andy.github.mqtt</groupId>
	<artifactId>MqttManager</artifactId>
	<version>1.0.0</version>
	<type>pom</type>
</dependency>
```

MQTT的连接/订阅/发布，支持以下功能

 *	建立连接
 *	断开连接
 *	订阅 (单个&多个)
 *	取消订阅 (单个&多个)
 *	发布 (单个&多个)
 *	创建主题

### 建立连接

```kotlin
mConnection = MqttConnection
                    .createConnection(applicationContext, IP, CLIENT_ID)
                    .addConnectionOptions(ConnectOptionsBuilder()
                    .setUserCredentials(false)
                    .setConnectionTimeout(30)
                    .setKeppAliveInterval(10))
                    
mConnection!!.connect(object : OnActionListener<MqttTopic> {
            override fun onSuccess(topic: MqttTopic?, message: String?) {
                runOnUiThread { mBinding!!.stateTv.text = "已连接,已订阅${mDeviceTopicList.size}种主题" }
            }

            override fun onFailure(topic: MqttTopic?, throwable: Throwable?) {
                runOnUiThread { mBinding!!.stateTv.text = "断开连接" }
            }
        })
```

### 断开连接

```kotlin
mConnection!!.disconnect()
```

### 订阅

- [x] 单个

```
mConnection!!.subscribeTopic(LightTopic(), object : OnActionListener<LightTopic> {
            override fun onSuccess(topic: LightTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

- [x] 多个

```
 mConnection!!.subscribeTopics(mDeviceTopicList, object : OnActionListener<DeviceTopic> {
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

### 取消订阅

- [x] 单个
```
mConnection!!.unSubscribeTopic(LightTopic(), object : OnActionListener<LightTopic> {
            override fun onSuccess(topic: LightTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

- [x] 多个
```
mConnection!!.unSubscribeTopics(mDeviceTopicList,object : OnActionListener<DeviceTopic>{
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

### 发布

- [x] 单个
```
mConnection!!.publishTopic(LightTopic(),object : OnActionListener<LightTopic>{
            override fun onSuccess(topic: LightTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

- [x] 多个
```
mConnection!!.publishTopics(mDeviceTopicList,object : OnActionListener<DeviceTopic>{
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })
```

### 创建主题

```
//DeviceTopic
open class DeviceTopic(val topic: String, var message: String) : MqttTopic("/DEVICE/$topic") {
    constructor(mqttTopic: String) : this(mqttTopic, "") {}
}

//LightTopic
class LightTopic : DeviceTopic, Subscription, Publishing {
    constructor() : super("Light") {}
    constructor(topic: String?, message: String?) : super(topic!!, message!!) {}

    override val publishingQoS: Int
        get() = MqttConstants.EXACTLY_ONCE
    override val isRetained: Int
        get() = MqttConstants.RETAINED
    override val publishingMessage: String
        get() = message
    override val subscriptionQoS: Int
        get() = MqttConstants.AT_LEAST_ONCE
}

```



#### 连接函数

| 外部调用                                                     |
| ------------------------------------------------------------ |
| fun connect(onActionListener: OnActionListener<MqttTopic>, retryTime: Int) |
| fun connect(onActionListener: OnActionListener<MqttTopic>)   |

#### 订阅函数

| 外部调用                                                     |
| ------------------------------------------------------------ |
| fun <T : MqttTopic> subscribeTopics(topics: MutableList<T>, onActionListener: OnActionListener<T>) |
| fun <T : MqttTopic> subscribeTopics(retryTime: Int, topics: MutableList<T>, onActionListener: OnActionListener<T>) |
| fun <T : MqttTopic> subscribeTopic(mqttTopic: T, onActionListener: OnActionListener<T>) |
| fun <T : MqttTopic> subscribeTopic(retryTime: Int, mqttTopic: T, onActionListener: OnActionListener<T>) |


#### 取消订阅函数

| 外部调用                                                     |
| ------------------------------------------------------------ |
| fun <T : MqttTopic> publishTopic(topic: T?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopic(retryTime: Int, topic: T?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopics(mqttTopics:MutableList<T>?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopics(retryTime: Int, mqttTopics: MutableList<T>?, onActionListener: OnActionListener<T>?) |

#### 发布函数

| 外部调用                                                     |
| ------------------------------------------------------------ |
| fun <T : MqttTopic> publishTopic(topic: T?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopic(retryTime: Int, topic: T?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopics(mqttTopics:MutableList<T>?, onActionListener: OnActionListener<T>?) |
| fun <T : MqttTopic> publishTopics(retryTime: Int, mqttTopics: MutableList<T>?, onActionListener: OnActionListener<T>?) |

#### 示例截图

|                                                              |                                                              |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| <img src="https://github.com/goodfree/MqttManager/blob/master/pictrue/20200917173858.jpg" style="zoom:25%;" /> | <img src="https://github.com/goodfree/MqttManager/blob/master/pictrue//20200917173913.jpg" style="zoom:25%;" /> |


#### License
This plugin is available under the Apache License, Version 2.0.

(c) All rights reserved JFrog
