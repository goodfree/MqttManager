package andy.github.mqtt.internal

import andy.github.mqtt.MqttConstants
import andy.github.mqtt.annotation.Action
import andy.github.mqtt.external.MqttTopic
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * MQTT 连接监听
 *
 * @author Andy
 * @since 2020-07-06 17:49
 */
class MqttActionListener : IMqttActionListener {
    // The mqtt action retry time
    var retryTime: Int
        private set

    //Action
    @Action
    var action: Int

    //Mqtt Connection
    var connection: MqttConnection

    //Subscription
    var mqttTopic: MqttTopic? = null

    // Subscriptions
    var mqttTopics: MutableList<MqttTopic>? = null

    // The callback listener for mqtt action listener
    var onActionListener: OnActionListener<MqttTopic>? = null

    // The mqtt action listener
    var mqttActionListener: MqttActionListener? = null

    /**
     * Constructor for [CONNECT] action listener with retry mechanism
     * @param action            [Action]
     * @param connection        [MqttConnection]
     * @param onActionListener  [OnActionListener]
     */
    constructor(@Action action: Int, connection: MqttConnection, onActionListener: OnActionListener<MqttTopic>?) : this(0, action, connection, onActionListener)

    /**
     * Constructor for [CONNECT] action listener with retry mechanism
     * @param retryTime         mRetryTime
     * @param action            [Action]
     * @param connection        [MqttConnection]
     * @param onActionListener  [OnActionListener]
     */
    constructor(retryTime: Int, @Action action: Int, connection: MqttConnection, onActionListener: OnActionListener<MqttTopic>?) {
        this.retryTime = retryTime
        this.action = action
        this.connection = connection
        this.onActionListener = onActionListener
    }

    /**
     * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] action listener without retry mechanism
     * @param action                [Action]
     * @param connection            [MqttConnection]
     * @param mqttTopic             [MqttTopic]
     * @param onActionListener      [OnActionListener]
     */
    constructor(@Action action: Int, connection: MqttConnection, mqttTopic: MqttTopic?, onActionListener: OnActionListener<MqttTopic>?) : this(0, action, connection, mqttTopic, onActionListener)

    /**
     * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] action listener with retry mechanism
     * @param retryTime             mRetryTime
     * @param action                [Action]
     * @param connection            [MqttConnection]
     * @param mqttTopic             [MqttTopic]
     * @param onActionListener      [OnActionListener]
     */
    constructor(retryTime: Int, @Action action: Int, connection: MqttConnection, mqttTopic: MqttTopic?, onActionListener: OnActionListener<MqttTopic>?) {
        this.retryTime = retryTime
        this.action = action
        this.connection = connection
        this.mqttTopic = mqttTopic
        this.onActionListener = onActionListener
    }

    /**
     * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] action listener without retry mechanism
     * @param action                [Action]
     * @param connection            [MqttConnection]
     * @param mqttTopics            [MqttTopic]
     * @param onActionListener      [OnActionListener]
     */
    constructor(@Action action: Int, connection: MqttConnection, mqttTopics: MutableList<MqttTopic>?, onActionListener: OnActionListener<MqttTopic>?) : this(0, action, connection, mqttTopics, onActionListener)

    /**
     * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] action listener without retry mechanism
     * @param retryTime             mRetryTime
     * @param action                [Action]
     * @param connection            [MqttConnection]
     * @param mqttTopics            mMqttTopics
     * @param onActionListener      [OnActionListener]
     */
    constructor(retryTime: Int, @Action action: Int, connection: MqttConnection, mqttTopics: MutableList<MqttTopic>?, onActionListener: OnActionListener<MqttTopic>?) {
        this.retryTime = retryTime
        this.action = action
        this.connection = connection
        this.mqttTopics = mqttTopics
        this.onActionListener = onActionListener
    }

    /**
     * Constructor for connect action to wrapper the other action listener
     * @param connection                [MqttConnection]
     * @param mqttActionListener        [MqttActionListener]
     */
    constructor(connection: MqttConnection, mqttActionListener: MqttActionListener) {
        this.connection = connection
        this.mqttActionListener = mqttActionListener
        this.action = MqttConstants.CONNECT
        retryTime = mqttActionListener!!.retryTime
    }

    fun retryTime(): Int {
        if (retryTime == ALWAYS_RETRY) {
            return 1
        } else if (retryTime > 0) {
            retryTime--
        }
        return retryTime
    }

    fun clearRetryTime() {
        retryTime = 0
    }

    /**************************************** Success ****************************************/
    private fun connectionSuccess() {
        this.onActionListener?.let { connection.connection(this,it,null) }
    }

    private fun subscribeSuccess() {
        mqttTopic?.let { this.onActionListener?.let { it1 -> connection.subscribe(it, it1, null) } }
                ?: mqttTopics?.let { this.onActionListener?.let { it1 -> connection.subscribes(it, it1, null) } }

    }

    private fun unSubscribeSuccess() {
        mqttTopic?.let { this.onActionListener?.let { it1 -> connection.unSubscribe(it, it1, null) } }
                ?: mqttTopics?.let { this.onActionListener?.let { it1 -> connection.unSubscribes(it, it1, null) } }
    }

    private fun publishSuccess() {
        mqttTopic?.let { this.onActionListener?.let { it1 -> connection.publish(it, it1, null) } }
                ?: mqttTopics?.let { this.onActionListener?.let { it1 -> connection.publishs(it, it1, null) } }
    }

    /**************************************** Failure ****************************************/
    private fun connectionFailure(throwable: Throwable?) {
        connection.connection(this, this.onActionListener, throwable)
    }

    private fun subscribeFailure(throwable: Throwable?) {
        if (mqttTopic != null) {
            this.onActionListener?.let { connection.subscribe(this.mqttTopic!!, it, throwable) }
        } else if (mqttTopics != null) {
            this.onActionListener?.let { connection.subscribes(this.mqttTopics!!, it, throwable) }
        }
    }

    private fun unSubscribeFailure(throwable: Throwable?) {
        if (mqttTopic != null) {
            this.onActionListener?.let { connection.unSubscribe(mqttTopic!!,it,throwable) }
        } else if (mqttTopics != null) {
            this.onActionListener?.let { connection.unSubscribes(mqttTopics!!,it,throwable) }
        }
    }

    private fun publishFailure(throwable: Throwable?) {
        if (mqttTopic != null) {
            this.onActionListener?.let { connection.publish(this.mqttTopic!!, it, throwable) }
        } else if (mqttTopics != null) {
            connection.publishs(this.mqttTopics!!, this.onActionListener, throwable)
        }
    }

    override fun onSuccess(asyncActionToken: IMqttToken) {
        when (action) {
            MqttConstants.CONNECT -> connectionSuccess()
            MqttConstants.SUBSCRIBE -> subscribeSuccess()
            MqttConstants.UN_SUBSCRIBE -> unSubscribeSuccess()
            MqttConstants.PUBLISH -> publishSuccess()
            else -> {
            }
        }
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        when (action) {
            MqttConstants.CONNECT -> connectionFailure(if (asyncActionToken!!.exception != null) asyncActionToken.exception else exception)
            MqttConstants.SUBSCRIBE -> subscribeFailure(exception)
            MqttConstants.UN_SUBSCRIBE -> unSubscribeFailure(exception)
            MqttConstants.PUBLISH -> publishFailure(exception)
            else -> {
            }
        }
    }

    companion object {
        // Always retry flag
        const val ALWAYS_RETRY = -1
    }
}