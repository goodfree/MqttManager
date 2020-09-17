package andy.github.demo.topic

import andy.github.mqtt.external.MqttTopic

/**
 * 记得写注释哦
 *
 * @author Andy
 * @since 2020-07-07 14:43
 */
open class DeviceTopic(val topic: String, var message: String) : MqttTopic("/DEVICE/$topic") {

    constructor(mqttTopic: String) : this(mqttTopic, "") {}
}