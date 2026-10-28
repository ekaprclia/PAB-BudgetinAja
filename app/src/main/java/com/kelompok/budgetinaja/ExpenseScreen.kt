package com.kelompok.budgetinaja

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

// Memformat angka dengan titik ribuan untuk tampilan (misal: 10000 -> "10.000")
fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return formatter.format(amount)
}

// VisualTransformation agar titik ribuan muncul otomatis TANPA membuat kursor meloncat
class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val cleanDigits = originalText.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parsed = cleanDigits.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val formattedText = NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(cleanDigits.length)
                var dotsBefore = 0
                val totalDigits = cleanDigits.length
                for (i in 1..safeOffset) {
                    val digitsFromRight = totalDigits - i
                    if (digitsFromRight > 0 && digitsFromRight % 3 == 0) {
                        dotsBefore++
                    }
                }
                return (safeOffset + dotsBefore).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(formattedText.length)
                val digitsBefore = formattedText.take(safeOffset).count { it.isDigit() }
                return digitsBefore.coerceAtMost(cleanDigits.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Data") },
            text = { Text("Apakah Anda yakin ingin menghapus seluruh data budget dan daftar pengeluaran?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAll()
                        showResetDialog = false
                    }
                ) {
                    Text("Ya, Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BudgetinAja") },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Data")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BudgetAndFormSection(uiState = uiState, viewModel = viewModel)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ExpenseListSection(
                            uiState = uiState,
                            onDelete = viewModel::deleteExpense,
                            onReset = { showResetDialog = true }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BudgetAndFormSection(uiState = uiState, viewModel = viewModel)
                    ExpenseListSection(
                        uiState = uiState,
                        onDelete = viewModel::deleteExpense,
                        onReset = { showResetDialog = true }
                    )
                }
            }

            uiState.errorMessage?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }
    }
}

@Composable
fun BudgetAndFormSection(
    uiState: ExpenseUiState,
    viewModel: ExpenseViewModel
) {
    var budgetInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    val visualTransformation = remember { ThousandSeparatorVisualTransformation() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Ringkasan Budget", style = MaterialTheme.typography.titleMedium)
            Text(text = "Target Budget: Rp${formatRupiah(uiState.dailyBudget)},00")
            Text(
                text = "Sisa Budget: Rp${formatRupiah(uiState.remainingBudget)},00",
                color = if (uiState.remainingBudget < 0) Color.Red else Color.Unspecified
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { input ->
                        budgetInput = input.filter { it.isDigit() }
                    },
                    label = { Text("Set Budget Harian") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTransformation,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.setDailyBudget(budgetInput)
                    budgetInput = ""
                }) {
                    Text("Simpan")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Tambah Pengeluaran", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Nama Pengeluaran (misal: Makan Siang)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    amountInput = input.filter { it.isDigit() }
                },
                label = { Text("Nominal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.addExpense(titleInput, amountInput)
                    titleInput = ""
                    amountInput = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Tambah")
            }
        }
    }
}

@Composable
fun ExpenseListSection(
    uiState: ExpenseUiState,
    onDelete: (String) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Daftar Pengeluaran", style = MaterialTheme.typography.titleMedium)
        if (uiState.expenses.isNotEmpty() || uiState.dailyBudget > 0) {
            TextButton(onClick = onReset) {
                Text("Reset Semua", color = Color.Red)
            }
        }
    }

    if (uiState.expenses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada pengeluaran dicatat.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.expenses, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "Rp${formatRupiah(item.amount)},00", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus")
                        }
                    }
                }
            }
        }
    }
}