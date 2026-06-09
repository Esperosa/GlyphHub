package com.pelikan.glyphhub.systemstatus

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pelikan.glyphhub.glyph.GlyphHubToyService

class GlyphNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageLabel = sbn.packageName.substringAfterLast('.').take(3).uppercase()
        GlyphHubToyService.requestStatusEvent(
            this,
            GlyphStatusEvent(
                type = GlyphStatusEventType.Notification,
                label = packageLabel.ifBlank { "NOT" }
            )
        )
    }
}
