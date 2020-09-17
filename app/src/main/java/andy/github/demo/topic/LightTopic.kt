package andy.github.demo.topic

import andy.github.mqtt.MqttConstants
import andy.github.mqtt.external.Publishing
import andy.github.mqtt.external.Subscription

/**
 * 灯主题
 *
 * @author Andy
 * @since 2020-07-07 14:57
 */
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