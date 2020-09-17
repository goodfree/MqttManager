package andy.github.mqtt.internal

import andy.github.mqtt.external.MqttTopic

/**
 * MQTT 连接&订阅回调监听
 *
 * @author Andy
 * @since 2020-07-06 17:49
 */
interface OnActionListener<in T : MqttTopic> {
    fun onSuccess(topic: T?, message: String?)
    fun onFailure(topic: T?, throwable: Throwable?)
}