package com.sapraliev.studedu.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.neumorphic
import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Сетка недели/дня: те же неоморфные рамки, что у сетки месяца ([MonthGrid]),
 * только вместо квадратов дней — вертикальная временная шкала (0–24 ч), по
 * горизонтали — колонки дней (для недели) или одна колонка (для дня).
 * Пересекающиеся события раскладываются по «дорожкам» ([layoutLanes]),
 * как в большинстве календарей.
 */
private val HOUR_HEIGHT = 60.dp
private val GUTTER_WIDTH = 34.dp

@Composable
fun DayTimelineGrid(
    section: DaySection?,
    now: Instant,
    onCardClick: (ScheduleCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = TimeZone.currentSystemDefault()
    val nowLocal = now.toLocalDateTime(zone)
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    LaunchedEffect(section?.date) {
        val isToday = section?.date == nowLocal.date
        val targetHour = if (isToday) (nowLocal.hour - 2).coerceIn(0, 22) else 7
        val targetPx = with(density) { (HOUR_HEIGHT * targetHour).toPx() }
        scrollState.scrollTo(targetPx.roundToInt())
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(LocalNeuShadows.current, cornerRadius = 24.dp, blur = 12.dp, offset = 5.dp),
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            HourGutter()
            DayColumn(
                cards = section?.cards.orEmpty(),
                dayDate = section?.date ?: nowLocal.date,
                now = now,
                onClick = onCardClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun WeekTimelineGrid(
    days: List<DaySection>,
    selectedDate: LocalDate,
    now: Instant,
    onDayHeaderClick: (LocalDate) -> Unit,
    onCardClick: (ScheduleCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = TimeZone.currentSystemDefault()
    val nowLocal = now.toLocalDateTime(zone)
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val targetHour = (nowLocal.hour - 2).coerceIn(0, 22)
        val targetPx = with(density) { (HOUR_HEIGHT * targetHour).toPx() }
        scrollState.scrollTo(targetPx.roundToInt())
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(LocalNeuShadows.current, cornerRadius = 24.dp, blur = 12.dp, offset = 5.dp),
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxSize(),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(GUTTER_WIDTH))
                days.forEach { section ->
                    WeekDayHeaderCell(
                        section = section,
                        isSelected = section.date == selectedDate,
                        isToday = section.date == nowLocal.date,
                        onClick = { onDayHeaderClick(section.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                HourGutter()
                days.forEach { section ->
                    DayColumn(
                        cards = section.cards,
                        dayDate = section.date,
                        now = now,
                        onClick = onCardClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayHeaderCell(
    section: DaySection,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekdayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(10.dp),
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            weekdayLabels[section.date.dayOfWeek.ordinal],
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            section.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun HourGutter(modifier: Modifier = Modifier) {
    Column(modifier.width(GUTTER_WIDTH)) {
        for (hour in 0 until 24) {
            Box(Modifier.height(HOUR_HEIGHT)) {
                Text(
                    "$hour:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    cards: List<ScheduleCard>,
    dayDate: LocalDate,
    now: Instant,
    onClick: (ScheduleCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = TimeZone.currentSystemDefault()
    val dayStart = dayDate.atTime(0, 0).toInstant(zone)
    val lanes = remember(cards) { layoutLanes(cards) }

    BoxWithConstraints(modifier.height(HOUR_HEIGHT * 24)) {
        HourGridLines(Modifier.fillMaxSize())
        if (dayDate == now.toLocalDateTime(zone).date) {
            NowLine(now = now, dayStart = dayStart)
        }
        cards.forEach { card ->
            val lane = lanes.getValue(card.key)
            val laneWidth = maxWidth / lane.count
            val startMinutes = (card.start - dayStart).inWholeMinutes.coerceIn(0, 24 * 60)
            val minEndMinutes = (startMinutes + 20).coerceAtMost(24 * 60)
            val endMinutes = (card.end - dayStart).inWholeMinutes.coerceIn(minEndMinutes, 24 * 60)
            val topOffset = HOUR_HEIGHT * (startMinutes / 60f)
            val blockHeight = HOUR_HEIGHT * ((endMinutes - startMinutes) / 60f)
            TimelineEventBlock(
                card = card,
                now = now,
                onClick = { onClick(card) },
                modifier = Modifier
                    .offset(x = laneWidth * lane.index, y = topOffset)
                    .width(laneWidth)
                    .height(blockHeight),
            )
        }
    }
}

@Composable
private fun HourGridLines(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Canvas(modifier) {
        val hourPx = size.height / 24f
        for (hour in 0..24) {
            val y = hourPx * hour
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
private fun NowLine(now: Instant, dayStart: Instant) {
    val minutes = ((now - dayStart).inWholeMinutes).coerceIn(0, 24 * 60)
    val topOffset = HOUR_HEIGHT * (minutes / 60f)
    Box(
        Modifier
            .padding(top = topOffset)
            .fillMaxWidth()
            .height(2.dp)
            .background(ConflictRed),
    )
}

@Composable
private fun TimelineEventBlock(
    card: ScheduleCard,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (baseColor, washAlpha) = scheduleCardColors(card, now)
    val hasConflict = card.conflictTitles.isNotEmpty()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        border = if (hasConflict) BorderStroke(1.5.dp, ConflictRed) else null,
        modifier = modifier
            .padding(horizontal = 1.dp)
            .neumorphic(LocalNeuShadows.current, cornerRadius = 8.dp, blur = 5.dp, offset = 2.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(baseColor.copy(alpha = washAlpha))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        ) {
            Text(
                card.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Позиция события в стопке пересекающихся событий: [index]-я из [count] дорожек. */
private data class LaneInfo(val index: Int, val count: Int)

/**
 * Раскладка пересекающихся событий по дорожкам (как в большинстве календарей):
 * события группируются в кластеры по цепочке пересечений, внутри кластера —
 * жадное распределение по первой свободной дорожке. Все события кластера
 * получают одинаковое общее число дорожек — иначе ширины соседних событий
 * не совпадали бы визуально без видимой причины.
 */
private fun layoutLanes(cards: List<ScheduleCard>): Map<String, LaneInfo> {
    val sorted = cards.sortedBy { it.start }
    val result = mutableMapOf<String, LaneInfo>()

    var clusterCards = mutableListOf<ScheduleCard>()
    var clusterEnd: Instant? = null
    val clusters = mutableListOf<List<ScheduleCard>>()
    for (card in sorted) {
        val end = clusterEnd
        if (end == null || card.start < end) {
            clusterCards.add(card)
            clusterEnd = if (end == null || card.end > end) card.end else end
        } else {
            clusters += clusterCards
            clusterCards = mutableListOf(card)
            clusterEnd = card.end
        }
    }
    if (clusterCards.isNotEmpty()) clusters += clusterCards

    for (cluster in clusters) {
        val laneEnds = mutableListOf<Instant>()
        val laneIndexByKey = mutableMapOf<String, Int>()
        for (card in cluster) {
            var placedLane = -1
            for (i in laneEnds.indices) {
                if (card.start >= laneEnds[i]) {
                    placedLane = i
                    break
                }
            }
            if (placedLane == -1) {
                laneEnds += card.end
                placedLane = laneEnds.size - 1
            } else {
                laneEnds[placedLane] = card.end
            }
            laneIndexByKey[card.key] = placedLane
        }
        val laneCount = laneEnds.size
        cluster.forEach { card -> result[card.key] = LaneInfo(laneIndexByKey.getValue(card.key), laneCount) }
    }
    return result
}
