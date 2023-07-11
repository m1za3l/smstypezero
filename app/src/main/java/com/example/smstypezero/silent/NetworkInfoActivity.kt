package com.example.smstypezero.silent

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.smstypezero.R
import java.util.Locale

class NetworkInfoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_info)
        val manager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (manager == null) {
            Toast.makeText(
                applicationContext, "TelephonyManager not found",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        (findViewById<View>(R.id.textViewPhoneNr) as TextView).text =
            if (manager.line1Number != null && !manager.line1Number.isEmpty()) manager.line1Number else "Unknown"
        (findViewById<View>(R.id.textViewImsi) as TextView).text =
            if (manager.subscriberId != null && !manager.subscriberId.isEmpty()) manager.subscriberId else "Unknown"
        (findViewById<View>(R.id.textViewImei) as TextView).text =
            if (manager.deviceId != null && !manager.deviceId.isEmpty()) manager.deviceId else "Unknown"
        (findViewById<View>(R.id.textViewSimSerial) as TextView).text =
            if (manager.simSerialNumber != null && !manager.simSerialNumber.isEmpty()) manager.simSerialNumber else "Unknown"
        var mccmnc = manager.networkOperator
        mccmnc = if (mccmnc != null && mccmnc.length > 2) {
            mccmnc.substring(0, 3) + " / " + mccmnc.substring(3)
        } else {
            "Unknown"
        }
        (findViewById<View>(R.id.textViewMccMnc) as TextView).text = mccmnc
        (findViewById<View>(R.id.textViewNetworkType) as TextView).text =
            getNetworkType(manager.networkType)
        (findViewById<View>(R.id.textViewNetworkOperatorName) as TextView).text =
            if (manager.networkOperatorName != null && !manager.networkOperatorName.isEmpty()) manager.networkOperatorName else "Unknown"
        (findViewById<View>(R.id.textViewNetworkCountry) as TextView).text =
            if (manager.networkCountryIso != null && !manager.networkCountryIso.isEmpty()) manager.networkCountryIso.uppercase(
                Locale.getDefault()
            ) else "Unknown"

        // SIM
        (findViewById<View>(R.id.textViewSimOperatorName) as TextView).text =
            if (manager.simOperatorName != null && !manager.simOperatorName.isEmpty()) manager.simOperatorName else "Unknown"
        (findViewById<View>(R.id.textViewSimCountry) as TextView).text =
            if (manager.simCountryIso != null && !manager.simCountryIso.isEmpty()) manager.simCountryIso.uppercase(
                Locale.getDefault()
            ) else "Unknown"

        //starts with api 17
//        if (Build.VERSION.SDK_INT >= 17) {
//
//            List<CellInfo> infos = manager.getAllCellInfo();
//            if (infos != null) {
//                for (CellInfo info : infos) {
//                    text += "info: " + info.toString() + "\r\n";
//                }
//            }
//
//            List<NeighboringCellInfo> neighboringCellInfos = manager.getNeighboringCellInfo();
//            if (neighboringCellInfos != null) {
//                for (NeighboringCellInfo info : neighboringCellInfos) {
//                    text += "info: " + info.getNetworkType()
//                            + " : " + info.getCid()
//                            + " : " + info.getLac()
//                            + " : " + info.getRssi()
//
//
//                            + "\r\n";
//                }
//            }
//        }
    }

    private fun getNetworkType(type: Int): String {
        return when (type) {
            0 -> "Unknown"
            1 -> "GPRS"
            2 -> "EDGE"
            3 -> "UMTS"
            4 -> "CDMA"
            5 -> "EVDO_0"
            6 -> "EVDO_A"
            7 -> "1xRTT"
            8 -> "HSDPA"
            9 -> "HSUPA"
            10 -> "HSPA"
            11 -> "IDEN"
            12 -> "EVDO_B"
            13 -> "LTE"
            14 -> "EHRPD"
            15 -> "HSPAP"
            else -> "Unknown"
        }
    }
}