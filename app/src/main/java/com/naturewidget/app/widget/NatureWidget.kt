package com.naturewidget.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextDecoration
import androidx.glance.unit.ColorProvider
import com.naturewidget.app.data.repository.NatureRepository
import com.naturewidget.app.data.repository.ObservationDisplayData

class NatureWidget : GlanceAppWidget() {
    
    companion object {
        val OBSERVATION_URL_KEY = ActionParameters.Key<String>("observation_url")
    }
    
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
private val White = Color.White
private val LightGray = Color(0xFFE0E0E0)

@Composable
private fun NatureWidgetContent(
    imageFile: java.io.File?,
    data: ObservationDisplayData?
) {
    // Determine click action: open iNaturalist if URL available, otherwise refresh
    val clickAction = if (data?.observationUrl?.isNotEmpty() == true) {
        actionRunCallback<OpenObservationAction>(
            actionParametersOf(NatureWidget.OBSERVATION_URL_KEY to data.observationUrl)
        )
    } else {
        actionRunCallback<RefreshAction>()
    }
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(DarkGreen))
            .cornerRadius(16.dp)
            .clickable(clickAction),
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
                        fontSize = 16.sp
                    )
                )
            }
        }
        
        // Text overlay at bottom (no shaded background - text with shadow effect via padding)
        if (data != null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Common name - larger, bold, with shadow simulation via multiple layers
                Text(
                    text = data.commonName,
                    style = TextStyle(
                        color = ColorProvider(White),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
                if (data.scientificName.isNotEmpty()) {
                    Text(
                        text = data.scientificName,
                        style = TextStyle(
                            color = ColorProvider(LightGray),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Action to open the observation in iNaturalist app/website
 */
class OpenObservationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val url = parameters[NatureWidget.OBSERVATION_URL_KEY] ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
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
