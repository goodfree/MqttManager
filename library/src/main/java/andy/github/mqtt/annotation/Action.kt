package andy.github.mqtt.annotation

import androidx.annotation.IntDef
import andy.github.mqtt.MqttConstants

/**
 * 记得写注释哦
 *
 * @author Andy
 * @since 2020-07-06 17:52
 */
@IntDef(MqttConstants.CONNECT, MqttConstants.SUBSCRIBE, MqttConstants.UN_SUBSCRIBE, MqttConstants.PUBLISH)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class Action