package com.pelikan.glyphhub.systemstatus

enum class GlyphStatusEventType(val id: String, val priority: Int, val defaultTtlMs: Long) {
    ChargingConnected("charging_connected", 40, 1_800L),
    ChargingDisconnected("charging_disconnected", 40, 1_400L),
    ChargingFull("charging_full", 70, 2_500L),
    Volume("volume", 25, 1_000L),
    Notification("notification", 20, 1_300L),
    NetworkConnected("network_connected", 20, 1_200L),
    NetworkDisconnected("network_disconnected", 25, 1_200L),
    PaymentManual("payment_manual", 80, 2_000L),
    PaymentSuccessManual("payment_success_manual", 90, 2_000L),
    NfcEvent("nfc_event", 85, 2_000L),
    Beacon("beacon", 100, 5_000L),
    Wake("wake", 80, 3_000L);

    companion object {
        fun fromId(id: String): GlyphStatusEventType? = entries.firstOrNull { it.id == id }
    }
}

data class GlyphStatusEvent(
    val type: GlyphStatusEventType,
    val value: Int = 0,
    val label: String = "",
    val ttlMs: Long = type.defaultTtlMs
)
