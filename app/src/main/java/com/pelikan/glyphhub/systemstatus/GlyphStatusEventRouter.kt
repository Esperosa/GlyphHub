package com.pelikan.glyphhub.systemstatus

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.toys.drawCenteredText3x5
import com.pelikan.glyphhub.toys.frameFromPoints

class GlyphStatusEventRouter {
    private var active: ActiveStatus? = null

    fun submit(event: GlyphStatusEvent, nowMs: Long = android.os.SystemClock.elapsedRealtime()): Boolean {
        val current = active
        if (current != null && current.expiresAtMs > nowMs && current.event.type.priority > event.type.priority) {
            return false
        }
        active = ActiveStatus(event, nowMs + event.ttlMs)
        return true
    }

    fun activeFrame(brightness: Int, nowMs: Long = android.os.SystemClock.elapsedRealtime()): GlyphFrame? {
        val current = active ?: return null
        if (current.expiresAtMs <= nowMs) {
            active = null
            return null
        }
        return render(current.event, brightness)
    }

    private fun render(event: GlyphStatusEvent, brightness: Int): GlyphFrame =
        when (event.type) {
            GlyphStatusEventType.ChargingConnected -> chargeFrame(brightness, "IN")
            GlyphStatusEventType.ChargingDisconnected -> chargeFrame(brightness, "OUT")
            GlyphStatusEventType.ChargingFull -> ringText("FUL", brightness)
            GlyphStatusEventType.Volume -> volumeFrame(event.value, brightness)
            GlyphStatusEventType.Notification -> ringText(event.label.take(3).ifBlank { "NOT" }, brightness)
            GlyphStatusEventType.NetworkConnected -> ringText("NET", brightness)
            GlyphStatusEventType.NetworkDisconnected -> ringText("OFF", brightness)
            GlyphStatusEventType.PaymentManual -> ringText("PAY", brightness)
            GlyphStatusEventType.PaymentSuccessManual,
            GlyphStatusEventType.NfcEvent -> checkFrame(brightness)
            GlyphStatusEventType.Beacon -> beaconFrame(brightness)
            GlyphStatusEventType.Wake -> ringText("UP", brightness)
        }

    private fun ringText(text: String, brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 58, thickness = 0.55)
        frame = frame.overlay(drawCenteredText3x5(text, brightness, yOffset = 4))
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun chargeFrame(brightness: Int, label: String): GlyphFrame =
        ringText(label, brightness).overlay(
            frameFromPoints(setOf(6 to 1, 5 to 3, 6 to 3, 5 to 5, 7 to 5, 6 to 7, 7 to 7, 6 to 9), 100)
        )

    private fun volumeFrame(value: Int, brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 35, thickness = 0.5)
        frame = GlyphDesignSystem.drawProgressRing(frame, (value.coerceIn(0, 100) / 100f), 100)
        frame = frame.overlay(drawCenteredText3x5("VOL", brightness, yOffset = 4))
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun checkFrame(brightness: Int): GlyphFrame =
        frameFromPoints(
            setOf(2 to 7, 3 to 8, 4 to 9, 5 to 8, 6 to 7, 7 to 6, 8 to 5, 9 to 4, 10 to 3),
            brightness
        )

    private fun beaconFrame(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), 1.4, 100, thickness = 0.7)
        frame = GlyphDesignSystem.drawCircleApprox(frame, 3.4, 78, thickness = 0.55)
        frame = GlyphDesignSystem.drawCircleApprox(frame, GlyphDesignSystem.fullRingRadius, 58, thickness = 0.5)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private data class ActiveStatus(
        val event: GlyphStatusEvent,
        val expiresAtMs: Long
    )
}
