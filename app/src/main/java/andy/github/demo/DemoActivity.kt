package andy.github.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import andy.github.mqtt.MqttManager.Companion.instance
import andy.github.mqtt.external.MqttTopic
import andy.github.mqtt.internal.MqttConnection
import andy.github.mqtt.internal.MqttConnection.ConnectOptionsBuilder
import andy.github.mqtt.internal.OnActionListener
import andy.github.demo.topic.DeviceTopic
import andy.github.demo.topic.DoorTopic
import andy.github.demo.topic.LightTopic
import andy.github.demo.topic.WindowTopic
import kotlinx.android.synthetic.main.activity_demo.*
import java.util.*

/**
 * 记得写注释哦
 *
 * @author Andy
 * @since 2020-07-07 14:09
 */
class DemoActivity : AppCompatActivity() {
    var mConnection: MqttConnection? = null
    var mDeviceTopicList: MutableList<DeviceTopic> = ArrayList()
    var mLightTopic = LightTopic()
    var mWindowTopic = WindowTopic()
    var mDoorTopic = DoorTopic()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        initView()
    }

    override fun onResume() {
        super.onResume()
        instance!!.onResume(CLIENT_ID)
    }

    override fun onPause() {
        super.onPause()
        instance!!.onPause(CLIENT_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance!!.release()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        connect_btn.setOnClickListener { view: View? -> createConnection() }
        disconnect_btn.setOnClickListener { view: View? -> disconnection() }
        light_open_btn.setOnClickListener { view: View? ->
            mLightTopic.message = "open light"
            mConnection!!.publishTopic(mLightTopic,object : OnActionListener<LightTopic>{
                override fun onSuccess(topic: LightTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        light_close_btn.setOnClickListener { view: View? ->
            mLightTopic.message = "close light"
            mConnection!!.publishTopic(mLightTopic, object : OnActionListener<LightTopic>{
                override fun onSuccess(topic: LightTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        window_open_btn.setOnClickListener { view: View? ->
            mWindowTopic.message = "open window"
            mConnection!!.publishTopic(mWindowTopic, object : OnActionListener<WindowTopic>{
                override fun onSuccess(topic: WindowTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: WindowTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        window_close_btn.setOnClickListener { view: View? ->
            mWindowTopic.message = "close window"
            mConnection!!.publishTopic(mWindowTopic, object : OnActionListener<WindowTopic>{
                override fun onSuccess(topic: WindowTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: WindowTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        door_open_btn.setOnClickListener { view: View? ->
            mDoorTopic.message = "open door"
            mConnection!!.publishTopic(mDoorTopic, object : OnActionListener<DoorTopic>{
                override fun onSuccess(topic: DoorTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: DoorTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        door_close_btn.setOnClickListener { view: View? ->
            mDoorTopic.message = "close door"
            mConnection!!.publishTopic(mDoorTopic, object : OnActionListener<DoorTopic>{
                override fun onSuccess(topic: DoorTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: DoorTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        only_one_subscribe_btn.setOnClickListener { view: View? ->
            mDeviceTopicList.clear()
            mDeviceTopicList.add(LightTopic("Light", "light is opened"))
            mDeviceTopicList.add(WindowTopic("Window", "light is opened"))
            mDeviceTopicList.add(DoorTopic("Door", "light is opened"))
            mConnection!!.subscribeTopics(mDeviceTopicList, object : OnActionListener<DeviceTopic> {
                override fun onSuccess(topic: DeviceTopic?, message: String?) {
                    runOnUiThread { state_tv.text = "已连接,已订阅${mDeviceTopicList.size}种主题" }
                }

                override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }
            })
        }
        only_one_publish_btn.setOnClickListener { view: View? ->
            mDeviceTopicList.clear()
            mDeviceTopicList.add(LightTopic("Light", "light is opened"))
            mDeviceTopicList.add(WindowTopic("Window", "window is opened"))
            mDeviceTopicList.add(DoorTopic("Door", "door is opened"))
            mConnection!!.publishTopics(mDeviceTopicList, object : OnActionListener<DeviceTopic>{
                override fun onSuccess(topic: DeviceTopic?, message: String?) {
                    runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                    runOnUiThread { message_tv.text = message }
                }

                override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                    TODO("Not yet implemented")
                }

            })
        }
    }

    private fun subscribeSingle(){
        mConnection!!.unSubscribeTopic(LightTopic(), object : OnActionListener<LightTopic> {
            override fun onSuccess(topic: LightTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: LightTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })

        mConnection!!.unSubscribeTopics(mDeviceTopicList,object : OnActionListener<DeviceTopic>{
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })

        mConnection!!.publishTopics(mDeviceTopicList,object : OnActionListener<DeviceTopic>{
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })

    }

    private fun subscribeMulti(){
        mConnection!!.subscribeTopics(mDeviceTopicList, object : OnActionListener<DeviceTopic> {
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {
                TODO("Not yet implemented")
            }
        })



    }

    private fun createConnection() {
        if (mConnection == null) {
            mConnection = MqttConnection
                    .createConnection(applicationContext, IP, CLIENT_ID)
                    .addConnectionOptions(ConnectOptionsBuilder()
                            .setUserCredentials(false)
                            .setConnectionTimeout(30)
                            .setKeppAliveInterval(10))
        }
        mConnection!!.connect(object : OnActionListener<MqttTopic> {
            override fun onSuccess(topic: MqttTopic?, message: String?) {
                subscribeTopics()
                runOnUiThread { state_tv.text = "已连接,已订阅${mDeviceTopicList.size}种主题" }
            }

            override fun onFailure(topic: MqttTopic?, throwable: Throwable?) {
                runOnUiThread { state_tv.text = "断开连接" }
            }
        })
    }

    private fun disconnection() {
        if (mConnection == null) {
            return
        }
        mConnection!!.disconnect()
        mConnection = null
        mDeviceTopicList.clear()
        state_tv.text = "断开连接"
        topic_tv.text = ""
        message_tv.text = ""
    }

    private fun subscribeTopics() {
        mDeviceTopicList.add(mLightTopic)
        mDeviceTopicList.add(mWindowTopic)
        mDeviceTopicList.add(mDoorTopic)
        mConnection!!.subscribeTopics(mDeviceTopicList, object : OnActionListener<DeviceTopic> {
            override fun onSuccess(topic: DeviceTopic?, message: String?) {
                runOnUiThread { topic_tv.text = topic!!.mqttTopic }
                runOnUiThread { message_tv.text = message }
            }

            override fun onFailure(topic: DeviceTopic?, throwable: Throwable?) {}
        })
    }

    companion object {
        private const val IP = "118.118.118.118"
        private const val CLIENT_ID = "X00001"
    }
}