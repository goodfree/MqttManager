package andy.github.mqtt.external

import andy.github.mqtt.annotation.Qos

/**
 * 订阅消息的QOS
 * @author Andy
 * @since 2020-07-06 17:42
 */
interface Subscription {
    @get:Qos
    val subscriptionQoS: Int
}