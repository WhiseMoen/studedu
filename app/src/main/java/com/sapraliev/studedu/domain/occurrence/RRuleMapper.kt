package com.sapraliev.studedu.domain.occurrence

import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.dmfs.rfc5545.recur.RecurrenceRule

/**
 * Сборка RFC 5545 RRULE-строки из нашей сущности правила.
 * Строку парсит dmfs lib-recur — так мы опираемся на его
 * проверенный парсер, а не на тонкости programmatic-API.
 */
object RRuleMapper {

    /**
     * Если заданы и count, и until — приоритет у count (RFC запрещает
     * оба сразу; наша форма создания события не даёт задать оба).
     */
    fun toRRuleString(rule: RecurrenceRuleEntity, zone: TimeZone): String {
        val sb = StringBuilder("FREQ=").append(rule.freq.name.uppercase())
        if (rule.interval > 1) sb.append(";INTERVAL=").append(rule.interval)
        rule.byweekday?.takeIf { it.isNotEmpty() }?.let { days ->
            sb.append(";BYDAY=").append(days.joinToString(","))
        }
        when {
            rule.count != null -> sb.append(";COUNT=").append(rule.count)
            rule.until != null -> {
                // UNTIL включительно: конец дня в зоне пользователя, в формате UTC.
                val endOfDay = rule.until.atTime(23, 59, 59).toInstant(zone)
                val utc = endOfDay.toLocalDateTime(TimeZone.UTC)
                sb.append(";UNTIL=")
                    .append("%04d%02d%02dT%02d%02d%02dZ".format(
                        utc.year, utc.monthNumber, utc.dayOfMonth,
                        utc.hour, utc.minute, utc.second,
                    ))
            }
        }
        return sb.toString()
    }

    fun toRRule(rule: RecurrenceRuleEntity, zone: TimeZone): RecurrenceRule =
        RecurrenceRule(toRRuleString(rule, zone))
}
