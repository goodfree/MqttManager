package andy.github.mqtt.internal

import android.content.Context
import android.util.Log
import andy.github.mqtt.MqttConstants
import andy.github.mqtt.MqttManager
import andy.github.mqtt.MqttManager.Companion.instance
import andy.github.mqtt.annotation.ConnectionStatus
import andy.github.mqtt.external.MqttTopic
import andy.github.mqtt.external.Publishing
import andy.github.mqtt.external.Subscription
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

/**
 * Mqtt连接
 * <pre>
 * 功能:
 * 1. 建立连接
 * 2. 断开连接
 * 3. 订阅 (单个&多个)
 * 4. 取消订阅 (单个&多个)
 * 5. 发布 (单个&多个)
</pre> *
 *
 * @author Andy
 * @since 2020-07-06 17:55
 */
class MqttConnection private constructor(val client: MqttAndroidClient, serverUri: String, clientId: String) : MqttCallbackExtended {
    val serverUri: String
    val clientId: String
    private var mMqttActionListeners: MutableList<MqttActionListener> = CopyOnWriteArrayList()
    private var mLeaving: Boolean = false

    private var mMqttConnectOptions: MqttConnectOptions? = null
    private var mOnActionListener: OnActionListener<MqttTopic>? = null
    private var mSubscriptionList: MutableList<MqttTopic> = ArrayList()
    private var mPublishingList: MutableList<MqttTopic> = ArrayList()
    private fun weakReferenceListener(onActionListener: OnActionListener<MqttTopic>?): OnActionListener<MqttTopic>? {
        return if (onActionListener == null) {
            null
        } else WeakReference(onActionListener).get()
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String) {
        debugMessage("$clientId [CONNECT COMPLETE] , isReconnect = $reconnect")
        status = MqttConstants.CONNECTED
        if (mLeaving) {
            disconnect()
        } else {
            if (reconnect || mSubscriptionList.isEmpty()) {
                mOnActionListener!!.onSuccess(null, MqttConstants.RECONNECT)
            }
        }
    }

    override fun connectionLost(cause: Throwable) {
        if (!checkStatus(MqttConstants.LEAVE)) {
            status = MqttConstants.DISCONNECTED
        } else {
            release()
        }
    }

    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {}

    /*************************************************** 订阅 ***************************************************/
    fun <T : MqttTopic> subscribeTopics(topics: MutableList<T>, onActionListener: OnActionListener<T>) {
        subscribeTopics(0, topics, onActionListener)
    }

    fun <T : MqttTopic> subscribeTopics(retryTime: Int, topics: MutableList<T>, onActionListener: OnActionListener<T>) {
        synchronized(mSubscriptionList) {
            for (topic in topics) {
                if (!mSubscriptionList.contains(topic)) {
                    mSubscriptionList.add(topic)
                }
            }
        }
        val subscribeListener = MqttActionListener(retryTime, MqttConstants.SUBSCRIBE, this, topics as MutableList<MqttTopic>, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            subscribeTopics(topics, subscribeListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, subscribeListener)
            addAction(mqttActionListener)
        }
    }

    fun <T : MqttTopic> subscribeTopic(mqttTopic: T, onActionListener: OnActionListener<T>) {
        subscribeTopic(0, mqttTopic, onActionListener)
    }

    fun <T : MqttTopic> subscribeTopic(retryTime: Int, mqttTopic: T, onActionListener: OnActionListener<T>) {
        synchronized(mSubscriptionList) {
            if (!mSubscriptionList.contains(mqttTopic)) {
                mSubscriptionList.add(mqttTopic)
            }
        }
        val subscribeListener = MqttActionListener(retryTime, MqttConstants.SUBSCRIBE, this, mqttTopic, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            subscribeTopic(mqttTopic, subscribeListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, subscribeListener)
            addAction(mqttActionListener)
        }
    }

    private fun subscribeTopic(mqttTopic: MqttTopic, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        val subscription = mqttTopic as Subscription
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                client.subscribe(mqttTopic.mqttTopic, subscription.subscriptionQoS, null, mqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            mqttActionListener.onFailure(null, throwable)
        }
    }

    private fun subscribeTopics(mqttTopics: MutableList<MqttTopic>, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                val size = mqttTopics.size
                val topicArray = arrayOfNulls<String>(size)
                val qos = IntArray(size)
                for (i in 0 until size) {
                    val topic = mqttTopics[i]
                    val subscription = topic as Subscription?
                    topicArray[i] = mqttTopics[i].mqttTopic
                    qos[i] = subscription!!.subscriptionQoS
                }
                client.subscribe(topicArray, qos, null, mqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            throwable.let { it }?.let { mqttActionListener.onFailure(null!!, it) }
        }
    }

    fun subscribe(mqttTopic: MqttTopic?, onActionListener: OnActionListener<MqttTopic>, throwable: Throwable?) {
        val subscription = mqttTopic as Subscription
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            debugMessage("[SUBSCRIBE] " + mqttTopic.mqttTopic + " succeeded.")
            try {
                client.subscribe(mqttTopic.mqttTopic, subscription.subscriptionQoS) { s, mqttMessage ->
                    val message = String(mqttMessage.payload)
                    debugMessage("[RECEIVED] " + mqttTopic.mqttTopic + ", message : " + message)
                    onActionListener.onSuccess(mqttTopic, message)
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            debugMessage("[SUBSCRIBE] " + mqttTopic.mqttTopic + " failure.")
            onActionListener.onFailure(mqttTopic, throwable)
        }
    }

    fun subscribes(mqttTopics: MutableList<MqttTopic>?, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                val size = mqttTopics!!.size
                val topicArray = arrayOfNulls<String>(size)
                val qos = IntArray(size)
                val listeners = arrayOfNulls<IMqttMessageListener>(size)
                for (i in 0 until size) {
                    val topic = mqttTopics[i]
                    val subscription = topic as Subscription
                    topicArray[i] = topic.mqttTopic
                    qos[i] = subscription.subscriptionQoS
                    debugMessage("[SUBSCRIBE] " + topicArray[i] + " succeeded.")
                    listeners[i] = IMqttMessageListener { s, mqttMessage ->
                        val message = String(mqttMessage.payload)
                        debugMessage("[RECEIVED] " + topic.mqttTopic + ", message: " + message)
                        onActionListener?.onSuccess(topic, message)
                    }
                }
                client.subscribe(topicArray, qos, listeners)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            for (topic in mqttTopics!!) {
                debugMessage("[SUBSCRIBE] " + topic.mqttTopic + " failure, " + throwable)
                onActionListener!!.onFailure(topic, throwable)
            }
        }
    }

    /*************************************************** 取消订阅 ***************************************************/
    fun <T : MqttTopic> unSubscribeTopic(topic: T, onActionListener: OnActionListener<T>) {
        val unsubscribeListener = MqttActionListener(MqttConstants.UN_SUBSCRIBE, this, topic, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            unSubscribeTopic(topic, unsubscribeListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, unsubscribeListener)
            addAction(mqttActionListener)
        }
    }

    fun <T : MqttTopic> unSubscribeTopics(mqttTopics: MutableList<T>,onActionListener: OnActionListener<T>){
        val unsubscribeListener = MqttActionListener(MqttConstants.UN_SUBSCRIBE, this, mqttTopics as MutableList<MqttTopic>, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            unSubscribeTopics(mqttTopics, unsubscribeListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, unsubscribeListener)
            addAction(mqttActionListener)
        }
    }

    private fun unSubscribeTopic(mqttTopic: MqttTopic, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                client.unsubscribe(mqttTopic.mqttTopic, null, mqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            mqttActionListener.onFailure(null!!, throwable!!)
        }
    }

    fun unSubscribe(mqttTopic: MqttTopic, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            debugMessage("[UN-SUBSCRIBE] " + mqttTopic.mqttTopic + " succeeded.")
            onActionListener?.onSuccess(mqttTopic, null)
        } else {
            debugMessage("[UN-SUBSCRIBE] " + mqttTopic.mqttTopic + " failure, " + throwable)
            onActionListener?.onFailure(mqttTopic, throwable)
        }
    }

    private fun unSubscribeTopics(mqttTopics: MutableList<MqttTopic>, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                val size = mqttTopics.size
                val topicArray = arrayOfNulls<String>(size)
                for (i in 0 until size) {
                    val topic = mqttTopics[i]
                    topicArray[i] = mqttTopics[i]!!.mqttTopic
                }
                client.unsubscribe(topicArray, null, mqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            mqttActionListener.onFailure(null!!, throwable!!)
        }
    }

    fun unSubscribes(mqttTopics: MutableList<MqttTopic>, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            for (topic in mqttTopics) {
                debugMessage("[UN-SUBSCRIBE] " + topic.mqttTopic + " succeeded.")
                onActionListener?.onSuccess(topic, null)
            }
        } else {
            for (topic in mqttTopics) {
                debugMessage("[UN-SUBSCRIBE] " + topic.mqttTopic + " failure, " + throwable)
                onActionListener!!.onFailure(topic, throwable)
            }
        }
    }

    /*************************************************** 发布 ***************************************************/
    fun <T : MqttTopic> publishTopic(topic: T?, onActionListener: OnActionListener<T>?) {
        val publishListener = MqttActionListener(MqttConstants.PUBLISH, this, topic, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            publishTopic(topic, publishListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, publishListener)
            addAction(mqttActionListener)
        }
    }

    fun <T : MqttTopic> publishTopic(retryTime: Int, topic: T?, onActionListener: OnActionListener<T>?) {
        synchronized(mPublishingList) {
            if (!mPublishingList.contains(topic)) {
                mPublishingList.add(topic!!)
            }
        }
        val publishListener = MqttActionListener(retryTime, MqttConstants.PUBLISH, this, topic, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            publishTopic(topic, publishListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, publishListener)
            addAction(mqttActionListener)
        }
    }

    fun <T : MqttTopic> publishTopics(mqttTopics:MutableList<T>?, onActionListener: OnActionListener<T>?) {
        val publishListener = MqttActionListener(MqttConstants.PUBLISH, this, mqttTopics as MutableList<MqttTopic>, weakReferenceListener(onActionListener as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            publishTopics(mqttTopics, publishListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, publishListener)
            addAction(mqttActionListener)
        }
    }

    fun <T : MqttTopic> publishTopics(retryTime: Int, mqttTopics: MutableList<T>?, onActionListener: OnActionListener<T>?) {
        val publishListener = MqttActionListener(retryTime, MqttConstants.PUBLISH, this, mqttTopics as MutableList<MqttTopic>, weakReferenceListener(onActionListener  as OnActionListener<MqttTopic>))
        if (checkStatus(MqttConstants.CONNECTED)) {
            publishTopics(mqttTopics, publishListener, null)
        } else {
            val mqttActionListener = MqttActionListener(this, publishListener)
            addAction(mqttActionListener)
        }
    }

    private fun publishTopic(mqttTopic: MqttTopic?, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        val publishing = mqttTopic as Publishing
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                var message = publishing.publishingMessage
                if (message == null) {
                    message = ""
                }
                client.publish(mqttTopic.mqttTopic, message.toByteArray(), publishing.publishingQoS, publishing.isRetained == MqttConstants.RETAINED, null, mqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            mqttActionListener.onFailure(null, throwable)
        }
    }

    private fun publishTopics(mqttTopics: MutableList<MqttTopic>, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            try {
                for (i in mqttTopics.indices) {
                    val topic = mqttTopics[i]
                    val publishing = topic as Publishing?
                    var message = publishing!!.publishingMessage
                    if (message == null) {
                        message = ""
                    }
                    client.publish(topic.mqttTopic, message.toByteArray(), publishing.publishingQoS, publishing.isRetained == MqttConstants.RETAINED, null, mqttActionListener)
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        } else {
            mqttActionListener.onFailure(null, throwable)
        }
    }

    fun publish(mqttTopic: MqttTopic?, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        val publishing = mqttTopic as Publishing
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            debugMessage("[PUBLISH] " + mqttTopic.mqttTopic + ", message: " + publishing.publishingMessage + " succeeded.")
            onActionListener?.onSuccess(mqttTopic, publishing.publishingMessage)
        } else {
            debugMessage("[PUBLISH] " + mqttTopic.mqttTopic + " failure, " + throwable)
            onActionListener?.onFailure(mqttTopic, throwable)
        }
    }

    fun publishs(mqttTopics: MutableList<MqttTopic>, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (throwable == null && checkStatus(MqttConstants.CONNECTED)) {
            for (i in mqttTopics.indices) {
                val topic = mqttTopics[i]
                val publishing = topic as Publishing
                debugMessage("[PUBLISH] " + topic.mqttTopic + ", message: " + publishing.publishingMessage + " succeeded.")
                onActionListener?.onSuccess(topic, publishing.publishingMessage)
            }
        } else {
            for (topic in mqttTopics) {
                debugMessage("[PUBLISH] " + topic.mqttTopic + " failure, " + throwable)
                onActionListener?.onFailure(topic, throwable)
            }
        }
    }

    /*************************************************** 操作 ***************************************************/
    private fun addAction(mqttActionListener: MqttActionListener) {
        val actionListener = mqttActionListener.mqttActionListener
        if (checkStatus(MqttConstants.CONNECTED)) {
            if (actionListener != null) {
                doSubAction(actionListener.mqttTopic, actionListener.mqttTopics, actionListener, null)
            }
        } else if (!checkStatus(MqttConstants.LEAVE)) {
            actionListener?.let { addSubAction(it) }
            // Re-connect when connection is disconnected
            if (checkStatus(MqttConstants.DISCONNECTED)) {
                connect(mqttActionListener)
            }
        }
    }

    private fun addSubAction(mqttActionListener: MqttActionListener) {
        synchronized(mMqttActionListeners) { mMqttActionListeners.add(mqttActionListener) }
    }

    private fun subActions(onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (onActionListener != null) {
            if (throwable == null) {
                onActionListener.onSuccess(null, "connectCallback to server")
            } else {
                onActionListener.onFailure(null, throwable)
            }
        }
        synchronized(mMqttActionListeners) {
            for (actionListener in mMqttActionListeners) {
                doSubAction(actionListener.mqttTopic, actionListener.mqttTopics, actionListener, throwable)
                mMqttActionListeners.remove(actionListener)
            }
        }
    }

    private fun doSubAction(mqttTopic: MqttTopic?, mqttTopics: MutableList<MqttTopic>?, mqttActionListener: MqttActionListener, throwable: Throwable?) {
        when (mqttActionListener.action) {
            MqttConstants.SUBSCRIBE -> mqttTopic?.let { subscribeTopic(it, mqttActionListener, throwable) }
                    ?: mqttTopics?.let { subscribeTopics(it, mqttActionListener, throwable) }
            MqttConstants.UN_SUBSCRIBE -> mqttTopic?.let { unSubscribeTopic(it, mqttActionListener, throwable) }
                    ?: mqttTopics?.let { unSubscribeTopics(it, mqttActionListener, throwable) }
            MqttConstants.PUBLISH -> mqttTopic?.let { publishTopic(it, mqttActionListener, throwable) }
                    ?: mqttTopics?.let { publishTopics(it, mqttActionListener, throwable) }
            else -> {
            }
        }
    }

    /*************************************************** 连接 ***************************************************/
    fun connect(onActionListener: OnActionListener<MqttTopic>, retryTime: Int) {
        mOnActionListener = weakReferenceListener(onActionListener)
        if (checkStatus(MqttConstants.CONNECTED)) {
            mOnActionListener!!.onSuccess(null, "$clientId is connected")
        } else if (!checkStatus(MqttConstants.CONNECTING or MqttConstants.LEAVE)) {
            Timer().schedule(object : TimerTask() {
                var i = retryTime
                override fun run() {
                    i--
                    val connectionListener = MqttActionListener(i, MqttConstants.CONNECT, this@MqttConnection, mOnActionListener)
                    connect(connectionListener)
                    if (i <= 0) {
                        cancel()
                        //重试retryTime后上报
                    }
                }
            }, 1000, retryTime * 1000.toLong())
        }
    }

    fun connect(onActionListener: OnActionListener<MqttTopic>) {
        mOnActionListener = weakReferenceListener(onActionListener)
        if (checkStatus(MqttConstants.CONNECTED)) {
            mOnActionListener!!.onSuccess(null, "$clientId is connected")
        } else if (!checkStatus(MqttConstants.CONNECTING or MqttConstants.LEAVE)) {
            val connectionListener = MqttActionListener(MqttConstants.CONNECT, this, mOnActionListener)
            connect(connectionListener)
        }
    }

    private fun connect(mqttActionListener: MqttActionListener) {
        try {
            checkNotNull(mMqttConnectOptions) { "ConnectOptions is null" }
            status = MqttConstants.CONNECTING
            debugMessage("$clientId [CONNECTING]...")
            client.connect(mMqttConnectOptions, null, mqttActionListener)
        } catch (e: MqttException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    fun connection(mqttActionListener: MqttActionListener, onActionListener: OnActionListener<MqttTopic>?, throwable: Throwable?) {
        if (status < MqttConstants.CONNECTED) {
            if (throwable == null) {
                debugMessage("$clientId [CONNECT] to mqtt server ($serverUri) succeeded.")
                status = MqttConstants.CONNECTED
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                try {
                    client.setBufferOpts(disconnectedBufferOptions)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
                subActions(onActionListener, null)
            } else {
                status = MqttConstants.DISCONNECTED
                val retryTime = mqttActionListener.retryTime()
                debugMessage(clientId + " [CONNECT] to MQTT server failure, " + throwable + ", rest retry times: " + retryTime + "s, sub action: " + mqttActionListener.mqttActionListener)
                val subMqttActionListener = mqttActionListener.mqttActionListener
                subMqttActionListener?.clearRetryTime()
                subActions(onActionListener, throwable)
            }
        }
    }

    fun disconnect() {
        if (checkStatus(MqttConstants.CONNECTING)) {
            debugMessage("$clientId [LEAVING]...")
            mLeaving = true
        } else {
            debugMessage("$clientId [DISCONNECT] to server ($serverUri)")
            status = MqttConstants.LEAVE
            try {
                mOnActionListener = null
                client.disconnect()
                mSubscriptionList.clear()
                mPublishingList.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun release() {
        try {
            client.unregisterResources()
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var status: Int = MqttConstants.INIT
        get() {
            synchronized(MqttConnection::class.java) { return field }
        }
        private set(_status) {
            synchronized(MqttConnection::class.java) { field = _status }
        }

    fun checkStatus(@ConnectionStatus _status: Int): Boolean {
        return status == _status
    }

    fun addConnectionOptions(builder: ConnectOptionsBuilder): MqttConnection {
        mMqttConnectOptions = builder.build()
        return this
    }

    private fun debugMessage(message: String) {
        if (MqttManager.DEBUG) {
            Log.d(MqttManager.MQTT_TAG, message)
        }
    }

    class ConnectOptionsBuilder {
        private var user: String? = null
        private var password: String? = null
        private var connectionTimeout = 30
        private var keepAliveInterval = 60
        private var autoReconnect = true
        private var cleanSession = false
        private var userCredentials = false
        private var socketFactory: SocketFactory? = null
        fun setUser(user: String?): ConnectOptionsBuilder {
            this.user = user
            return this
        }

        fun setPassword(password: String?): ConnectOptionsBuilder {
            this.password = password
            return this
        }

        fun setConnectionTimeout(connectionTimeout: Int): ConnectOptionsBuilder {
            this.connectionTimeout = connectionTimeout
            return this
        }

        fun setKeppAliveInterval(keepAliveInterval: Int): ConnectOptionsBuilder {
            this.keepAliveInterval = keepAliveInterval
            return this
        }

        fun setAutoReconnect(autoReconnect: Boolean): ConnectOptionsBuilder {
            this.autoReconnect = autoReconnect
            return this
        }

        fun setCleanSession(cleanSession: Boolean): ConnectOptionsBuilder {
            this.cleanSession = cleanSession
            return this
        }

        fun setUserCredentials(userCredentials: Boolean): ConnectOptionsBuilder {
            this.userCredentials = userCredentials
            return this
        }

        fun setSocketFactory(socketFactory: SSLSocketFactory?): ConnectOptionsBuilder {
            this.socketFactory = socketFactory
            return this
        }

        fun build(): MqttConnectOptions {
            if (userCredentials) {
                if (user == null) {
                    throw NullPointerException("User can't be null")
                }
                if (password == null) {
                    throw NullPointerException("Password can't be null")
                }
            }
            val connectOptions = MqttConnectOptions()
            if (userCredentials) {
                connectOptions.userName = user
                connectOptions.password = password!!.toCharArray()
            }
            connectOptions.connectionTimeout = connectionTimeout
            connectOptions.keepAliveInterval = keepAliveInterval
            connectOptions.isAutomaticReconnect = autoReconnect
            connectOptions.isCleanSession = cleanSession
            if (socketFactory != null) {
                connectOptions.socketFactory = socketFactory
            }
            return connectOptions
        }
    }

    companion object {
        private const val RETRY_DELAY_TIME = 5000L

        @JvmOverloads
        fun createConnection(context: Context?, serverUri: String, clientId: String, sslConnection: Boolean = false, port: Int = 19883): MqttConnection {
            val uri: String
            uri = if (sslConnection) {
                MqttConstants.SSL + serverUri + ":" + port
            } else {
                MqttConstants.TCP + serverUri + ":" + port
            }
            // See https://github.com/eclipse/paho.mqtt.android/issues/185
            val client = MqttAndroidClient(context, uri, clientId, MemoryPersistence(), MqttAndroidClient.Ack.AUTO_ACK)
            return MqttConnection(client, uri, clientId)
        }

        @JvmOverloads
        fun createConnectionWithTimeStamp(context: Context?, serverUri: String, clientId: String, sslConnection: Boolean = false, port: Int = 19883): MqttConnection {
            val timestampId = clientId + instance!!.getTimeStamp(clientId)
            return createConnection(context, serverUri, timestampId, sslConnection, port)
        }
    }

    init {
        client.setCallback(this)
        this.serverUri = serverUri
        this.clientId = clientId
    }
}