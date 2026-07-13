package com.sapraliev.studedu.domain.conflict

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectorTest {

    private data class Slot(val name: String, val startAt: Instant, val endAt: Instant)

    private val detector = ConflictDetector()

    private fun at(hour: Int, minute: Int = 0): Instant =
        Instant.parse("2026-07-06T%02d:%02d:00Z".format(hour, minute))

    private fun slot(name: String, from: Int, to: Int, fromMin: Int = 0, toMin: Int = 0) =
        Slot(name, at(from, fromMin), at(to, toMin))

    private fun run(vararg slots: Slot): Map<Slot, List<Slot>> =
        detector.findConflicts(slots.toList(), Slot::startAt, Slot::endAt)

    @Test
    fun `пересечение отмечает оба элемента`() {
        val a = slot("a", 10, 12)
        val b = slot("b", 11, 13)
        val result = run(a, b)
        assertEquals(listOf(b), result[a])
        assertEquals(listOf(a), result[b])
    }

    @Test
    fun `касание границ — не конфликт`() {
        val a = slot("a", 10, 11)
        val b = slot("b", 11, 12)
        assertTrue(run(a, b).isEmpty())
    }

    @Test
    fun `вложенный интервал конфликтует`() {
        val outer = slot("outer", 10, 14)
        val inner = slot("inner", 11, 12)
        val result = run(outer, inner)
        assertEquals(listOf(inner), result[outer])
        assertEquals(listOf(outer), result[inner])
    }

    @Test
    fun `одинаковые интервалы конфликтуют`() {
        val a = slot("a", 10, 11)
        val b = slot("b", 10, 11)
        val result = run(a, b)
        assertEquals(listOf(b), result[a])
        assertEquals(listOf(a), result[b])
    }

    @Test
    fun `цепочка - средний конфликтует с двумя, крайние друг с другом нет`() {
        val a = slot("a", 10, 12)
        val b = slot("b", 11, 14)
        val c = slot("c", 13, 15)
        val result = run(a, b, c)
        assertEquals(listOf(b), result[a])
        assertEquals(setOf(a, c), result[b]!!.toSet())
        assertEquals(listOf(b), result[c])
    }

    @Test
    fun `непересекающиеся элементы отсутствуют в карте`() {
        val a = slot("a", 10, 11)
        val b = slot("b", 12, 13)
        val result = run(a, b)
        assertTrue(result.isEmpty())
        assertNull(result[a])
    }

    @Test
    fun `порядок входа не важен`() {
        val a = slot("a", 12, 14)
        val b = slot("b", 10, 13)
        val result = run(a, b)
        assertEquals(listOf(a), result[b])
    }

    @Test
    fun `пустой вход и одиночный элемент`() {
        assertTrue(detector.findConflicts(emptyList<Slot>(), Slot::startAt, Slot::endAt).isEmpty())
        assertTrue(run(slot("a", 10, 11)).isEmpty())
    }

    @Test
    fun `нулевая длительность не конфликтует`() {
        val point = slot("point", 10, 10)
        val around = slot("around", 9, 12)
        assertTrue(run(point, around).isEmpty())
    }
}
