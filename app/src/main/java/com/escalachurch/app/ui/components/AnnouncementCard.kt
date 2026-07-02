package com.escalachurch.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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

@Composable
private fun MediaPreview(announcement: Announcement) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        AsyncImage(
            model = announcement.mediaUrl,
            contentDescription = announcement.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
        )
        if (announcement.mediaType == MediaType.VIDEO) {
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Reproduzir vídeo",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(56.dp)
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
