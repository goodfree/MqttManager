package andy.github.mqtt.annotation

import androidx.annotation.IntDef
import andy.github.mqtt.MqttConstants

/**
 * 遗嘱消息
 *
 * @author Andy
 * @since 2020-07-06 17:19
 */
@IntDef(MqttConstants.UN_RETAINED, MqttConstants.RETAINED)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class Retained