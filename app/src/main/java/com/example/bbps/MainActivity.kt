package com.example.bbps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BbpsApp(MockBbpsRepository()) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BbpsApp(repo: BbpsRepository) {
    var category by remember { mutableStateOf<Category?>(null) }
    var biller by remember { mutableStateOf<Biller?>(null) }
    var bill by remember { mutableStateOf<BillDetails?>(null) }
    var result by remember { mutableStateOf<PaymentResult?>(null) }

    fun reset() { category = null; biller = null; bill = null; result = null }

    BackHandler(enabled = category != null) {
        when {
            result != null -> reset()
            bill != null -> bill = null
            biller != null -> biller = null
            else -> category = null
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Sarale Bill Payment App") }) }) { pad ->
        Box(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            when {
                result != null -> ResultScreen(result!!, ::reset)
                bill != null -> BillScreen(repo, bill!!) { result = it }
                biller != null -> FetchScreen(repo, biller!!) { bill = it }
                category != null -> BillerList(repo, category!!) { biller = it }
                else -> CategoryGrid { category = it }
            }
        }
    }
}

@Composable
fun CategoryGrid(onPick: (Category) -> Unit) {
    LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        gridItems(Category.values().toList()) { c ->
            Card(Modifier.fillMaxWidth().height(90.dp).clickable { onPick(c) }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(c.label, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
fun BillerList(repo: BbpsRepository, category: Category, onPick: (Biller) -> Unit) {
    var list by remember { mutableStateOf<List<Biller>?>(null) }
    LaunchedEffect(category) { list = repo.billers(category) }
    Column {
        Text(category.label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (list == null) CircularProgressIndicator()
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list!!) { b ->
                Card(Modifier.fillMaxWidth().clickable { onPick(b) }) { Text(b.name, Modifier.padding(16.dp)) }
            }
        }
    }
}

@Composable
fun FetchScreen(repo: BbpsRepository, biller: Biller, onBill: (BillDetails) -> Unit) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(biller.name, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(input, { input = it }, label = { Text(biller.inputLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading && input.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            loading = true; error = null
            scope.launch {
                repo.fetchBill(biller, input.trim()).onSuccess(onBill).onFailure { error = it.message }
                loading = false
            }
        }) { Text(if (loading) "Fetching..." else "Fetch Bill") }
    }
}

@Composable
fun BillScreen(repo: BbpsRepository, bill: BillDetails, onDone: (PaymentResult) -> Unit) {
    var mode by remember { mutableStateOf("UPI") }
    var paying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Customer: ${bill.customerName}")
                Text("Bill No: ${bill.billNumber}")
                Text("Due date: ${bill.dueDate}")
                Text("Amount: ₹%.2f".format(bill.amount), style = MaterialTheme.typography.headlineSmall)
            }
        }
        Text("Pay using")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("UPI", "Card", "NetBanking").forEach {
                FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(it) })
            }
        }
        Button(enabled = !paying, modifier = Modifier.fillMaxWidth(), onClick = {
            paying = true
            scope.launch { onDone(repo.pay(bill, mode)) }
        }) { Text(if (paying) "Processing..." else "Pay ₹%.2f".format(bill.amount)) }
    }
}

@Composable
fun ResultScreen(r: PaymentResult, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (r.success) "✅ ${r.message}" else "❌ ${r.message}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Txn ID: ${r.txnId}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onHome) { Text("Back to home") }
    }
}
