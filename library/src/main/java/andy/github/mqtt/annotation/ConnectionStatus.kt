package andy.github.mqtt.annotation

import androidx.annotation.IntDef
import andy.github.mqtt.MqttConstants

/**
 * 连接状态
 *
 * @author Andy
 * @since 2020-07-06 17:18
 */
@IntDef(flag = true, value = [MqttConstants.INIT, MqttConstants.CONNECTING, MqttConstants.CONNECTED, MqttConstants.DISCONNECTED, MqttConstants.LEAVE])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class ConnectionStatus