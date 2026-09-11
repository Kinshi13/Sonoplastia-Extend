package com.escalachurch.app.ui.screens.home

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.WorshipSong
import java.time.LocalDate

/** Fase 11.11 (Home 2.0) - pure content-shaping logic, deliberately kept free of Compose/Android
 *  so it's directly unit-testable (see HomeContentModelsTest) instead of only provable by running
 *  the real app. Nothing here talks to a repository - HomeViewModel already fetched everything;
 *  this only decides what to *show* from what's already in hand. */

/** One function+person line the Hero preview shows, in a fixed, always-the-same order (Sonoplastia,
 *  Regência, Pregação, Recepção, Mensagem Musical) - never the arbitrary order the 5 legacy columns
 *  happen to sit in on [ScaleItem], so the Hero never reshuffles from one scale to the next. */
data class HeroRoleLine(val roleLabel: String, val personName: String)

data class HeroSummary(
    val scale: ScaleItem,
    val visibleRoles: List<HeroRoleLine>,
    val hiddenRoleCount: Int,
    val specialLabel: String?
)

private const val MAX_VISIBLE_ROLES = 3

/** Bloco 5/6 - builds the Hero card's content from the next official [scale], never more than
 *  [MAX_VISIBLE_ROLES] role lines - the rest collapse into "+ N funções" ([HeroSummary.hiddenRoleCount]).
 *  Bloco 6: never surfaces `is_temporary` as a raw column name - [specialLabelFor] does the
 *  translation, and this function is the only caller. */
fun heroSummaryFor(scale: ScaleItem): HeroSummary {
    val allRoles = listOf(
        HeroRoleLine("Sonoplastia", scale.soundPerson),
        HeroRoleLine("Regência", scale.conductingPerson),
        HeroRoleLine("Pregação", scale.preachingPerson),
        HeroRoleLine("Recepção", scale.receptionPerson),
        HeroRoleLine("Mensagem Musical", scale.musicalMessagePerson)
    ).filter { it.personName.isNotBlank() }

    return HeroSummary(
        scale = scale,
        visibleRoles = allRoles.take(MAX_VISIBLE_ROLES),
        hiddenRoleCount = (allRoles.size - MAX_VISIBLE_ROLES).coerceAtLeast(0),
        specialLabel = specialLabelFor(scale)
    )
}

/** Bloco 6 - "Programação especial"/"Programação extraordinária" instead of the raw
 *  `is_special_event`/`is_temporary` flags. A scale can be both; extraordinária (temporária, feita
 *  às pressas) is the more specific/urgent read, so it wins when both are true. Null means neither
 *  flag is set - the Hero shows no badge at all. */
fun specialLabelFor(scale: ScaleItem): String? = when {
    scale.isTemporary -> "Programação extraordinária"
    scale.isSpecialEvent -> "Programação especial"
    else -> null
}

/** Bloco 8 - one card in "Agora na Igreja". [ranked] below returns at most [MAX_AGORA_ITEMS], in
 *  the fixed conceptual priority the spec lays out (urgent > relevant schedule info > pinned
 *  announcement > special schedule > daily music > recent retrospective). Steps 1
 *  ("conteúdo urgente") and 6 ("retrospectiva recente") have no supporting field/feature yet - not
 *  implemented, per "não inventar campos que não existem" / "não construir Retrospectiva agora". */
sealed class AgoraNaIgrejaItem {
    data class PinnedAnnouncement(val announcement: Announcement) : AgoraNaIgrejaItem()
    data class SpecialSchedule(val scale: ScaleItem, val label: String) : AgoraNaIgrejaItem()
    data class DailyMusic(val song: WorshipSong) : AgoraNaIgrejaItem()
}

private const val MAX_AGORA_ITEMS = 3

/** Deterministic - same inputs always produce the same, same-order output (Bloco 8: "ranking
 *  determinístico simples", not a recommendation engine). [today] is passed in rather than read
 *  from [LocalDate.now] so this stays a pure function under test. */
fun rankAgoraNaIgreja(
    pinnedAnnouncement: Announcement?,
    nextScale: ScaleItem?,
    todaysRecommendation: WorshipSong?
): List<AgoraNaIgrejaItem> = buildList {
    pinnedAnnouncement?.let { add(AgoraNaIgrejaItem.PinnedAnnouncement(it)) }
    nextScale?.let { scale -> specialLabelFor(scale)?.let { label -> add(AgoraNaIgrejaItem.SpecialSchedule(scale, label)) } }
    todaysRecommendation?.let { add(AgoraNaIgrejaItem.DailyMusic(it)) }
}.take(MAX_AGORA_ITEMS)

/** Bloco 10 - the exact three-field check the spec calls out, reused instead of re-deriving it at
 *  the call site (mirrors [WorshipSong.isTodaysRecommendation], kept here too since Home needs it
 *  as a plain predicate over a nullable list, not a method call per item). */
fun todaysRecommendationFrom(songs: List<WorshipSong>, today: LocalDate): WorshipSong? =
    songs.firstOrNull { it.isTodaysRecommendation(today) }
