package andy.github.mqtt

/**
 * Mqtt常量值
 *
 * @author Andy
 * @since 2020-07-06 17:16
 */
object MqttConstants {
    // [QOS]
    const val AT_MOST_ONCE = 0 //最多一次
    const val AT_LEAST_ONCE = 1 //至少一次
    const val EXACTLY_ONCE = 2 //恰好一次

    // [TOPIC TYPE]
    const val UN_RETAINED = 0 //未保留
    const val RETAINED = 1 //保留

    // [CONNECT TYPE]
    const val TCP = "tcp://" //TCP
    const val SSL = "ssl://" //SSL

    // [CONNECTION STATUS]
    const val INIT = 1 //初始化
    const val CONNECTING = 1 shl 1 //连接中
    const val CONNECTED = 1 shl 2 //已连接
    const val DISCONNECTED = 1 shl 3 //断线
    const val LEAVE = 1 shl 4 //离线

    // [CLIENT STATUS]
    const val CONNECT = 0 //连接
    const val SUBSCRIBE = 1 //订阅
    const val UN_SUBSCRIBE = 2 //取消订阅
    const val PUBLISH = 3 //发布

    const val RECONNECT = "RECONNECT"
}