package andy.github.mqtt

import andy.github.mqtt.internal.MqttConnection
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT管理器
 * @author Andy
 * @since 2020-07-06 17:48
 */
class MqttManager {
    private val mMqttConnectionMap: MutableMap<String, MqttConnection> = ConcurrentHashMap()
    private val mTimeStamps: MutableMap<String, Long> = ConcurrentHashMap()
    fun release() {
        sInstance!!.clearMqttConnections()
        sInstance = null
    }

    /**
     * onResume更新时间戳
     * @param clientId
     */
    fun onResume(clientId: String) {
        synchronized(MqttManager::class.java) {
            mTimeStamps.remove(clientId)
            mTimeStamps.put(clientId, System.currentTimeMillis())
        }
    }

    /**
     * onPause 断开连接
     * @param clientId
     */
    fun onPause(clientId: String) {
        synchronized(MqttManager::class.java) {
            val connection = getMqttConnection(clientId)
            if (connection != null) {
                connection.disconnect()
                if (connection.checkStatus(MqttConstants.LEAVE)) {
                    removeMqttConnections(clientId)
                }
            }
        }
    }

    fun getMqttConnection(clientId: String): MqttConnection? {
        synchronized(MqttManager::class.java) {
            var currentConnection: MqttConnection? = null
            if (mTimeStamps.containsKey(clientId)) {
                return null
            }
            val currentTime = mTimeStamps[clientId]!!
            for (key in mMqttConnectionMap.keys) {
                if (key.contains(clientId)) {
                    val connection = mMqttConnectionMap[key]
                    val time = convertTime(key)
                    // Get current time stamp connection
                    if (time == currentTime) {
                        currentConnection = connection
                    } else {
                        // release the connection which out of date
                        if (connection!!.checkStatus(MqttConstants.LEAVE)) {
                            mMqttConnectionMap.remove(key)
                        }
                    }
                }
            }
            return currentConnection
        }
    }

    fun addMqttConnecttion(mqttConnection: MqttConnection): MqttConnection {
        synchronized(MqttManager::class.java) {
            mMqttConnectionMap[mqttConnection.clientId] = mqttConnection
            return mqttConnection
        }
    }

    fun removeMqttConnections(clientId: String) {
        mMqttConnectionMap.remove(clientId)
    }

    private fun clearMqttConnections() {
        for (key in mMqttConnectionMap.keys) {
            mMqttConnectionMap[key]!!.disconnect()
        }
        mMqttConnectionMap.clear()
    }

    /**
     * The client key is compose by client id + time stamp, as below:
     * "clientId_timeStamp", e.g. device_12345678.
     *
     *
     * For getting the time stamp, the easy way is split the string by
     * "_", to get the time stamp of client id
     * @param key       client key
     * @return          timestamp
     */
    private fun convertTime(key: String): Long {
        try {
            if (key.contains("_")) {
                val strings = key.split("_".toRegex()).toTypedArray()
                return strings[strings.size - 1].toLong()
            }
            return key.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Gets current time stamp of client id
     * @param clientId target client id
     * @return time stamp
     */
    fun getTimeStamp(clientId: String): String {
        return "_" + mTimeStamps[clientId]
    }

    companion object {
        const val MQTT_TAG = "MQTT"
        const val DEBUG = true

        @Volatile
        private var sInstance: MqttManager? = null
        @JvmStatic
        val instance: MqttManager?
            get() {
                if (sInstance == null) {
                    synchronized(MqttManager::class.java) {
                        if (sInstance == null) {
                            sInstance = MqttManager()
                        }
                    }
                }
                return sInstance
            }
    }
}