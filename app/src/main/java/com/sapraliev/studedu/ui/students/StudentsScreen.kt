package com.sapraliev.studedu.ui.students

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.BillingMode
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.neumorphic
import com.sapraliev.studedu.ui.util.RussianDates
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.UserCheck
import compose.icons.feathericons.UserMinus

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
                                    e.pricePerLesson?.let { "${e.subject} ${formatMoney(it)}" }
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
            onSave = { name, contact, subject, price, mode, fee ->
                viewModel.addStudent(name, contact, subject, price, mode, fee)
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
                        formatMoney(detail.balance),
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
                                    enrollmentRateLabel(enrollment),
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
                    Text("Начислено: ${formatMoney(summary.charged)}")
                    Text("Получено: ${formatMoney(summary.paid)}")
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
            onDismiss = { paymentOpen = false },
            onSave = { amount, comment ->
                viewModel.addPayment(amount, comment)
                paymentOpen = false
            },
        )
    }
    if (enrollmentOpen) {
        StudentEditorDialog(
            title = "Новый предмет",
            askName = false,
            onDismiss = { enrollmentOpen = false },
            onSave = { _, _, subject, price, mode, fee ->
                viewModel.addEnrollment(subject, price, mode, fee)
                enrollmentOpen = false
            },
        )
    }
    if (editStudentOpen) {
        EditStudentDialog(
            initialName = detail.student.name,
            initialContact = detail.student.contact,
            onDismiss = { editStudentOpen = false },
            onSave = { name, contact ->
                viewModel.updateStudent(name, contact)
                editStudentOpen = false
            },
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
            initialMode = enrollment.billingMode,
            initialMonthlyFee = enrollment.monthlyFee,
            onDismiss = { editingEnrollment = null },
            onSave = { _, _, subject, price, mode, fee ->
                viewModel.updateEnrollment(enrollment.id, subject, price, mode, fee)
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
                (if (item.isIncome) "+" else "−") + formatMoney(item.amount),
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
        formatMoney(balance),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = when {
            balance < 0 -> ConflictRed
            balance > 0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/** Диалог создания ученика (askName) или добавления предмета. */
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
        mode: BillingMode,
        monthlyFee: Double?,
    ) -> Unit,
    initialSubject: String = "",
    initialPrice: Double? = null,
    initialMode: BillingMode = BillingMode.PER_LESSON,
    initialMonthlyFee: Double? = null,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(initialSubject) }
    var priceText by remember { mutableStateOf(initialPrice?.let { formatEditableAmount(it) } ?: "") }
    var mode by remember { mutableStateOf(initialMode) }
    var feeText by remember { mutableStateOf(initialMonthlyFee?.let { formatEditableAmount(it) } ?: "") }

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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mode == BillingMode.PER_LESSON,
                        onClick = { mode = BillingMode.PER_LESSON },
                        label = { Text("Поурочно") },
                    )
                    FilterChip(
                        selected = mode == BillingMode.PACKAGE,
                        onClick = { mode = BillingMode.PACKAGE },
                        label = { Text("Пакет") },
                    )
                    FilterChip(
                        selected = mode == BillingMode.MONTHLY,
                        onClick = { mode = BillingMode.MONTHLY },
                        label = { Text("Фикс/мес") },
                    )
                }
                if (mode == BillingMode.MONTHLY) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filterMoney() },
                        label = { Text("Сумма в месяц, ₽") },
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filterMoney() },
                        label = { Text("Ставка за занятие, ₽") },
                        singleLine = true,
                    )
                }
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
                        mode,
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

@Composable
private fun PaymentDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, comment: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Платёж от ученика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filterMoney() },
                    label = { Text("Сумма, ₽") },
                    singleLine = true,
                )
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
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    if (amount > 0) onSave(amount, comment.takeIf { it.isNotBlank() })
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
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String?) -> Unit,
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

private fun enrollmentRateLabel(enrollment: EnrollmentEntity): String = when (enrollment.billingMode) {
    BillingMode.MONTHLY -> enrollment.monthlyFee?.let { "${formatMoney(it)}/мес" } ?: "фикс/мес — ставка не задана"
    BillingMode.PACKAGE -> enrollment.pricePerLesson?.let { "${formatMoney(it)}/занятие · пакет" } ?: "пакет"
    BillingMode.PER_LESSON -> enrollment.pricePerLesson?.let { "${formatMoney(it)}/занятие" } ?: "ставка не задана"
}

private fun formatEditableAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()

private fun String.filterMoney(): String =
    filter { it.isDigit() || it == '.' }.take(9)

private fun formatMoney(amount: Double): String {
    val rounded = if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        "%.2f".format(amount)
    }
    return "$rounded ₽"
}

private fun monthTitle(monthStart: kotlinx.datetime.LocalDate): String {
    val months = listOf(
        "январь", "февраль", "март", "апрель", "май", "июнь",
        "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
    )
    return "${months[monthStart.monthNumber - 1]} ${monthStart.year}"
}
