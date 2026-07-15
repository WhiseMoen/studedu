package com.sapraliev.studedu.ui.util

/** Единое форматирование и ввод денежных сумм — было продублировано по экранам. */
object Money {

    fun format(amount: Double): String {
        val rounded = if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            "%.2f".format(amount)
        }
        return "$rounded ₽"
    }

    /** Значение для предзаполнения редактируемого поля суммы (без " ₽", без лишних нулей). */
    fun formatEditable(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
}

/** Фильтр ввода в текстовое поле суммы: только цифры и точка, разумный лимит длины. */
fun String.filterMoneyInput(): String =
    filter { it.isDigit() || it == '.' }.take(9)
