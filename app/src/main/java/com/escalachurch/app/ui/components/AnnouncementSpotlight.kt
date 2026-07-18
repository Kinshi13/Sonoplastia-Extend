package com.escalachurch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fase 11.9B Entrega 3 Bloco 4 - pop-up shown when there are new/changed announcements the viewer
 * hasn't confirmed yet (see AnnouncementSpotlightViewModel for eligibility). At most 3 per opening,
 * one at a time, with an explicit "N de total" indicator.
 *
 * Media here is a static thumbnail only (no inline video playback) - keeps the pop-up light and
 * avoids two competing "which video is allowed to play" coordinators (the feed's own, see
 * AnnouncementCard); tapping "Ver anúncio" is expected to navigate to the real feed item.
 *
 * Fase 11.9B Bloco 23 - a VIDEO announcement has no separate thumbnail field (only [mediaUrl]
 * pointing at the video itself), and Coil isn't set up to extract video frames here (that's the
 * coil-video artifact, not a dependency of this module) - generating a real preview would mean
 * adding a library for one card type. A simple play-icon placeholder instead, so a video
 * announcement doesn't just render as a bare text card with no visual cue at all.
 */
@Composable
fun AnnouncementSpotlight(
    announcement: Announcement,
    index: Int,
    total: Int,
    onView: () -> Unit,
    onSnooze: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onSnooze) {
        CelestialFrame(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column {
                if (!announcement.mediaUrl.isNullOrBlank() && announcement.mediaType == MediaType.IMAGE) {
                    AsyncImage(
                        model = announcement.mediaUrl,
                        contentDescription = announcement.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                } else if (!announcement.mediaUrl.isNullOrBlank() && announcement.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayCircle,
                            contentDescription = "Este anúncio tem vídeo - toque em Ver anúncio para assistir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SpotlightBadge(announcement)
                        Text(
                            "${index + 1} de $total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        announcement.title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(formatDate(announcement.publishedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (announcement.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            announcement.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onSnooze) { Text("Agora não") }
                        Row {
                            TextButton(onClick = onConfirm) { Text("Marcar como visto") }
                            PrimaryButton(text = "Ver anúncio", onClick = onView)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightBadge(announcement: Announcement) {
    val isChanged = announcement.updatedAt > announcement.publishedAt
    val label = if (isChanged) "Alterado" else "Novo"
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.error) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")).format(Date(epochMillis))
