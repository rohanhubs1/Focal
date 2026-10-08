package com.focal.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.model.User

enum class EntitlementState {
    FULL_ACCESS,      // Trial, Active, or Grace Period
    READ_ONLY_EXPIRED // Expired entitlement: Parent view-only, rules enforced on child
}

data class SubscriptionDetails(
    val statusText: String,
    val isTrial: Boolean,
    val trialDaysRemaining: Int,
    val renewalDate: String,
    val monthlyPriceInr: Int = 299,
    val paymentFailed: Boolean = false,
    val isExpired: Boolean = false,
    val entitlementState: EntitlementState = EntitlementState.FULL_ACCESS
)

interface SubscriptionRepository {
    fun getSubscriptionDetails(user: User): SubscriptionDetails
    suspend fun launchGooglePlayBillingFlow(): Result<Unit>
    suspend fun restorePurchases(): Result<Unit>
    suspend fun getBillingHistory(): List<Map<String, String>>
}

class DemoSubscriptionRepository : SubscriptionRepository {
    var isSimulatedExpired by mutableStateOf(false)
    var isSimulatedPaymentFailed by mutableStateOf(false)

    override fun getSubscriptionDetails(user: User): SubscriptionDetails {
        if (isSimulatedExpired) {
            return SubscriptionDetails(
                statusText = "Expired",
                isTrial = false,
                trialDaysRemaining = 0,
                renewalDate = "Expired Oct 1, 2026",
                monthlyPriceInr = 299,
                paymentFailed = false,
                isExpired = true,
                entitlementState = EntitlementState.READ_ONLY_EXPIRED
            )
        }
        if (isSimulatedPaymentFailed) {
            return SubscriptionDetails(
                statusText = "Payment failed — update payment",
                isTrial = false,
                trialDaysRemaining = 0,
                renewalDate = "Grace Period (7 days left)",
                monthlyPriceInr = 299,
                paymentFailed = true,
                isExpired = false,
                entitlementState = EntitlementState.FULL_ACCESS
            )
        }
        return SubscriptionDetails(
            statusText = "Free trial · ${user.trialDaysRemaining} days left",
            isTrial = true,
            trialDaysRemaining = user.trialDaysRemaining,
            renewalDate = user.nextBillingDate,
            monthlyPriceInr = user.monthlyPriceInr,
            paymentFailed = false,
            isExpired = false,
            entitlementState = EntitlementState.FULL_ACCESS
        )
    }

    override suspend fun launchGooglePlayBillingFlow(): Result<Unit> {
        // TODO: Call Google Play Billing Library / StoreKit 2 for "focal_family_monthly"
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<Unit> {
        // TODO: Verify purchases with server Cloud Function
        return Result.success(Unit)
    }

    override suspend fun getBillingHistory(): List<Map<String, String>> {
        return listOf(
            mapOf("date" to "2026-09-18", "description" to "3-Month Free Trial Started", "amount" to "₹0"),
            mapOf("date" to "2026-12-18", "description" to "FOCAL Premium Family (Upcoming)", "amount" to "₹299")
        )
    }

    companion object {
        val instance: DemoSubscriptionRepository by lazy { DemoSubscriptionRepository() }
    }
}
