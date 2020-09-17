package andy.github.mqtt.external

import andy.github.mqtt.annotation.Retained

/**
 * 发布消息
 *
 * @author Andy
 * @since 2020-07-06 17:42
 */
interface Publishing {
    val publishingQoS: Int

    @get:Retained
    val isRetained: Int
    val publishingMessage: String?
}