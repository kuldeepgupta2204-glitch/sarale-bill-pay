# Sarale Bill Payment App (Android, Kotlin + Jetpack Compose)

Open this folder in Android Studio (Koala+), let Gradle sync, and run. It ships with
`MockBbpsRepository`, so the full flow works offline:
category -> biller -> fetch bill -> pay -> receipt.

## Going live with real BBPS
BBPS is run by NPCI Bharat BillPay. You cannot call it directly as a normal app; you need either
1. to be an RBI-authorised BBPS Operating Unit / Agent Institution (via a sponsor bank), or
2. a licensed aggregator API (Razorpay, Cashfree, Setu, PayU, etc.).

Then: build a small backend that talks to the provider, and implement `BbpsRepository`
(`billers`, `fetchBill`, `pay`) with Retrofit calls to that backend. Keep secrets/certs server-side,
add login + KYC, store receipts, and handle pending/failed payment status and refunds.
