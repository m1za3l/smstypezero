/**
 * Copyright (C) 2016 Roman Khassraf.
 *
 *
 * This file is part of GAT-App.
 *
 *
 * GAT-App is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * GAT-App is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with GAT-App.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.example.smstypezero.silent

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.telephony.gsm.SmsManager
import android.text.format.Formatter
import android.text.method.ScrollingMovementMethod
import android.util.Patterns
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.smstypezero.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.StringTokenizer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class ServerModeActivity : Activity() {
    private enum class Status {
        STOPPED, RUNNING
    }

    private val context: Context = this
    private var textViewStatus: TextView? = null
    private var textViewOutput: TextView? = null
    private var editTextPort: EditText? = null
    private var button: Button? = null
    private var status = Status.STOPPED
    private var port = 0
    private var ipAddress: String? = null
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var updateUIHandler: Handler? = null
    private var updateUIOutputHandler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_mode)
        textViewStatus = findViewById<View>(R.id.statusTextView) as TextView
        textViewOutput = findViewById<View>(R.id.outputTextView) as TextView
        editTextPort = findViewById<View>(R.id.portEditText) as EditText
        button = findViewById<View>(R.id.button) as Button
        textViewOutput!!.movementMethod = ScrollingMovementMethod()
        textViewStatus!!.text = Status.STOPPED.name
        editTextPort!!.setText("8008")
        val wifiMgr = getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        ipAddress = Formatter.formatIpAddress(ip)
        updateUIHandler = Handler()
        updateUIOutputHandler = Handler()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_server_mode, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    /**
     * Handles click on the Start / Stop button according to the status.
     *
     * @param view the view.
     */
    fun onButtonClick(view: View?) {
        if (status == Status.STOPPED) {
            startServer()
        } else {
            stopServer()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (status != Status.STOPPED) {
                stopServer()
            }
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton(
            "Ok"
        ) { dialog, id -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun startServer() {
        if (editTextPort!!.text.toString() == "") {
            showAlert("Empty port number", "You must set a valid port number!")
            return
        }
        var port = 0
        port = try {
            Integer.valueOf(editTextPort!!.text.toString())
        } catch (e: NumberFormatException) {
            showAlert("Invalid port number", "You must set a valid port number!")
            return
        }
        if (port <= 0 || port > 65535) {
            showAlert("Invalid port number", "You must set a valid port number!")
            return
        }
        this.port = port
        serverThread = Thread(ServerThread())
        serverThread!!.start()
    }

    private fun stopServer() {
        try {
            serverSocket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        serverThread!!.interrupt()
        status = Status.STOPPED
        updateUIHandler!!.post(updateUIThread(false))
        updateUIOutputHandler!!.post(updateUIOutputThread("Server stopped."))
    }

    private inner class ServerThread : Runnable {
        var threads: MutableList<ReadThread> = ArrayList()
        override fun run() {
            var socket: Socket? = null
            try {
                serverSocket = ServerSocket(port)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            updateUIHandler!!.post(updateUIThread(true))
            status = Status.RUNNING
            updateUIOutputHandler!!.post(updateUIOutputThread("Server started."))
            while (!Thread.currentThread().isInterrupted) {
                try {
                    socket = serverSocket!!.accept()
                    val remoteIp = socket.remoteSocketAddress.toString()
                    updateUIOutputHandler!!.post(updateUIOutputThread("New Connection from $remoteIp"))
                    val readThread = ReadThread(socket)
                    val t = Thread(readThread)
                    threads.add(readThread)
                    t.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            for (t in threads) {
                t.closeSocket()
            }
        }
    }

    private inner class ReadThread(private val clientSocket: Socket?) : Runnable {
        private var bufferedReader: BufferedReader? = null
        private val remoteIp: String
        private var writeThread: WriteThread? = null
        private val msgQueue: BlockingQueue<String>
        private val serverSentReceiver: SMSServerSentReceiver
        private val serverDeliveryReceiver: SMSServerDeliveryReceiver

        init {
            remoteIp = clientSocket!!.remoteSocketAddress.toString() + ": "
            try {
                bufferedReader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            msgQueue = LinkedBlockingQueue()
            serverSentReceiver = SMSServerSentReceiver()
            serverDeliveryReceiver = SMSServerDeliveryReceiver()
        }

        override fun run() {
            writeThread = WriteThread(clientSocket, msgQueue)
            val t = Thread(writeThread)
            t.start()
            registerReceiver(serverSentReceiver, IntentFilter(ACTION_SMS_SENT))
            registerReceiver(serverDeliveryReceiver, IntentFilter(ACTION_SMS_DELIVERED))
            var read: String? = null
            while (!Thread.currentThread().isInterrupted && clientSocket!!.isConnected) {
                try {
                    if (bufferedReader!!.readLine().also { read = it } != null) {
                        if (read!!.startsWith("sms-send")) {
                            val tokenizer = StringTokenizer(read, "#", false)
                            val tokenCount = tokenizer.countTokens()
                            if (tokenCount < 3) {
                                updateUIOutputHandler!!.post(updateUIOutputThread(remoteIp + "Invalid command."))
                            } else {
                                // remove prefix
                                tokenizer.nextToken()
                                val typeS = tokenizer.nextToken()
                                var type: MessageType? = null
                                try {
                                    val typeNr = Integer.valueOf(typeS)
                                    type = MessageType.values()[typeNr]
                                } catch (ex: NumberFormatException) {
                                    updateUIOutputHandler!!.post(updateUIOutputThread(remoteIp + "Invalid command."))
                                }
                                val phoneNr = tokenizer.nextToken()
                                if (type != null) {
                                    val smsManager = SmsManager.getDefault()
                                    var cmd: String? = ".gat#" + type.value.toString() + "#"
                                    if (tokenCount == 4 && type.hasText()) {
                                        val text = tokenizer.nextToken()
                                        cmd += text
                                    }
                                    var pendingDeliveryIntent: PendingIntent? = null
                                    if (type.hasDeliveryReport()) {
                                        val deliveryIntent = Intent(ACTION_SMS_DELIVERED)
                                        deliveryIntent.putExtra("RECIPIENT", phoneNr)
                                        pendingDeliveryIntent = PendingIntent.getBroadcast(
                                            context,
                                            intentId++,
                                            deliveryIntent,
                                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                        )
                                    }
                                    val sentIntent = Intent(ACTION_SMS_SENT)
                                    sentIntent.putExtra("RECIPIENT", phoneNr)
                                    sentIntent.putExtra("TYPE", type.value)
                                    val pendingSentIntent = PendingIntent.getBroadcast(
                                        context,
                                        intentId++,
                                        sentIntent,
                                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    if (isValidPhoneNr(phoneNr)) {
                                        updateUIOutputHandler!!.post(updateUIOutputThread(remoteIp + "Sending..."))
                                        smsManager.sendTextMessage(
                                            phoneNr,
                                            null,
                                            cmd,
                                            pendingSentIntent,
                                            pendingDeliveryIntent
                                        )
                                    } else {
                                        try {
                                            updateUIOutputHandler!!.post(
                                                updateUIOutputThread(
                                                    remoteIp + "Invalid phone nr"
                                                )
                                            )
                                            msgQueue.put("sms-send#$phoneNr#NOK#")
                                        } catch (e: InterruptedException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        } else if (read == "quit") {
                            updateUIOutputHandler!!.post(updateUIOutputThread(remoteIp + "quit"))
                            closeSocket()
                            return
                        } else {
                            updateUIOutputHandler!!.post(updateUIOutputThread(remoteIp + "Invalid command."))
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            unregisterReceiver(serverSentReceiver)
            unregisterReceiver(serverDeliveryReceiver)
            closeSocket()
        }

        private fun isValidPhoneNr(phoneNr: String): Boolean {
            return !phoneNr.isEmpty() && Patterns.PHONE.matcher(phoneNr).matches()
        }

        fun closeSocket() {
            try {
                clientSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private inner class SMSServerSentReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                val bundle = intent.extras
                if (action == "SMS_SENT" && bundle != null) {
                    val number = bundle.getString("RECIPIENT")
                    val typeNr = bundle.getInt("TYPE")
                    val type = MessageType.values()[typeNr]
                    var typeDesc = "Unknown message type"
                    when (type) {
                        MessageType.SMS -> typeDesc = "Default SMS"
                        MessageType.SMS_DELIVERY_REPORT -> typeDesc =
                            "Default SMS with delivery report"

                        MessageType.CLASS0 -> typeDesc = "Class 0"
                        MessageType.CLASS0_DELIVERY_REPORT -> typeDesc =
                            "Class 0 with delivery report"

                        MessageType.SILENT_TYPE0 -> typeDesc = "Type 0"
                        MessageType.SILENT_TYPE0_DELIVERY_REPORT -> typeDesc = "Type 0 Ping"
                        MessageType.MWIA -> typeDesc = "MWI Activate"
                        MessageType.MWID -> typeDesc = "MWI Deactivate"
                        MessageType.MWID_DELIVERY_REPORT -> typeDesc = "MWI Ping"
                        else -> {}
                    }
                    if (number != null) {
                        val values = ContentValues()
                        values.put("address", number)
                        when (resultCode) {
                            RESULT_OK -> try {
                                msgQueue.put("sms-send#$number#OK#")
                                updateUIOutputHandler!!.post(
                                    updateUIOutputThread(
                                        "$remoteIp$typeDesc sent to $number"
                                    )
                                )
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                            else -> try {
                                msgQueue.put("sms-send#$number#NOK#")
                                updateUIOutputHandler!!.post(
                                    updateUIOutputThread(
                                        "$remoteIp$typeDesc not sent to $number"
                                    )
                                )
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        private inner class SMSServerDeliveryReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                val bundle = intent.extras
                if (action == "SMS_DELIVERED" && bundle != null) {
                    val number = bundle.getString("RECIPIENT")
                    if (number != null) {
                        val values = ContentValues()
                        values.put("address", number)
                        when (resultCode) {
                            RESULT_OK -> try {
                                msgQueue.put("sms-delivery#$number#OK#")
                                updateUIOutputHandler!!.post(
                                    updateUIOutputThread(
                                        remoteIp + "Message delivered to " + number
                                    )
                                )
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                            RESULT_CANCELED -> try {
                                msgQueue.put("sms-delivery#$number#NOK#")
                                updateUIOutputHandler!!.post(
                                    updateUIOutputThread(
                                        remoteIp + "Message not delivered to " + number
                                    )
                                )
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                            else -> try {
                                msgQueue.put("sms-delivery#$number#NOK#")
                                updateUIOutputHandler!!.post(
                                    updateUIOutputThread(
                                        remoteIp + "Message with unclear status to " + number
                                    )
                                )
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class WriteThread(
        private val clientSocket: Socket?,
        messages: BlockingQueue<String>
    ) : Runnable {
        private var printWriter: PrintWriter? = null
        var messages: BlockingQueue<String>

        init {
            try {
                val ostream = clientSocket!!.getOutputStream()
                printWriter = PrintWriter(ostream, true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            this.messages = messages
        }

        override fun run() {
            while (!Thread.currentThread().isInterrupted && clientSocket!!.isConnected) {
                var output: String? = null
                try {
                    output = messages.take()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                if (output != null && printWriter != null) {
                    printWriter!!.println(output)
                    printWriter!!.flush()
                }
            }
        }
    }

    private inner class updateUIThread(private val running: Boolean) : Runnable {
        override fun run() {
            if (running) {
                textViewStatus!!.text = String.format(
                    "%s (%s:%s)",
                    Status.RUNNING.toString(),
                    ipAddress, port.toString()
                )
                editTextPort!!.isEnabled = false
                button!!.text = "Stop"
            } else {
                textViewStatus!!.text =
                    Status.STOPPED.toString()
                editTextPort!!.isEnabled = true
                button!!.text = "Start"
            }
        }
    }

    private inner class updateUIOutputThread(private val msg: String) : Runnable {
        override fun run() {
            textViewOutput!!.append(
                """
    
    ${msg}
    """.trimIndent()
            )
        }
    }

    companion object {
        private const val ACTION_SMS_DELIVERED = "SMS_DELIVERED"
        private const val ACTION_SMS_SENT = "SMS_SENT"
        private var intentId = 7000000
    }
}