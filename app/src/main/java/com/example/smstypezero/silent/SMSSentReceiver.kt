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
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class SMSSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val bundle = intent.extras
        if (action == "SMS_SENT" && bundle != null) {
            val number = bundle.getString("RECIPIENT")
            val typeNr = bundle.getInt("TYPE")
            val type = MessageType.values()[typeNr]
            var typeDesc = "Unknown message type"
            when (type) {
                MessageType.SMS -> typeDesc = "SMS"
                MessageType.SMS_DELIVERY_REPORT -> typeDesc = "SMS with delivery report"
                MessageType.CLASS0 -> typeDesc = "Class 0"
                MessageType.CLASS0_DELIVERY_REPORT -> typeDesc = "Class 0 with delivery report"
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
                    Activity.RESULT_OK -> {
                        Toast.makeText(
                            context, "$typeDesc sent to $number",
                            Toast.LENGTH_LONG
                        ).show()
                        values.put("body", "GAT: $typeDesc message sent to $number")
                        context.contentResolver.insert(Uri.parse("content://sms/sent"), values)
                    }

                    else -> {
                        Toast.makeText(
                            context, "$typeDesc not sent to $number",
                            Toast.LENGTH_LONG
                        ).show()
                        values.put("body", "GAT: $typeDesc not sent to $number")
                        context.contentResolver.insert(Uri.parse("content://sms/sent"), values)
                    }
                }
            }
        }
    }
}