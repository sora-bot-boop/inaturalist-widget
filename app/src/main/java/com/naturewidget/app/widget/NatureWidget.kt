package com.naturewidget.app.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
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

// Color constants
private val DarkGreen = Color(0xFF1a472a)
private val SemiTransparentBlack = Color(0x99000000)
private val White = Color.White
private val LightGray = Color(0xFFCCCCCC)
private val MediumGray = Color(0xFFAAAAAA)

@Composable
private fun NatureWidgetContent(
    imageFile: java.io.File?,
    data: ObservationDisplayData?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(DarkGreen))
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
                    .background(ColorProvider(DarkGreen)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to load nature photos",
                    style = TextStyle(
                        color = ColorProvider(White),
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
                    .background(ColorProvider(SemiTransparentBlack))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = data.commonName,
                        style = TextStyle(
                            color = ColorProvider(White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    if (data.scientificName.isNotEmpty()) {
                        Text(
                            text = data.scientificName,
                            style = TextStyle(
                                color = ColorProvider(LightGray),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                    if (data.location.isNotEmpty()) {
                        Text(
                            text = "📍 ${data.location}",
                            style = TextStyle(
                                color = ColorProvider(MediumGray),
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
