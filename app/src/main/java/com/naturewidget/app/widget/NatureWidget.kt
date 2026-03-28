package com.naturewidget.app.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.naturewidget.app.data.repository.NatureRepository
import com.naturewidget.app.data.repository.ObservationDisplayData

class NatureWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = NatureRepository(context)
        val imageFile = repository.getCachedImageFile()
        val data = repository.getCachedObservationData()
        
        provideContent {
            NatureWidgetContent(
                imageFile = imageFile,
                data = data
            )
        }
    }
}

@Composable
private fun NatureWidgetContent(
    imageFile: java.io.File?,
    data: ObservationDisplayData?
) {
    val backgroundColor = ColorProvider(
        day = androidx.glance.R.color.glance_colorBackground,
        night = androidx.glance.R.color.glance_colorBackground
    )
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(android.R.drawable.screen_background_dark))
            .cornerRadius(16.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.BottomStart
    ) {
        // Background image
        if (imageFile != null && imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = data?.commonName ?: "Nature observation",
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            // Placeholder when no image
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(android.graphics.Color.parseColor("#1a472a"))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to load nature photos",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE),
                        fontSize = 14.sp
                    )
                )
            }
        }
        
        // Info overlay at bottom
        if (data != null) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(android.graphics.Color.parseColor("#99000000")))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = data.commonName,
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.WHITE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    if (data.scientificName.isNotEmpty()) {
                        Text(
                            text = data.scientificName,
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.parseColor("#CCCCCC")),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                    if (data.location.isNotEmpty()) {
                        Text(
                            text = "📍 ${data.location}",
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.parseColor("#AAAAAA")),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action to refresh the widget with a new image
 */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Trigger a refresh via WorkManager
        NatureWidgetWorker.enqueueOneTime(context)
    }
}
