package com.sapraliev.studedu.domain.conflict

import kotlinx.datetime.Instant

/**
 * Поиск пересечений по времени — ядро приложения.
 *
 * Generic по типу элемента: сегодня это Occurrence, на Этапе 2 — общий
 * элемент ленты (личное событие или пара вуза). Детектор не знает и не
 * должен знать, что именно пересекается.
 *
 * Семантика:
 * - касание границ (end == start) — НЕ конфликт, как в Google Calendar;
 * - элементы нулевой длительности не конфликтуют;
 * - события «весь день» и скрытые пары сюда просто не передаются —
 *   фильтрация на совести вызывающего.
 */
class ConflictDetector {

    /**
     * @return для каждого конфликтующего элемента — список тех, с кем он
     * пересекается (в порядке начала). Элементы без конфликтов в карте
     * отсутствуют: `result[x] == null` значит «конфликтов нет».
     */
    fun <T : Any> findConflicts(
        items: List<T>,
        start: (T) -> Instant,
        end: (T) -> Instant,
    ): Map<T, List<T>> {
        // Элементы нулевой (или отрицательной) длительности времени не занимают.
        val timed = items.filter { start(it) < end(it) }
        if (timed.size < 2) return emptyMap()

        val sorted = timed.sortedBy(start)
        val conflicts = LinkedHashMap<T, MutableList<T>>()
        val active = ArrayDeque<T>()

        for (item in sorted) {
            val itemStart = start(item)
            // Всё, что закончилось к началу текущего (включая касание), — не конфликт.
            active.removeAll { end(it) <= itemStart }
            for (other in active) {
                conflicts.getOrPut(other) { mutableListOf() }.add(item)
                conflicts.getOrPut(item) { mutableListOf() }.add(other)
            }
            active.add(item)
        }
        return conflicts
    }
}
