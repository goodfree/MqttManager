package andy.github.mqtt.annotation

import androidx.annotation.IntDef
import andy.github.mqtt.MqttConstants

/**
 * Qos优先级
 *
 * @author Andy
 * @since 2020-07-06 17:15
 */
@IntDef(MqttConstants.AT_MOST_ONCE, MqttConstants.AT_LEAST_ONCE, MqttConstants.EXACTLY_ONCE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class Qos