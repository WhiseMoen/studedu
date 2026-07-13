package com.sapraliev.studedu.ui.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Русские названия дат. kotlinx-datetime не локализует названия,
 * а java.time-локализация тянет за собой сюрпризы на разных прошивках —
 * для одного языка проще и надёжнее явные таблицы.
 */
object RussianDates {

    private val monthsGenitive = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )

    private val weekdaysFull = listOf(
        "понедельник", "вторник", "среда", "четверг",
        "пятница", "суббота", "воскресенье",
    )

    private val weekdaysShort = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

    private val monthsNominative = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )

    /** «пятница, 11 июля» */
    fun fullDate(date: LocalDate): String =
        "${weekdaysFull[date.dayOfWeek.ordinal]}, ${date.dayOfMonth} ${monthsGenitive[date.monthNumber - 1]}"

    /** «Июль 2026» */
    fun monthYear(date: LocalDate): String =
        "${monthsNominative[date.monthNumber - 1]} ${date.year}"

    /** «11 июля» */
    fun dayMonth(date: LocalDate): String =
        "${date.dayOfMonth} ${monthsGenitive[date.monthNumber - 1]}"

    fun weekdayShort(day: DayOfWeek): String = weekdaysShort[day.ordinal]

    fun weekdayFull(day: DayOfWeek): String = weekdaysFull[day.ordinal]
}
