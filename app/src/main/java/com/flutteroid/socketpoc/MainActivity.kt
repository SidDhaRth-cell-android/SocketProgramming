package com.flutteroid.socketpoc

import SocketHandler
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SocketHandler.SocketListener {


  private lateinit var mSocket: SocketHandler


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    mSocket = SocketHandler(this)
    mSocket.connect()

    findViewById<Button>(R.id.send_message).setOnClickListener {
      mSocket.emitMessage("message", "Message from Device")
    }
  }

  override fun onPause() {
    super.onPause()
    mSocket.disconnect()
  }

  override fun onDestroy() {
    super.onDestroy()
    mSocket.disconnect()
  }

  override fun onMessageReceived(message: String) {
    findViewById<TextView>(R.id.status).text = message
  }

  override fun onError(errorMessage: String) {
    findViewById<TextView>(R.id.status).text = errorMessage
  }
}