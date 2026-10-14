package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.config.FocalPricingConfig
import com.focal.app.model.PlanType
import com.focal.app.theme.FocalColors

@Composable
fun PaymentDialog(
    planType: PlanType,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var upiId by remember { mutableStateOf("user@okhdfcbank") }
    var shippingAddress by remember { mutableStateOf("104 Lotus Boulevard, Sector 100, Noida, UP - 201304") }
    var isProcessing by remember { mutableStateOf(false) }
    var stepSuccess by remember { mutableStateOf(false) }

    val amount = if (planType == PlanType.NFC) 999 else 0

    Dialog(onDismissRequest = onDismiss) {
        DarkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = FocalColors.CardElevated,
            borderColor = FocalColors.BorderGlow
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = if (planType == PlanType.NFC) "NFC HARDWARE CHECKOUT" else "QR INSTANT ACTIVATION",
                        backgroundColor = FocalColors.PrimaryAccentDark.copy(alpha = 0.3f),
                        textColor = FocalColors.PrimaryAccentLight,
                        dotColor = FocalColors.Success
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = FocalIcons.Close,
                            contentDescription = "Close",
                            tint = FocalColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (stepSuccess) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FocalColors.SuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Check,
                                contentDescription = null,
                                tint = FocalColors.Success,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (planType == PlanType.NFC) "Card Order Placed!" else "Account Activated!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (planType == PlanType.NFC) {
                                "₹999 payment verified. Your NFC Focus Card has been assigned. 3 Months Free Trial is now active!"
                            } else {
                                "Free account verified. 3 Months Free Trial started. No payment required today."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FocalColors.TextSecondary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GradientButton(
                            text = "Go To Dashboard",
                            onClick = onPaymentSuccess,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Order Summary Box
                    DarkCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Card,
                        borderColor = FocalColors.Border
                    ) {
                        Text(
                            text = if (planType == PlanType.NFC) "Physical NFC Focus Card" else "Digital QR Focus System",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (planType == PlanType.NFC) {
                                "Includes physical hardware NFC card + 3 Months Free Trial"
                            } else {
                                "100% Free setup + 3 Months Free Trial"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocalColors.TextSecondary
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = FocalColors.Border
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Initial Cost", color = FocalColors.TextSecondary)
                            Text(
                                text = if (planType == PlanType.NFC) "${FocalPricingConfig.NFC_CARD_PRICE_FORMATTED} (Card Price)" else "₹0 (FREE)",
                                fontWeight = FontWeight.Bold,
                                color = if (planType == PlanType.NFC) Color.White else FocalColors.Success
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Free Trial", color = FocalColors.TextSecondary)
                            Text("${FocalPricingConfig.PLAN_FREE_TRIAL_MONTHS} MONTHS FREE", color = FocalColors.PrimaryAccentLight, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Post Trial Billing", color = FocalColors.TextSecondary)
                            Text(FocalPricingConfig.PLAN_PRICE_PER_MONTH, color = FocalColors.TextSecondary)
                        }
                    }

                    if (planType == PlanType.NFC) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "SHIPPING ADDRESS FOR CARD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                color = FocalColors.TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = shippingAddress,
                            onValueChange = { shippingAddress = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FocalColors.PrimaryAccent,
                                unfocusedBorderColor = FocalColors.Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "SELECT PAYMENT METHOD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                color = FocalColors.TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("UPI", "Card", "Netbanking").forEach { method ->
                                val isSelected = selectedMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FocalColors.PrimaryAccentDark.copy(alpha = 0.4f) else FocalColors.Card)
                                        .border(
                                            1.dp,
                                            if (isSelected) FocalColors.PrimaryAccent else FocalColors.Border,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedMethod = method }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSelected) Color.White else FocalColors.TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedMethod == "UPI") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. mobile@upi", color = FocalColors.TextDisabled) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocalColors.PrimaryAccent,
                                    unfocusedBorderColor = FocalColors.Border,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GradientButton(
                        text = if (planType == PlanType.NFC) "Pay ₹999 & Start Free Trial" else "Start 3 Months Free Trial (₹0)",
                        onClick = {
                            isProcessing = true
                            stepSuccess = true
                            isProcessing = false
                        },
                        isLoading = isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "🔒 256-bit Encrypted. 30-Day Money Back Guarantee.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocalColors.TextMuted,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun NfcCheckoutModal(
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    PaymentDialog(
        planType = PlanType.NFC,
        onDismiss = onDismiss,
        onPaymentSuccess = onPaymentSuccess
    )
}

@Composable
fun NfcPurchaseSuccessModal(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        DarkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = FocalColors.CardElevated,
            borderColor = FocalColors.Border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FocalColors.SuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Check,
                        contentDescription = null,
                        tint = FocalColors.Success,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Card Order Placed!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your NFC Focus Card has been assigned.\n3 Months Premium Included.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Set Up FOCAL",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
