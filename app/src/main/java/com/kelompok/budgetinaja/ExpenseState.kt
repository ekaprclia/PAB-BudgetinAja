package com.kelompok.budgetinaja

import java.util.UUID

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double
)

data class ExpenseUiState(
    val dailyBudget: Double = 0.0,
    val expenses: List<ExpenseItem> = emptyList(),
    val errorMessage: String? = null
) {
    val remainingBudget: Double
        get() = dailyBudget - expenses.sumOf { it.amount }
}