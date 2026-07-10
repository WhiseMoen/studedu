package com.sapraliev.studedu.domain.occurrence

import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.ExceptionType
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceFreq
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Поведенческие тесты генератора вхождений.
 * Эталон семантики повторений — поведение Google Calendar.
 * Зона фиксирована (Europe/Moscow), чтобы тесты были детерминированы.
 */
class OccurrenceGeneratorTest {

    private val zone = TimeZone.of("Europe/Moscow")
    private val generator = OccurrenceGenerator(zone)
    private val now = Instant.parse("2026-07-01T00:00:00Z")

    // 6 июля 2026 — понедельник.
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone)

    private fun event(
        id: String = "e1",
        start: Instant,
        end: Instant,
        ruleId: String? = null,
    ) = EventEntity(
        id = id, userId = "u", title = "Событие", comment = null,
        type = EventType.PERSONAL, startAt = start, endAt = end,
        isAllDay = false, studentId = null, recurrenceRuleId = ruleId,
        color = null, source = null, createdAt = now, updatedAt = now,
    )

    private fun rule(
        id: String = "r1",
        freq: RecurrenceFreq,
        interval: Int = 1,
        byweekday: List<String>? = null,
        count: Int? = null,
        until: LocalDate? = null,
    ) = RecurrenceRuleEntity(
        id = id, userId = "u", freq = freq, interval = interval,
        byweekday = byweekday, count = count, until = until,
        createdAt = now, updatedAt = now,
    )

    private fun exception(
        eventId: String = "e1",
        original: Instant,
        type: ExceptionType,
        newStart: Instant? = null,
        newEnd: Instant? = null,
    ) = RecurrenceExceptionEntity(
        id = "x-$original", userId = "u", eventId = eventId,
        originalDate = original, type = type,
        newStartAt = newStart, newEndAt = newEnd, createdAt = now,
    )

    // ---------- одиночные события ----------

    @Test
    fun `одиночное событие внутри окна попадает в результат`() {
        val e = event(start = at(2026, 7, 7, 10), end = at(2026, 7, 7, 11))
        val result = generator.generate(
            listOf(e), emptyMap(), emptyList(),
            from = at(2026, 7, 6, 0), to = at(2026, 7, 13, 0),
        )
        assertEquals(1, result.size)
        assertEquals(at(2026, 7, 7, 10), result[0].start)
        assertEquals(null, result[0].originalStart)
    }

    @Test
    fun `одиночное событие вне окна не попадает`() {
        val e = event(start = at(2026, 7, 20, 10), end = at(2026, 7, 20, 11))
        val result = generator.generate(
            listOf(e), emptyMap(), emptyList(),
            from = at(2026, 7, 6, 0), to = at(2026, 7, 13, 0),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `событие через полночь на границе окна включается`() {
        // 6 июля 23:00 – 7 июля 01:00, окно начинается 7 июля 00:00
        val e = event(start = at(2026, 7, 6, 23), end = at(2026, 7, 7, 1))
        val result = generator.generate(
            listOf(e), emptyMap(), emptyList(),
            from = at(2026, 7, 7, 0), to = at(2026, 7, 13, 0),
        )
        assertEquals(1, result.size)
    }

    // ---------- серии ----------

    @Test
    fun `ежедневная серия с count генерирует ровно count вхождений`() {
        val r = rule(freq = RecurrenceFreq.DAILY, count = 5)
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 1, 0), to = at(2026, 8, 1, 0),
        )
        assertEquals(5, result.size)
        assertEquals(at(2026, 7, 6, 10), result[0].start)
        assertEquals(at(2026, 7, 10, 10), result[4].start)
        // Длительность вхождения равна длительности шаблона.
        assertEquals(at(2026, 7, 10, 11), result[4].end)
    }

    @Test
    fun `каждые две недели по пн и ср`() {
        val r = rule(freq = RecurrenceFreq.WEEKLY, interval = 2, byweekday = listOf("MO", "WE"))
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 6, 0), to = at(2026, 7, 26, 0),
        )
        val starts = result.map { it.start }
        assertEquals(
            listOf(
                at(2026, 7, 6, 10),  // пн
                at(2026, 7, 8, 10),  // ср
                at(2026, 7, 20, 10), // пн через неделю
                at(2026, 7, 22, 10), // ср через неделю
            ),
            starts,
        )
    }

    @Test
    fun `until включает последний день`() {
        val r = rule(freq = RecurrenceFreq.DAILY, until = LocalDate(2026, 7, 8))
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 1, 0), to = at(2026, 8, 1, 0),
        )
        assertEquals(3, result.size) // 6, 7 и 8 июля
        assertEquals(at(2026, 7, 8, 10), result.last().start)
    }

    @Test
    fun `окно обрезает бесконечную серию`() {
        val r = rule(freq = RecurrenceFreq.DAILY)
        val e = event(start = at(2026, 7, 1, 10), end = at(2026, 7, 1, 11), ruleId = "r1")
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 6, 0), to = at(2026, 7, 9, 0),
        )
        assertEquals(3, result.size) // 6, 7, 8 июля
    }

    @Test
    fun `старая ежедневная серия доживает до текущего окна`() {
        // Серия стартовала 1 января — окно в июле должно получить свои вхождения.
        val r = rule(freq = RecurrenceFreq.DAILY)
        val e = event(start = at(2026, 1, 1, 10), end = at(2026, 1, 1, 11), ruleId = "r1")
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 6, 0), to = at(2026, 7, 13, 0),
        )
        assertEquals(7, result.size)
    }

    // ---------- исключения ----------

    @Test
    fun `отменённое вхождение выпадает из серии`() {
        val r = rule(freq = RecurrenceFreq.DAILY, count = 3)
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val ex = exception(original = at(2026, 7, 7, 10), type = ExceptionType.CANCELLED)
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), listOf(ex),
            from = at(2026, 7, 1, 0), to = at(2026, 8, 1, 0),
        )
        assertEquals(listOf(at(2026, 7, 6, 10), at(2026, 7, 8, 10)), result.map { it.start })
    }

    @Test
    fun `перенос внутри окна заменяет вхождение`() {
        val r = rule(freq = RecurrenceFreq.DAILY, count = 3)
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val ex = exception(
            original = at(2026, 7, 7, 10), type = ExceptionType.MOVED,
            newStart = at(2026, 7, 7, 15), newEnd = at(2026, 7, 7, 16),
        )
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), listOf(ex),
            from = at(2026, 7, 1, 0), to = at(2026, 8, 1, 0),
        )
        assertEquals(3, result.size)
        val moved = result.first { it.isMoved }
        assertEquals(at(2026, 7, 7, 15), moved.start)
        assertEquals(at(2026, 7, 7, 10), moved.originalStart)
        assertTrue(result.none { it.start == at(2026, 7, 7, 10) })
    }

    @Test
    fun `перенос В окно из-за его пределов подхватывается`() {
        // Понедельничная серия; вхождение 6 июля перенесено на 9 июля.
        // Окно 8–12 июля: original вне окна, new внутри.
        val r = rule(freq = RecurrenceFreq.WEEKLY, byweekday = listOf("MO"))
        val e = event(start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val ex = exception(
            original = at(2026, 7, 6, 10), type = ExceptionType.MOVED,
            newStart = at(2026, 7, 9, 12), newEnd = at(2026, 7, 9, 13),
        )
        val result = generator.generate(
            listOf(e), mapOf("r1" to r), listOf(ex),
            from = at(2026, 7, 8, 0), to = at(2026, 7, 12, 0),
        )
        assertEquals(1, result.size)
        assertTrue(result[0].isMoved)
        assertEquals(at(2026, 7, 9, 12), result[0].start)
    }

    @Test
    fun `результат отсортирован по началу`() {
        val r = rule(freq = RecurrenceFreq.DAILY, count = 3)
        val series = event(id = "e1", start = at(2026, 7, 6, 10), end = at(2026, 7, 6, 11), ruleId = "r1")
        val single = event(id = "e2", start = at(2026, 7, 7, 8), end = at(2026, 7, 7, 9))
        val result = generator.generate(
            listOf(series, single), mapOf("r1" to r), emptyList(),
            from = at(2026, 7, 1, 0), to = at(2026, 8, 1, 0),
        )
        assertEquals(result.map { it.start }, result.map { it.start }.sorted())
        assertEquals(4, result.size)
    }
}
