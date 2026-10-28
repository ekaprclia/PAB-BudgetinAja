package com.kelompok.budgetinaja

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExpenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    fun setDailyBudget(budgetInput: String) {
        // Hapus titik/koma sebelum konversi ke Double
        val cleanInput = budgetInput.replace(".", "").replace(",", "").trim()
        val budget = cleanInput.toDoubleOrNull()
        if (budget == null || budget <= 0) {
            _uiState.update { it.copy(errorMessage = "Budget harus berupa angka positif!") }
            return
        }
        _uiState.update { it.copy(dailyBudget = budget, errorMessage = null) }
    }

    fun addExpense(title: String, amountInput: String) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama pengeluaran tidak boleh kosong!") }
            return
        }

        // Hapus titik/koma sebelum konversi ke Double
        val cleanInput = amountInput.replace(".", "").replace(",", "").trim()
        val amount = cleanInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Nominal pengeluaran harus berupa angka valid!") }
            return
        }

        val newExpense = ExpenseItem(title = title, amount = amount)
        _uiState.update { currentState ->
            currentState.copy(
                expenses = currentState.expenses + newExpense,
                errorMessage = null
            )
        }
    }

    fun deleteExpense(id: String) {
        _uiState.update { currentState ->
            currentState.copy(
                expenses = currentState.expenses.filterNot { it.id == id }
            )
        }
    }

    fun resetAll() {
        _uiState.update {
            ExpenseUiState()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}