package com.escalachurch.app.data

import com.escalachurch.app.di.AppContainer
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.ScaleItem
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Seeds a handful of example scales/doxologies so a first-time install isn't an empty app. */
object SeedData {

    suspend fun populateIfEmpty(container: AppContainer) {
        val existingScales = container.scaleRepository.observeAll().first()
        if (existingScales.isNotEmpty()) return

        val today = LocalDate.now()
        val nextWednesday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
        val nextSaturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val nextSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val scales = listOf(
            ScaleItem(
                date = nextWednesday,
                startTime = LocalTime.of(19, 30),
                title = "Culto de Oração",
                receptionPerson = "Maria Silva",
                soundPerson = "João Pereira",
                preachingPerson = "Pr. Carlos Souza",
                conductingPerson = "Ana Costa",
                musicalMessagePerson = "Coral da Igreja",
                notes = "Chegar 30 minutos antes para testes de som."
            ),
            ScaleItem(
                date = nextSaturday,
                startTime = LocalTime.of(19, 0),
                title = "Culto Jovem",
                receptionPerson = "Lucas Almeida",
                soundPerson = "Rafael Lima",
                preachingPerson = "Pr. Daniel Santos",
                conductingPerson = "Beatriz Rocha",
                musicalMessagePerson = "Grupo de Louvor Jovem",
                isSpecialEvent = false
            ),
            ScaleItem(
                date = nextSunday,
                startTime = LocalTime.of(9, 0),
                title = "Culto de Domingo - Manhã",
                receptionPerson = "Fernanda Dias",
                soundPerson = "Pedro Henrique",
                preachingPerson = "Pr. Carlos Souza",
                conductingPerson = "Marcos Vinícius",
                musicalMessagePerson = "Ministério de Louvor"
            )
        )
        scales.forEach { container.scaleRepository.save(it) }

        val doxology = DoxologyItem(
            date = nextSunday,
            startTime = LocalTime.of(9, 0),
            title = "Ordem do Culto - Domingo",
            programOrder = listOf(
                ProgramStep(order = 1, title = "Prelúdio", estimatedDurationMinutes = 5),
                ProgramStep(order = 2, title = "Boas-vindas", estimatedDurationMinutes = 5),
                ProgramStep(order = 3, title = "Hino inicial", estimatedDurationMinutes = 5),
                ProgramStep(order = 4, title = "Oração", estimatedDurationMinutes = 5),
                ProgramStep(order = 5, title = "Dízimos e ofertas", estimatedDurationMinutes = 10),
                ProgramStep(order = 6, title = "Mensagem musical", estimatedDurationMinutes = 10),
                ProgramStep(order = 7, title = "Sermão", estimatedDurationMinutes = 40),
                ProgramStep(order = 8, title = "Hino final", estimatedDurationMinutes = 5),
                ProgramStep(order = 9, title = "Oração final", estimatedDurationMinutes = 5),
                ProgramStep(order = 10, title = "Poslúdio", estimatedDurationMinutes = 5)
            )
        )
        container.doxologyRepository.save(doxology)

        val specialEvent = CustomEvent(
            title = "Santa Ceia",
            date = nextSunday,
            startTime = LocalTime.of(9, 0),
            description = "Celebração da Santa Ceia durante o culto de domingo.",
            eventType = ProgramType.SPECIAL_EVENT
        )
        container.customEventRepository.save(specialEvent)
    }
}
