package com.focal.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalRepository
import com.focal.app.model.UserRole
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*
import kotlinx.coroutines.delay

// -------------------------------------------------------------
// 1. SPLASH SCREEN (Centered logo + soft white radial glow + thin progress line)
// -------------------------------------------------------------
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val steps = 20
        for (i in 1..steps) {
            progress = i / steps.toFloat()
            delay(60)
        }
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x05FFFFFF), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.55f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                FocalAppLogo(modifier = Modifier.size(72.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "FOCAL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Physical Focus & Digital Boundaries",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocalColors.TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Thin progress line
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(FocalColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ONBOARDING SCREEN (3 Slides, Skip top-right, dots, round next button)
// -------------------------------------------------------------
@Composable
fun OnboardingFlowScreen(
    onOnboardingFinished: () -> Unit,
    onSkip: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val slides = listOf(
        OnboardingSlideData(
            badge = "HARDWARE FOCUS",
            title = "Focus, Made Physical",
            description = "Tap your FOCAL card or scan a QR code to instantly start a distraction-free session.",
            icon = FocalIcons.Nfc
        ),
        OnboardingSlideData(
            badge = "ANALYTICS",
            title = "Understand your screen time",
            description = "Track daily focus hours, screen time metrics, and saved productive time.",
            icon = FocalIcons.BarChart
        ),
        OnboardingSlideData(
            badge = "PROTECTION",
            title = "Focus without distractions",
            description = "Custom rules block social media, mobile games, and websites during focus hours.",
            icon = FocalIcons.Shield
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        // Top Bar (Skip button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOCAL",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            )

            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .clickable { onSkip() }
                    .padding(8.dp)
            )
        }

        // Slide Content
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                (fadeIn(tween(250)) + slideInVertically(tween(250)) { 20 })
                    .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(200)) { -20 })
            },
            modifier = Modifier.align(Alignment.Center)
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration Card Box
                FocalCard(
                    modifier = Modifier.size(140.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                StatusBadge(
                    text = slide.badge,
                    backgroundColor = FocalColors.Pressed,
                    textColor = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Nav (Dots + Round Next Button / Get Started)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocalPageIndicator(
                pageCount = slides.size,
                currentPage = currentPage
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (currentPage < slides.lastIndex) {
                // Round White Next Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { currentPage++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.ArrowForward,
                        contentDescription = "Next Slide",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                PrimaryButton(
                    text = "Get Started",
                    onClick = onOnboardingFinished,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class OnboardingSlideData(
    val badge: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// -------------------------------------------------------------
// 3. ROLE SELECTION SCREEN ("Who is using FOCAL?")
// -------------------------------------------------------------
@Composable
fun RoleSelectionScreen(
    onRoleConfirmed: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Who is using FOCAL?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select your role to configure device setup.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card 1: Parent / Controller
            ActionCard(
                isSelected = selectedRole == UserRole.CONTROLLER,
                onClick = { selectedRole = UserRole.CONTROLLER },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FocalColors.Pressed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I'm a Parent / Controller",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Set focus policies, manage children, and pair devices.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocalColors.TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Child
            ActionCard(
                isSelected = selectedRole == UserRole.CHILD,
                onClick = { selectedRole = UserRole.CHILD },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FocalColors.Pressed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Users,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I'm a Child — pair this phone",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pair with parent's phone to enable managed focus mode.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocalColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        PrimaryButton(
            text = "Continue",
            enabled = selectedRole != null,
            onClick = { selectedRole?.let { onRoleConfirmed(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

// -------------------------------------------------------------
// 4. PARENT LOGIN SCREEN
// -------------------------------------------------------------
@Composable
fun ParentLoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var email by remember { mutableStateOf("priya@focal.io") }
    var password by remember { mutableStateOf("••••••••") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            FocalAppLogo(modifier = Modifier.size(80.dp))

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Parent Sign In",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in to manage your family's focus policies.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            FocalTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "parent@focal.io",
                icon = FocalIcons.Smartphone
            )

            Spacer(modifier = Modifier.height(14.dp))

            FocalTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                icon = FocalIcons.Lock
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onNavigateToForgotPassword() }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Sign In",
                onClick = {
                    repository.switchUserRole(UserRole.CONTROLLER)
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. PARENT CREATE ACCOUNT SCREEN (Name, Email, Phone, Password)
// -------------------------------------------------------------
@Composable
fun ParentSignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Parent Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Set up your account to start managing family focus.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            FocalTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                placeholder = "Priya Sharma"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FocalTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "priya@focal.io"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FocalTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                placeholder = "+91 98765 43210"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FocalTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Create Account",
                onClick = {
                    repository.switchUserRole(UserRole.CONTROLLER)
                    if (name.isNotBlank()) {
                        repository.currentUser = repository.currentUser.copy(
                            name = name,
                            email = if (email.isNotBlank()) email else "parent@focal.io",
                            phone = if (phone.isNotBlank()) phone else "+91 98765 43210"
                        )
                    }
                    onSignUpSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 6. PARENT FORGOT PASSWORD SCREEN (+ Check your inbox success state)
// -------------------------------------------------------------
@Composable
fun ParentForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (submitted) "Instructions sent! Check your email inbox." else "Enter your email address to receive password reset instructions.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!submitted) {
                FocalTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "parent@focal.io"
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Send Reset Link",
                    onClick = { if (email.isNotBlank()) submitted = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SuccessState(
                    title = "Check your inbox",
                    message = "We sent password reset instructions to ${if (email.isNotBlank()) email else "your email"}.",
                    actionText = "Back to Sign In",
                    onActionClick = onBackToLogin
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!submitted) {
                SecondaryButton(
                    text = "Back to Sign In",
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 7. PARENT ADD CHILD SCREEN & EMPTY STATE
// -------------------------------------------------------------
@Composable
fun ParentAddChildScreen(
    onChildAdded: (String) -> Unit
) {
    var childName by remember { mutableStateOf("") }
    var childAge by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Add Child Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter child details to generate a device pairing code.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            FocalTextField(
                value = childName,
                onValueChange = { childName = it },
                label = "Child Name",
                placeholder = "e.g. Ananya"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FocalTextField(
                value = childAge,
                onValueChange = { childAge = it },
                label = "Child Age",
                placeholder = "e.g. 12"
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Create & Generate Pairing Code",
                onClick = {
                    onChildAdded(if (childName.isNotBlank()) childName else "Ananya")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// 8. PARENT PAIR CHILD PHONE SCREEN (Displays 6-Digit Code & QR + "Waiting for device…")
// -------------------------------------------------------------
@Composable
fun ParentPairChildScreen(
    childName: String,
    onPairingComplete: () -> Unit
) {
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (com.focal.app.config.FocalConfig.isDemoMode) {
            delay(4000)
            isConnected = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StatusBadge(
                text = if (isConnected) "CONNECTED" else "PAIRING IN PROGRESS",
                badgeType = if (isConnected) BadgeType.ACTIVE else BadgeType.PENDING
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pair $childName's Phone",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isConnected) "$childName's phone connected successfully!" else "Open FOCAL on $childName's phone and scan this QR code or enter the 6-digit code.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!isConnected) {
                // 6-Digit Code Box
                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 20.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "6-DIGIT PAIRING CODE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                color = FocalColors.TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "839 204",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code Display Card
                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 24.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        QrCodeView(qrContent = "FOCAL_PAIR_CHILD_$childName")
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Waiting for device…",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                            )
                        }
                    }
                }
            } else {
                SuccessState(
                    title = "$childName's phone connected",
                    message = "Device pairing complete. You can now enforce focus policies and schedules.",
                    actionText = "Go to Dashboard",
                    onActionClick = onPairingComplete
                )
            }
        }

        if (!isConnected && com.focal.app.config.FocalConfig.allowSameDevicePairing) {
            PrimaryButton(
                text = "Pair this phone (demo)",
                onClick = {
                    val repo = com.focal.app.data.FocalRepository.instance
                    if (repo.childrenList.isNotEmpty()) {
                        repo.childrenList[0] = repo.childrenList[0].copy(isOnline = true, deviceName = "Ananya's Phone")
                    }
                    isConnected = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

// -------------------------------------------------------------
// 9. CHILD PAIRING SCREEN (Camera Scanner + 6-digit Code Entry Boxes)
// -------------------------------------------------------------
@Composable
fun ChildPairingScreen(
    onPairingSuccess: () -> Unit
) {
    var pairingCodeDigits by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    var useCodeEntry by remember { mutableStateOf(false) }

    val isCodeComplete = pairingCodeDigits.all { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pair with Parent",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Scan the QR code from your parent's phone or enter the 6-digit pairing code.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!useCodeEntry) {
                // QR Camera Scanner Card Visual
                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 24.dp,
                    onClick = onPairingSuccess
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.QrCode,
                                contentDescription = "Scan QR",
                                tint = Color.Black,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Point camera at parent's QR code",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(Tap QR box to simulate successful scan)",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 11.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SecondaryButton(
                    text = "Enter code instead",
                    onClick = { useCodeEntry = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 6 Separate Square Digit Boxes
                Text(
                    text = "ENTER 6-DIGIT CODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = FocalColors.TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val digit = pairingCodeDigits.getOrElse(i) { "" }
                        OutlinedTextField(
                            value = digit,
                            onValueChange = { input ->
                                val clean = input.takeLast(1)
                                val updated = pairingCodeDigits.toMutableList()
                                updated[i] = clean
                                pairingCodeDigits = updated
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = FocalColors.Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = "Confirm & Pair Phone",
                    enabled = isCodeComplete,
                    onClick = onPairingSuccess,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryButton(
                    text = "Use Camera QR Scanner Instead",
                    onClick = { useCodeEntry = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 10. CARD SETUP SCREEN (NFC Card pairing, "Skip — use QR for now")
// -------------------------------------------------------------
@Composable
fun CardSetupScreen(
    onSetupFinished: () -> Unit,
    onSkip: () -> Unit
) {
    var showAddCardModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Text(
            text = "Skip — use QR for now",
            style = MaterialTheme.typography.labelSmall.copy(
                color = FocalColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { onSkip() }
                .padding(8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Have a FOCAL card?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Register a physical NFC card to your account and assign it to a child for one-tap focus sessions.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            NfcPulseAnimation(detected = false)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Hardware cards enable physical focus triggers without screen interaction.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocalColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            PrimaryButton(
                text = "Add Card",
                onClick = { showAddCardModal = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryButton(
                text = "Skip — use QR for now",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAddCardModal) {
        AddCardFlowModal(
            onDismiss = { showAddCardModal = false },
            onSuccess = { onSetupFinished() }
        )
    }
}
