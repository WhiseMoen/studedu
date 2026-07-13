package com.sapraliev.studedu.data.schedule

import com.sapraliev.studedu.domain.schedule.ScheduleEntry
import com.sapraliev.studedu.domain.schedule.ScheduleProvider
import com.sapraliev.studedu.domain.schedule.ScheduleSyncException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Адаптер расписания Московского Политеха.
 *
 * Источник — открытый JSON https://rasp.dmami.ru/site/group?group=XXX
 * (его же используют mospolyhelper/Edugma). Формат ответа:
 * `grid` → день недели "1".."7" → номер пары "1".."7" → список занятий
 * с полями sbj/teacher/type/auditories/df/dt.
 *
 * ВНИМАНИЕ: контракт внешний. Сайт периодически уходит на техобслуживание
 * и отдаёт HTML-заглушку — это распознаётся и превращается в понятную
 * ошибку, кэш при этом не трогается.
 */
class MospolytechProvider(
    private val client: HttpClient = sharedClient,
) : ScheduleProvider {

    override val id: String = "mospolytech"

    override suspend fun getSchedule(
        group: String,
        from: LocalDate,
        to: LocalDate,
    ): List<ScheduleEntry> {
        val body = try {
            client.get(ENDPOINT) {
                parameter("group", group)
                parameter("session", 0)
                header("Referer", "https://rasp.dmami.ru/")
                header("X-Requested-With", "XMLHttpRequest")
                header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
                )
            }.bodyAsText()
        } catch (e: Exception) {
            throw ScheduleSyncException("Нет связи с rasp.dmami.ru: ${e.message}", e)
        }

        // Сайт на техобслуживании или вернул страницу вместо JSON.
        if (body.trimStart().startsWith("<")) {
            throw ScheduleSyncException(
                "Сайт расписания сейчас недоступен (техобслуживание). " +
                    "Старое расписание в приложении сохранено — попробуй синк позже.",
            )
        }

        return try {
            parse(body, from, to)
        } catch (e: ScheduleSyncException) {
            throw e
        } catch (e: Exception) {
            throw ScheduleSyncException("Формат расписания изменился: ${e.message}", e)
        }
    }

    private fun parse(body: String, from: LocalDate, to: LocalDate): List<ScheduleEntry> {
        val root = Json.parseToJsonElement(body).jsonObject
        val grid = root["grid"]?.jsonObject
            ?: throw ScheduleSyncException("В ответе нет grid — возможно, группа не найдена")

        val zone = TimeZone.of("Europe/Moscow")
        val result = mutableListOf<ScheduleEntry>()

        var date = from
        while (date <= to) {
            // В grid дни недели: "1" = понедельник ... "7" = воскресенье.
            val dayLessons = grid[(date.dayOfWeek.ordinal + 1).toString()]?.jsonObject
            if (dayLessons != null) {
                for ((pairNumber, lessons) in dayLessons) {
                    val slot = PAIR_TIMES[pairNumber] ?: continue
                    val lessonsArray = runCatching { lessons.jsonArray }.getOrNull() ?: continue
                    for (lesson in lessonsArray) {
                        val obj = runCatching { lesson.jsonObject }.getOrNull() ?: continue
                        val subject = obj["sbj"]?.jsonPrimitive?.content?.trim()
                            ?.takeIf { it.isNotEmpty() } ?: continue

                        // Занятие действует в интервале дат df..dt (если заданы).
                        val dateFrom = obj["df"]?.jsonPrimitive?.content
                            ?.takeIf { it.isNotBlank() }?.let(::parseDateSafe)
                        val dateTo = obj["dt"]?.jsonPrimitive?.content
                            ?.takeIf { it.isNotBlank() }?.let(::parseDateSafe)
                        if (dateFrom != null && date < dateFrom) continue
                        if (dateTo != null && date > dateTo) continue

                        val place = obj["auditories"]?.let { auditories ->
                            runCatching {
                                auditories.jsonArray.mapNotNull { a ->
                                    a.jsonObject["title"]?.jsonPrimitive?.content
                                }.joinToString(", ") { stripHtml(it) }
                            }.getOrNull()
                        }?.takeIf { it.isNotBlank() }

                        result += ScheduleEntry(
                            subject = subject,
                            teacher = obj["teacher"]?.jsonPrimitive?.content
                                ?.trim()?.takeIf { it.isNotEmpty() },
                            place = place,
                            lessonType = obj["type"]?.jsonPrimitive?.content
                                ?.trim()?.takeIf { it.isNotEmpty() },
                            start = date.atTime(slot.first).toInstant(zone),
                            end = date.atTime(slot.second).toInstant(zone),
                        )
                    }
                }
            }
            date = date.plus(DatePeriod(days = 1))
        }
        return result.sortedBy { it.start }
    }

    private fun parseDateSafe(text: String): LocalDate? =
        runCatching { LocalDate.parse(text) }.getOrNull()

    private fun stripHtml(text: String): String =
        text.replace(Regex("<[^>]*>"), "").trim()

    companion object {
        private const val ENDPOINT = "https://rasp.dmami.ru/site/group"

        /** Один HTTP-клиент на всё приложение: пул соединений и потоки общие. */
        val sharedClient: HttpClient by lazy { HttpClient(OkHttp) }

        /** Звонковая сетка Политеха (дневная форма). */
        val PAIR_TIMES: Map<String, Pair<LocalTime, LocalTime>> = mapOf(
            "1" to (LocalTime(9, 0) to LocalTime(10, 30)),
            "2" to (LocalTime(10, 40) to LocalTime(12, 10)),
            "3" to (LocalTime(12, 20) to LocalTime(13, 50)),
            "4" to (LocalTime(14, 30) to LocalTime(16, 0)),
            "5" to (LocalTime(16, 10) to LocalTime(17, 40)),
            "6" to (LocalTime(18, 0) to LocalTime(19, 30)),
            "7" to (LocalTime(19, 40) to LocalTime(21, 10)),
        )
    }
}
