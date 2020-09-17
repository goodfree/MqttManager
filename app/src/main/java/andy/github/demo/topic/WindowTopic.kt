package andy.github.demo.topic

import andy.github.mqtt.MqttConstants
import andy.github.mqtt.external.Publishing
import andy.github.mqtt.external.Subscription

/**
 * 记得写注释哦
 *
 * @author Andy
 * @since 2020-07-07 14:58
 */
class WindowTopic : DeviceTopic, Subscription, Publishing {
    constructor() : super("Window") {}
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