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

enum class MessageType(val value: Int) {
    SMS(0), SMS_DELIVERY_REPORT(1), CLASS0(2), CLASS0_DELIVERY_REPORT(3), SILENT_TYPE0(4), SILENT_TYPE0_DELIVERY_REPORT(
        5
    ),
    MWIA(6), MWID(7), MWID_DELIVERY_REPORT(8);

    fun hasDeliveryReport(): Boolean {
        when (this) {
            SMS_DELIVERY_REPORT, CLASS0_DELIVERY_REPORT, SILENT_TYPE0_DELIVERY_REPORT, MWID_DELIVERY_REPORT -> return true
            else -> {}
        }
        return false
    }

    fun hasText(): Boolean {
        when (this) {
            SMS, SMS_DELIVERY_REPORT, CLASS0, CLASS0_DELIVERY_REPORT -> return true
            else -> {}
        }
        return false
    }
}