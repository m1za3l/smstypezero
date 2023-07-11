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
package com.example.smstypezero

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smstypezero.silent.MessageType
import com.example.smstypezero.silent.NetworkInfoActivity
import com.example.smstypezero.silent.SendClass0SMSActivity
import com.example.smstypezero.silent.SendMessagelessSMSActivity
import com.example.smstypezero.silent.ServerModeActivity

class MainActivity : AppCompatActivity() {

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
                permissions -> // Handle Permission granted/rejected
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted && permissionName == "android.permission.SEND_SMS") {
                    //sendMsm()
                }

            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityResultLauncher.launch(arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        ))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_manual_main, menu)
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

    fun buttonClass0Clicked(view: View?) {
        val intent = Intent(this, SendClass0SMSActivity::class.java)
        startActivity(intent)
    }

    fun buttonType0Clicked(view: View?) {
        val intent = Intent(this, SendMessagelessSMSActivity::class.java)
        intent.putExtra("MESSAGETYPE", MessageType.SILENT_TYPE0.value)
        startActivity(intent)
    }

    fun buttonType0PingClicked(view: View?) {
        val intent = Intent(this, SendMessagelessSMSActivity::class.java)
        intent.putExtra("MESSAGETYPE", MessageType.SILENT_TYPE0_DELIVERY_REPORT.value)
        startActivity(intent)
    }

    fun buttonMWIPingClicked(view: View?) {
        val intent = Intent(this, SendMessagelessSMSActivity::class.java)
        intent.putExtra("MESSAGETYPE", MessageType.MWID_DELIVERY_REPORT.value)
        startActivity(intent)
    }

    fun buttonMWIActivateClicked(view: View?) {
        val intent = Intent(this, SendMessagelessSMSActivity::class.java)
        intent.putExtra("MESSAGETYPE", MessageType.MWIA.value)
        startActivity(intent)
    }

    fun buttonMWIDeactivateClicked(view: View?) {
        val intent = Intent(this, SendMessagelessSMSActivity::class.java)
        intent.putExtra("MESSAGETYPE", MessageType.MWID.value)
        startActivity(intent)
    }

    fun buttonServerModeClicked(view: View?) {
        val intent = Intent(this, ServerModeActivity::class.java)
        startActivity(intent)
    }

    fun buttonInfoScreenClicked(view: View?) {
        val intent = Intent(this, NetworkInfoActivity::class.java)
        startActivity(intent)
    }
}