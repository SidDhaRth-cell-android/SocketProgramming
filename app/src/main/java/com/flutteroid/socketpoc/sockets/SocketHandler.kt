import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URISyntaxException
import kotlin.math.pow

class SocketHandler(private val listener: SocketListener) {

  private var socket: Socket? = null
  private var reconnectAttempts = 0
  private val maxReconnectAttempts = 5 // Maximum number of retry attempts
  private val initialReconnectDelay = 2000L // Initial delay in milliseconds

  init {
    try {
      socket = IO.socket("https://b868-103-55-90-105.ngrok-free.app")
    } catch (e: URISyntaxException) {
      e.printStackTrace()
    }
  }

  fun connect() {
    socket?.let {
      it.connect()
      it.on(Socket.EVENT_CONNECT, onConnect)
      it.on(Socket.EVENT_DISCONNECT, onDisconnect)
      it.on(EVENT_MESSAGE_RECEIVED, onMessageReceived)
      it.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
    }
  }

  private val onConnectError = Emitter.Listener { args ->
    val error = args[0] as Exception
    CoroutineScope(Dispatchers.Main).launch {
      Log.e(TAG, "Connection error: ${error.message}")
      listener.onError("Connection error: ${error.message}")
      attemptReconnect()
    }
  }

  fun disconnect() {
    socket?.let {
      if (it.connected()) {
        it.disconnect()
        it.off(Socket.EVENT_CONNECT, onConnect)
        it.off(Socket.EVENT_DISCONNECT, onDisconnect)
      }
    }
  }

  private fun attemptReconnect() {
    if (reconnectAttempts < maxReconnectAttempts) {
      CoroutineScope(Dispatchers.IO).launch {
        delay(calculateRetryDelay())
        reconnectAttempts++
        Log.d(TAG, "Retrying connection attempt $reconnectAttempts")
        socket?.connect()
      }
    } else {
      CoroutineScope(Dispatchers.Main).launch {
        Log.e(TAG, "Max reconnect attempts reached")
        listener.onError("Unable to reconnect after $maxReconnectAttempts attempts")
      }
    }
  }

  private fun calculateRetryDelay(): Long {
    return (initialReconnectDelay * 2.0.pow(reconnectAttempts.toDouble())).toLong()
  }

  fun emitMessage(eventName: String, message: String) {
    socket?.let {
      if (it.connected()) {
        it.emit(eventName, message)
      }
    }
  }

  private val onConnect = Emitter.Listener {
    CoroutineScope(Dispatchers.Main).launch {
      listener.onMessageReceived("Connected")
    }
  }

  private val onDisconnect = Emitter.Listener {
    CoroutineScope(Dispatchers.Main).launch {
      listener.onMessageReceived("Disconnected")
      attemptReconnect()
    }
  }

  private val onMessageReceived = Emitter.Listener { args ->
    val message = args[0] as String
    Log.d(TAG, "Message received: $message")
    CoroutineScope(Dispatchers.Main).launch {
      listener.onMessageReceived(message)
    }
  }

  interface SocketListener {
    fun onMessageReceived(message: String)
    fun onError(errorMessage: String)
  }

  companion object {
    private const val TAG = "SocketHandler"
    private const val EVENT_MESSAGE_RECEIVED = "messageReceived"
  }
}
