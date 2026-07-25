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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.runtime.LaunchedEffect
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

/**
 * Fase 11.9B Entrega 3 Bloco 3 - CelestialAnnouncementCard. Feed card for the Anúncios screen.
 * Images/video keep a fixed 4:3 ratio with rounded corners; the card itself is now a CelestialFrame.
 *
 * [activePlayingId]/[onRequestPlay] coordinate playback across the whole feed so at most one video
 * plays at a time (Bloco 3: "não tocar vários simultaneamente") - see AnnouncementsScreen, which
 * hoists a single "which announcement is playing" state above the list. Passing null for both
 * (the default) keeps this card's video fully self-contained, same as before this phase.
 *
 * Note (honestly scoped): the `announcements` table has no priority/urgency column today (checked
 * schema.sql and web/lib/types/database.ts - neither has one), so NORMAL/IMPORTANT/URGENT badges
 * requested for this block are not rendered here - there is no real data to back them, and this
 * phase's rules forbid fabricating fields to demonstrate a screen. Only isNew/isPinned (already
 * real, existing fields) are shown.
 *
 * Usabilidade (edição de anúncios): the three-dot admin menu ([onEdit]/[onTogglePublish]/
 * [onDeleteRequest]) only ever renders when a caller passes at least one of them - a member's feed
 * (AnnouncementsScreen only wires these when `state.isAdmin`) never sees it, so there's no client-
 * side-only gate to trust: a member simply never receives the callbacks that would show the menu.
 * [onShare] stays a separate, always-visible icon (public members can share too), not folded into
 * this menu. When `isAdmin` is showing an unpublished announcement (see
 * AnnouncementRepository.observeAllForAdmin), a small "Rascunho" badge marks it.
 */
@Composable
fun AnnouncementCard(
    announcement: Announcement,
    isNew: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onOpenCalendar: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onTogglePublish: (() -> Unit)? = null,
    onDeleteRequest: (() -> Unit)? = null,
    activePlayingId: String? = null,
    onRequestPlay: ((String) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CelestialFrame(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it },
        cornerRadius = 24.dp
    ) {
        Column {
            val hasMedia = !announcement.mediaUrl.isNullOrBlank()
            if (hasMedia && (announcement.mediaType == MediaType.IMAGE || announcement.mediaType == MediaType.VIDEO)) {
                MediaPreview(
                    announcement = announcement,
                    isActivePlayer = activePlayingId == announcement.id,
                    onRequestPlay = { onRequestPlay?.invoke(announcement.id) }
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (announcement.isPinned) {
                            Icon(Icons.Filled.PushPin, contentDescription = "Fixado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(announcement.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (!announcement.isActive) {
                            Spacer(Modifier.width(6.dp))
                            DraftBadge()
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isNew) NewBadge()
                        // Public share icon - stays outside the admin menu since any visitor can
                        // use it, not just an Admin.
                        if (onShare != null) {
                            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Compartilhar anúncio",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                        }
                        if (onEdit != null || onTogglePublish != null || onDeleteRequest != null) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "Mais ações do anúncio",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.height(18.dp)
                                    )
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    if (onEdit != null) {
                                        DropdownMenuItem(
                                            text = { Text("Editar") },
                                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                            onClick = { menuExpanded = false; onEdit() }
                                        )
                                    }
                                    if (onTogglePublish != null) {
                                        DropdownMenuItem(
                                            text = { Text(if (announcement.isActive) "Despublicar" else "Publicar") },
                                            leadingIcon = {
                                                Icon(
                                                    if (announcement.isActive) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = { menuExpanded = false; onTogglePublish() }
                                        )
                                    }
                                    if (onDeleteRequest != null) {
                                        DropdownMenuItem(
                                            text = { Text("Excluir") },
                                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                            onClick = { menuExpanded = false; onDeleteRequest() }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
private fun MediaPreview(announcement: Announcement, isActivePlayer: Boolean, onRequestPlay: () -> Unit) {
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
            MediaType.VIDEO -> androidx.compose.runtime.key(mediaUrl) {
                VideoPreview(uri = mediaUrl, isActivePlayer = isActivePlayer, onRequestPlay = onRequestPlay)
            }
            else -> Unit
        }
    }
}

/**
 * Minimal video player for the 4:3 media slot: never autoplays (and never with sound until the
 * viewer explicitly taps play), shows the first frame as a still, offers a simple play/pause
 * toggle, and releases the player when the composable leaves composition (list scroll/recycle) -
 * matching "controles básicos de play/pause", "evitar autoplay com som" and "liberar recursos".
 *
 * [isActivePlayer]/[onRequestPlay] let a parent (AnnouncementsScreen) enforce "only one video
 * plays at a time" across the feed: tapping play here asks the parent to make this card the active
 * one; if some *other* card becomes active, this one's LaunchedEffect below pauses itself.
 */
@Composable
private fun VideoPreview(uri: String, isActivePlayer: Boolean, onRequestPlay: () -> Unit) {
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var videoView by remember(uri) { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(isActivePlayer) {
        if (!isActivePlayer && isPlaying) {
            videoView?.pause()
            isPlaying = false
        }
    }

    // Fase 11.9B Bloco 19 - VideoView has no built-in "pause when the app backgrounds" behavior
    // (unlike Media3's own lifecycle awareness) - without this, a playing video keeps its audio
    // track running after the user leaves the app.
    val lifecycleState by androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleState) {
        if (!lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) && isPlaying) {
            videoView?.pause()
            isPlaying = false
        }
    }

    DisposableEffect(uri) {
        onDispose { videoView?.stopPlayback() }
    }

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
                    if (isPlaying) {
                        view.pause()
                        isPlaying = false
                    } else {
                        onRequestPlay()
                        view.start()
                        isPlaying = true
                    }
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

/** Usabilidade (edição de anúncios) - only ever rendered for an Admin viewing an unpublished
 *  announcement (see AnnouncementRepository.observeAllForAdmin); a member's feed never queries
 *  unpublished rows at all, so this badge simply can't appear there. */
@Composable
private fun DraftBadge() {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            "Rascunho",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun formatPublished(epochMillis: Long): String =
    SimpleDateFormat("dd 'de' MMMM 'às' HH:mm", Locale("pt", "BR")).format(Date(epochMillis))
