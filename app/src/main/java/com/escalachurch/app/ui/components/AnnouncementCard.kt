package com.escalachurch.app.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Feed card for the Anúncios screen. Images keep a fixed 4:3 ratio with rounded corners. */
@Composable
fun AnnouncementCard(
    announcement: Announcement,
    isNew: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onOpenCalendar: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 5.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (announcement.mediaType != MediaType.NONE && !announcement.mediaUrl.isNullOrBlank()) {
                MediaPreview(announcement)
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (announcement.isPinned) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(announcement.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (isNew) NewBadge()
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    formatPublished(announcement.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (announcement.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(announcement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                if (announcement.affectedClasses.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        announcement.affectedClasses.forEach { userClass ->
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (highlighted) 1f else 0.5f)) {
                                Text(
                                    userClass.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (announcement.relatedEventDate != null && onOpenCalendar != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.clickable { onOpenCalendar() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ver no calendário · ${announcement.relatedEventDate.toDisplayString()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** Images always render at a fixed 4:3 ratio; videos get their own player so the layout never breaks. */
@Composable
private fun MediaPreview(announcement: Announcement) {
    val mediaUrl = announcement.mediaUrl ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        when (announcement.mediaType) {
            MediaType.IMAGE -> AsyncImage(
                model = mediaUrl,
                contentDescription = announcement.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            MediaType.VIDEO -> androidx.compose.runtime.key(mediaUrl) { VideoPreview(mediaUrl) }
            MediaType.NONE -> Unit
        }
    }
}

/**
 * Minimal video player for the 4:3 media slot: never autoplays (and never with sound until the
 * viewer explicitly taps play), shows the first frame as a still, and offers a simple play/pause
 * toggle - matching "controles básicos de play/pause" and "evitar autoplay com som".
 */
@Composable
private fun VideoPreview(uri: String) {
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var videoView by remember(uri) { mutableStateOf<VideoView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(Uri.parse(uri))
                    setOnPreparedListener { player ->
                        player.isLooping = false
                        // Show the first frame as a still without starting playback (no autoplay).
                        seekTo(1)
                    }
                    setOnCompletionListener { isPlaying = false }
                    videoView = this
                }
            },
            onRelease = { it.stopPlayback() },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                .clickable {
                    val view = videoView ?: return@clickable
                    if (isPlaying) view.pause() else view.start()
                    isPlaying = !isPlaying
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar vídeo" else "Reproduzir vídeo",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@Composable
private fun NewBadge() {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.error) {
        Text(
            "Novo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun formatPublished(epochMillis: Long): String =
    SimpleDateFormat("dd 'de' MMMM 'às' HH:mm", Locale("pt", "BR")).format(Date(epochMillis))
