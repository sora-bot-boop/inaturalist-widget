package com.naturewidget.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NatureWidgetReceiver : GlanceAppWidgetReceiver() {
    
    override val glanceAppWidget: GlanceAppWidget = NatureWidget()
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start periodic updates when widget is first added
        NatureWidgetWorker.enqueuePeriodic(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel updates when all widgets are removed
        NatureWidgetWorker.cancel(context)
    }
}
