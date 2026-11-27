package com.example.bbps

import kotlinx.coroutines.delay

enum class Category(val label: String) {
    ELECTRICITY("Electricity"), MOBILE("Mobile Postpaid"), DTH("DTH"),
    WATER("Water"), GAS("Piped Gas"), BROADBAND("Broadband"), LPG("LPG Booking"), INSURANCE("Insurance")
}

data class Biller(val id: String, val name: String, val category: Category, val inputLabel: String)
data class BillDetails(val billerId: String, val customerName: String, val billNumber: String,
                       val amount: Double, val dueDate: String, val refId: String)
data class PaymentResult(val success: Boolean, val txnId: String, val message: String)

/**
 * Implement this against your BBPS provider (BBPS BOU / Agent Institution or an
 * aggregator such as Razorpay, Cashfree, Setu, PayU). Keep all calls going to YOUR
 * backend -- never put provider keys or BBPS certificates inside the app.
 */
interface BbpsRepository {
    suspend fun billers(category: Category): List<Biller>
    suspend fun fetchBill(biller: Biller, customerParam: String): Result<BillDetails>
    suspend fun pay(bill: BillDetails, paymentMode: String): PaymentResult
}

/** Offline demo implementation so the app runs without any backend. */
class MockBbpsRepository : BbpsRepository {
    private val all = listOf(
        Biller("UPPCL", "UP Power Corporation (UPPCL)", Category.ELECTRICITY, "Account Number"),
        Biller("MSEDCL", "MSEDCL Maharashtra", Category.ELECTRICITY, "Consumer Number"),
        Biller("AIRTELPOST", "Airtel Postpaid", Category.MOBILE, "Mobile Number"),
        Biller("JIOPOST", "Jio Postpaid", Category.MOBILE, "Mobile Number"),
        Biller("TATASKY", "Tata Play", Category.DTH, "Subscriber ID"),
        Biller("JALNIGAM", "Jal Nigam", Category.WATER, "Connection ID"),
        Biller("IGL", "Indraprastha Gas", Category.GAS, "BP Number"),
        Biller("ACT", "ACT Fibernet", Category.BROADBAND, "Account ID"),
        Biller("INDANE", "Indane LPG", Category.LPG, "Consumer ID"),
        Biller("LIC", "LIC Premium", Category.INSURANCE, "Policy Number"),
    )

    override suspend fun billers(category: Category): List<Biller> {
        delay(400); return all.filter { it.category == category }
    }

    override suspend fun fetchBill(biller: Biller, customerParam: String): Result<BillDetails> {
        delay(900)
        if (customerParam.length < 4) return Result.failure(Exception("Invalid ${biller.inputLabel}"))
        val amt = 300.0 + (customerParam.hashCode().mod(2000))
        return Result.success(
            BillDetails(biller.id, "Demo Customer", "BILL${customerParam.takeLast(4)}", amt, "05 Dec 2026", "REF${System.currentTimeMillis()}")
        )
    }

    override suspend fun pay(bill: BillDetails, paymentMode: String): PaymentResult {
        delay(1200)
        return PaymentResult(true, "TXN${System.currentTimeMillis()}", "Payment successful")
    }
}
