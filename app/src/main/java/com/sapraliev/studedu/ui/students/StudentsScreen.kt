package com.sapraliev.studedu.ui.students

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.StudentTint
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.StudentTintPalette
import com.sapraliev.studedu.ui.theme.neumorphic
import com.sapraliev.studedu.ui.util.Money
import com.sapraliev.studedu.ui.util.RussianDates
import com.sapraliev.studedu.ui.util.filterMoneyInput
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.UserCheck
import compose.icons.feathericons.UserMinus
import compose.icons.feathericons.X

@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel = viewModel(
        factory = StudentsViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    var addStudentOpen by remember { mutableStateOf(false) }

    val detail = state.detail
    if (detail != null) {
        BackHandler { viewModel.closeStudent() }
        StudentDetail(detail, viewModel)
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                FloatingActionButton(
                    onClick = { addStudentOpen = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    interactionSource = interactionSource,
                    modifier = Modifier.neumorphic(LocalNeuShadows.current, cornerRadius = 28.dp, pressed = pressed),
                ) {
                    Icon(FeatherIcons.Plus, contentDescription = "Добавить ученика")
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp),
                    ) {
                        Text(
                            "Ученики",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = state.showInactive,
                            onClick = { viewModel.setShowInactive(!state.showInactive) },
                            label = { Text("Неактивные") },
                        )
                    }
                }
                if (state.students.isEmpty()) {
                    item {
                        Text(
                            "Добавь первого ученика кнопкой «+». Предметы, ставки и оплаты — всё будет здесь.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.students, key = { it.student.id }) { overview ->
                    Card(
                        onClick = { viewModel.openStudent(overview.student.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .alpha(if (overview.student.active) 1f else 0.5f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (overview.student.active) {
                                        overview.student.name
                                    } else {
                                        "${overview.student.name} · не активен"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val subjects = overview.enrollments.joinToString(" · ") { e ->
                                    e.pricePerLesson?.let { "${e.subject} ${Money.format(it)}" }
                                        ?: e.subject
                                }
                                if (subjects.isNotEmpty()) {
                                    Text(
                                        subjects,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            BalanceText(overview.balance)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (addStudentOpen) {
        StudentEditorDialog(
            title = "Новый ученик",
            askName = true,
            onDismiss = { addStudentOpen = false },
            onSave = { name, contact, subject, price, fee ->
                viewModel.addStudent(name, contact, subject, price, fee)
                addStudentOpen = false
            },
        )
    }
}

@Composable
private fun StudentDetail(detail: StudentDetailState, viewModel: StudentsViewModel) {
    var paymentOpen by remember { mutableStateOf(false) }
    var enrollmentOpen by remember { mutableStateOf(false) }
    var editStudentOpen by remember { mutableStateOf(false) }
    var deleteStudentOpen by remember { mutableStateOf(false) }
    var editingEnrollment by remember { mutableStateOf<EnrollmentEntity?>(null) }
    var deletingEnrollment by remember { mutableStateOf<EnrollmentEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                IconButton(onClick = { viewModel.closeStudent() }) {
                    Icon(
                        FeatherIcons.ArrowLeft,
                        contentDescription = "Назад",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        detail.student.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    detail.student.contact?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!detail.student.active) {
                        Text(
                            "не активен",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConflictRed,
                        )
                    }
                }
                IconButton(onClick = { editStudentOpen = true }) {
                    Icon(
                        FeatherIcons.Edit2,
                        contentDescription = "Изменить ученика",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { viewModel.setStudentActive(!detail.student.active) }) {
                    Icon(
                        if (detail.student.active) FeatherIcons.UserMinus else FeatherIcons.UserCheck,
                        contentDescription = if (detail.student.active) {
                            "Сделать неактивным"
                        } else {
                            "Сделать активным"
                        },
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { deleteStudentOpen = true }) {
                    Icon(
                        FeatherIcons.Trash2,
                        contentDescription = "Удалить ученика",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Баланс",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.format(detail.balance),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            detail.balance < 0 -> ConflictRed
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                    detail.prepaidLessons?.let {
                        Text(
                            "≈ $it занятий предоплачено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (detail.balance < 0) {
                        Text(
                            "долг",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConflictRed,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { paymentOpen = true }) { Text("Платёж +") }
                        OutlinedButton(onClick = { enrollmentOpen = true }) { Text("Предмет +") }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Предметы",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (detail.enrollments.isEmpty()) {
                        Text(
                            "Пока нет предметов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    detail.enrollments.forEachIndexed { index, enrollment ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(enrollment.subject, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    enrollmentRateLabel(
                                        enrollment,
                                        monthCovered = enrollment.id in detail.monthCoveredEnrollmentIds,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editingEnrollment = enrollment }) {
                                Icon(
                                    FeatherIcons.Edit2,
                                    contentDescription = "Изменить предмет ${enrollment.subject}",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { deletingEnrollment = enrollment }) {
                                Icon(
                                    FeatherIcons.Trash2,
                                    contentDescription = "Удалить предмет ${enrollment.subject}",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val summary = detail.summary
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.shiftMonth(1) }) {
                            Icon(
                                FeatherIcons.ArrowLeft,
                                contentDescription = "Раньше",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            monthTitle(summary.monthStart),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                            Icon(
                                FeatherIcons.ArrowRight,
                                contentDescription = "Позже",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text("Занятий: ${summary.lessonsTotal}")
                    summary.lessonsBySubject.forEach { (subject, count) ->
                        Text(
                            "  · $subject — $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Начислено: ${Money.format(summary.charged)}")
                    Text("Получено: ${Money.format(summary.paid)}")
                }
            }
        }

        item {
            Text(
                "История",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (detail.summary.history.isEmpty()) {
            item {
                Text(
                    "В этом месяце пусто",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(detail.summary.history.size) { index ->
            val item = detail.summary.history[index]
            HistoryRow(item)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        }

        item { Spacer(Modifier.height(40.dp)) }
    }

    if (paymentOpen) {
        PaymentDialog(
            enrollments = detail.enrollments,
            onDismiss = { paymentOpen = false },
            onSaveOneOff = { amount, comment ->
                viewModel.addPayment(amount, comment)
                paymentOpen = false
            },
            onSaveMonth = { enrollmentId, amount, comment ->
                viewModel.addMonthPayment(enrollmentId, amount, comment)
                paymentOpen = false
            },
            onSavePackage = { enrollmentId, amount, lessonsCount, comment ->
                viewModel.addPackagePayment(enrollmentId, amount, lessonsCount, comment)
                paymentOpen = false
            },
        )
    }
    if (enrollmentOpen) {
        StudentEditorDialog(
            title = "Новый предмет",
            askName = false,
            onDismiss = { enrollmentOpen = false },
            onSave = { _, _, subject, price, fee ->
                viewModel.addEnrollment(subject, price, fee)
                enrollmentOpen = false
            },
        )
    }
    if (editStudentOpen) {
        EditStudentDialog(
            initialName = detail.student.name,
            initialContact = detail.student.contact,
            currentTint = detail.student.colorTint,
            onDismiss = { editStudentOpen = false },
            onSave = { name, contact ->
                viewModel.updateStudent(name, contact)
                editStudentOpen = false
            },
            onTintSelected = viewModel::setStudentTint,
        )
    }
    if (deleteStudentOpen) {
        AlertDialog(
            onDismissRequest = { deleteStudentOpen = false },
            title = { Text("Удалить ученика?") },
            text = {
                Text("«${detail.student.name}» и вся история занятий/платежей будут удалены безвозвратно.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudent()
                    deleteStudentOpen = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteStudentOpen = false }) { Text("Отмена") }
            },
        )
    }
    editingEnrollment?.let { enrollment ->
        StudentEditorDialog(
            title = "Изменить предмет",
            askName = false,
            initialSubject = enrollment.subject,
            initialPrice = enrollment.pricePerLesson,
            initialMonthlyFee = enrollment.monthlyFee,
            onDismiss = { editingEnrollment = null },
            onSave = { _, _, subject, price, fee ->
                viewModel.updateEnrollment(enrollment.id, subject, price, fee)
                editingEnrollment = null
            },
        )
    }
    deletingEnrollment?.let { enrollment ->
        AlertDialog(
            onDismissRequest = { deletingEnrollment = null },
            title = { Text("Удалить предмет?") },
            text = { Text("«${enrollment.subject}» будет удалён. История занятий и платежей останется.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEnrollment(enrollment.id)
                    deletingEnrollment = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deletingEnrollment = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            when (item) {
                is HistoryItem.Lesson -> {
                    Text(
                        "${RussianDates.dayMonth(item.date)} — занятие · ${item.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    item.topics?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "темы: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.homework?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "дз: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is HistoryItem.Money -> {
                    val kind = if (item.isIncome) "платёж" else "начислено"
                    val subject = item.subject?.let { " · $it" } ?: ""
                    Text(
                        "${RussianDates.dayMonth(item.date)} — $kind$subject",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    item.comment?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (item is HistoryItem.Money) {
            Text(
                (if (item.isIncome) "+" else "−") + Money.format(item.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isIncome) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun BalanceText(balance: Double) {
    Text(
        Money.format(balance),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = when {
            balance < 0 -> ConflictRed
            balance > 0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/**
 * Диалог создания ученика (askName) или добавления/изменения предмета.
 * Ставка за занятие и сумма за месяц — оба поля всегда видны и независимы
 * друг от друга: способ оплаты выбирается не здесь, а в момент платежа.
 */
@Composable
private fun StudentEditorDialog(
    title: String,
    askName: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        contact: String?,
        subject: String,
        price: Double?,
        monthlyFee: Double?,
    ) -> Unit,
    initialSubject: String = "",
    initialPrice: Double? = null,
    initialMonthlyFee: Double? = null,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(initialSubject) }
    var priceText by remember { mutableStateOf(initialPrice?.let { Money.formatEditable(it) } ?: "") }
    var feeText by remember { mutableStateOf(initialMonthlyFee?.let { Money.formatEditable(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (askName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Контакт (телефон/tg)") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Предмет") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filterMoneyInput() },
                    label = { Text("Ставка за занятие, ₽") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filterMoneyInput() },
                    label = { Text("Обычная сумма за месяц, ₽ (необязательно)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (subject.isBlank() || (askName && name.isBlank())) return@TextButton
                    onSave(
                        name,
                        contact.takeIf { it.isNotBlank() },
                        subject,
                        priceText.toDoubleOrNull(),
                        feeText.toDoubleOrNull(),
                    )
                },
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private enum class PaymentMode { ONE_OFF, MONTH, PACKAGE }

/**
 * Способ оплаты выбирается здесь, а не на предмете: разовый (просто
 * пополняет баланс), за месяц (все занятия этого предмета в текущем
 * просматриваемом месяце — бесплатны) или пакетом N занятий (списывается
 * по одному при «Проведено»). Месяц/пакет требуют выбора предмета, если
 * их у ученика больше одного.
 */
@Composable
private fun PaymentDialog(
    enrollments: List<EnrollmentEntity>,
    onDismiss: () -> Unit,
    onSaveOneOff: (amount: Double, comment: String?) -> Unit,
    onSaveMonth: (enrollmentId: String, amount: Double, comment: String?) -> Unit,
    onSavePackage: (enrollmentId: String, amount: Double, lessonsCount: Int, comment: String?) -> Unit,
) {
    var mode by remember { mutableStateOf(PaymentMode.ONE_OFF) }
    var selectedEnrollmentId by remember { mutableStateOf(enrollments.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf("") }
    var lessonsText by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    // Диалог закрывается сразу после сохранения, но окно между двойным тапом
    // и рекомпозицией остаётся — без этой защиты быстрый двойной тап на
    // «Сохранить» мог бы завести два платежа (особенно опасно для «Месяца»).
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Платёж от ученика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mode == PaymentMode.ONE_OFF,
                        onClick = { mode = PaymentMode.ONE_OFF },
                        label = { Text("Разовый") },
                    )
                    FilterChip(
                        selected = mode == PaymentMode.MONTH,
                        onClick = { mode = PaymentMode.MONTH },
                        label = { Text("Месяц") },
                        enabled = enrollments.isNotEmpty(),
                    )
                    FilterChip(
                        selected = mode == PaymentMode.PACKAGE,
                        onClick = { mode = PaymentMode.PACKAGE },
                        label = { Text("Пакет") },
                        enabled = enrollments.isNotEmpty(),
                    )
                }
                if (mode != PaymentMode.ONE_OFF) {
                    if (enrollments.isEmpty()) {
                        Text(
                            "Сначала добавь предмет ученику кнопкой «Предмет +».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (enrollments.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            enrollments.forEach { enrollment ->
                                FilterChip(
                                    selected = selectedEnrollmentId == enrollment.id,
                                    onClick = { selectedEnrollmentId = enrollment.id },
                                    label = { Text(enrollment.subject) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filterMoneyInput() },
                    label = { Text("Сумма, ₽") },
                    singleLine = true,
                )
                if (mode == PaymentMode.PACKAGE) {
                    OutlinedTextField(
                        value = lessonsText,
                        onValueChange = { lessonsText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Занятий в пакете") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (submitted) return@TextButton
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    if (amount <= 0) return@TextButton
                    val commentValue = comment.takeIf { it.isNotBlank() }
                    when (mode) {
                        PaymentMode.ONE_OFF -> {
                            submitted = true
                            onSaveOneOff(amount, commentValue)
                        }
                        PaymentMode.MONTH -> {
                            val enrollmentId = selectedEnrollmentId ?: return@TextButton
                            submitted = true
                            onSaveMonth(enrollmentId, amount, commentValue)
                        }
                        PaymentMode.PACKAGE -> {
                            val enrollmentId = selectedEnrollmentId ?: return@TextButton
                            val lessonsCount = lessonsText.toIntOrNull() ?: return@TextButton
                            submitted = true
                            onSavePackage(enrollmentId, amount, lessonsCount, commentValue)
                        }
                    }
                },
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun EditStudentDialog(
    initialName: String,
    initialContact: String?,
    currentTint: StudentTint?,
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String?) -> Unit,
    onTintSelected: (StudentTint?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var contact by remember { mutableStateOf(initialContact ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить ученика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Контакт (телефон/tg)") },
                    singleLine = true,
                )
                Text(
                    "Оттенок карточек занятий",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TintSwatchRow(selected = currentTint, onSelect = onTintSelected)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    onSave(name, contact.takeIf { it.isNotBlank() })
                },
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Ряд свотчей оттенка + «сброс» (обычный цвет типа события). */
@Composable
private fun TintSwatchRow(selected: StudentTint?, onSelect: (StudentTint?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TintSwatch(
            color = MaterialTheme.colorScheme.surfaceVariant,
            isSelected = selected == null,
            onClick = { onSelect(null) },
            content = {
                Icon(
                    FeatherIcons.X,
                    contentDescription = "Без оттенка",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        StudentTintPalette.entries.forEach { (tint, _) ->
            TintSwatch(
                color = StudentTintPalette.colorFor(tint),
                isSelected = selected == tint,
                onClick = { onSelect(tint) },
            )
        }
    }
}

@Composable
private fun TintSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content?.invoke()
    }
}

private fun enrollmentRateLabel(enrollment: EnrollmentEntity, monthCovered: Boolean): String {
    val parts = buildList {
        add(enrollment.pricePerLesson?.let { "${Money.format(it)}/занятие" } ?: "ставка не задана")
        enrollment.monthlyFee?.let { add("${Money.format(it)}/мес") }
        if (enrollment.remainingPackageLessons > 0) {
            add("осталось ${enrollment.remainingPackageLessons} по пакету")
        }
        if (monthCovered) add("месяц оплачен")
    }
    return parts.joinToString(" · ")
}

private fun monthTitle(monthStart: kotlinx.datetime.LocalDate): String {
    val months = listOf(
        "январь", "февраль", "март", "апрель", "май", "июнь",
        "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
    )
    return "${months[monthStart.monthNumber - 1]} ${monthStart.year}"
}
