/**
 * Copyright (C) 2016 Roman Khassraf.
 *
 * This file is part of GAT-App.
 *
 * GAT-App is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GAT-App is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GAT-App.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 */
package com.example.smstypezero.silent

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.gsm.SmsManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.example.smstypezero.R

class SendClass0SMSActivity : Activity() {
    private var textViewRecipientNr: TextView? = null
    private var textViewMessage: TextView? = null
    private var checkBoxDeliveryReport: CheckBox? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_class0_sms)
        context = applicationContext
        textViewRecipientNr = findViewById<View>(R.id.editTextPhone) as TextView
        textViewMessage = findViewById<View>(R.id.editTextMessage) as TextView
        checkBoxDeliveryReport = findViewById<View>(R.id.checkBoxDelivery) as CheckBox
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_smsmessage, menu)
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

    fun onButtonContactsClicked(view: View?) {
        try {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, PICK_CONTACT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start contact pick: " + e.message)
        }
    }

    fun onButtonSendClicked(view: View?) {
        val smsManager = SmsManager.getDefault()
        val recipientNr = textViewRecipientNr!!.text.toString()
        var cmd = ".gat#"
        var pendingDeliveryIntent: PendingIntent? = null
        var type: MessageType? = null
        if (checkBoxDeliveryReport!!.isChecked) {
            cmd += MessageType.CLASS0_DELIVERY_REPORT.value.toString() + "#"
            type = MessageType.CLASS0_DELIVERY_REPORT
            val deliveryIntent = Intent(ACTION_SMS_DELIVERED)
            deliveryIntent.putExtra("RECIPIENT", recipientNr)
            pendingDeliveryIntent = PendingIntent.getBroadcast(
                context,
                intentId++,
                deliveryIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            cmd += MessageType.CLASS0.value.toString() + "#"
            type = MessageType.CLASS0
        }
        cmd += textViewMessage!!.text.toString()
        val sentIntent = Intent(ACTION_SMS_SENT)
        sentIntent.putExtra("RECIPIENT", recipientNr)
        sentIntent.putExtra("TYPE", type.value)
        val pendingSentIntent = PendingIntent.getBroadcast(
            context,
            intentId++,
            sentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        smsManager.sendTextMessage(recipientNr, null, cmd, pendingSentIntent, pendingDeliveryIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            PICK_CONTACT -> if (resultCode == RESULT_OK) {
                val contactData = intent.data
                val c = managedQuery(contactData, null, null, null, null)
                if (c.moveToFirst()) {
                    val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val hasPhone =
                        c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    if (hasPhone.equals("1", ignoreCase = true)) {
                        val phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null, null
                        )
                        phones!!.moveToFirst()
                        val cNumber = phones.getString(phones.getColumnIndex("data1"))
                        textViewRecipientNr!!.text = cNumber
                    }
                }
            }

            else -> {}
        }
    }

    companion object {
        private var context: Context? = null
        private var intentId = 200000
        private const val TAG = "GAT-App"
        private const val ACTION_SMS_DELIVERED = "SMS_DELIVERED"
        private const val ACTION_SMS_SENT = "SMS_SENT"
        private const val PICK_CONTACT = 1
    }
}