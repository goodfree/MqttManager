package andy.github.mqtt.external

/**
 * Mqtt主题
 *
 * @author Andy
 * @since 2020-07-06 17:37
 */
abstract class MqttTopic(val mqttTopic: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val mqttTopic1 = other as MqttTopic
        return mqttTopic == mqttTopic1.mqttTopic
    }

    override fun hashCode(): Int {
        return mqttTopic.hashCode()
    }

    override fun toString(): String {
        return "MqttTopic{" +
                "mqttTopic='" + mqttTopic + '\'' +
                '}'
    }
}