package com.example.myapplication.service

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.ProtectionDecision

object BlockOverlayManager {

    private val MOTIVATIONAL_QUOTES = listOf(
        "Small focus now, big wins later.",
        "Stay in the zone. You've got this.",
        "One task at a time.",
        "Distractions are temporary. Focus is forever.",
        "Great things take uninterrupted time."
    )
    private var quoteIndex = 0

    private var overlayView: View? = null
    private var titleTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var quoteTextView: TextView? = null
    private var statsTextView: TextView? = null

    fun showOverlay(
        context: Context,
        title: String,
        subtitle: String,
        decision: ProtectionDecision,
        onBackToFocusClick: () -> Unit,
        onParentUnlockClick: (() -> Unit)? = null
    ) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val isFocusActive = FocusEngine.instance.isFocusActive
        val blockedCount = FocusEngine.instance.blockedAttemptsCount

        // Single Overlay Instance — update content if already showing
        if (overlayView != null && titleTextView != null && subtitleTextView != null) {
            titleTextView?.text = title
            subtitleTextView?.text = subtitle
            quoteTextView?.text = MOTIVATIONAL_QUOTES[quoteIndex % MOTIVATIONAL_QUOTES.size]
            if (isFocusActive) {
                statsTextView?.visibility = View.VISIBLE
                statsTextView?.text = "Blocked $blockedCount times this session"
            } else {
                statsTextView?.visibility = View.GONE
            }
            return
        }

        quoteIndex++
        val selectedQuote = MOTIVATIONAL_QUOTES[quoteIndex % MOTIVATIONAL_QUOTES.size]

        val wmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
        }

        val density = context.resources.displayMetrics.density

        // Root container: Fullscreen Black (#000000)
        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f // Start transparent for 180ms fade-in
        }

        // Main content vertical column
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((24 * density).toInt(), (60 * density).toInt(), (24 * density).toInt(), (32 * density).toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Center section containing icon, titles, and motivation card
        val centerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        // Spacer to vertically balance
        val topSpacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.2f
            )
        }
        centerContainer.addView(topSpacer)

        // -------------------------------------------------------------
        // Icon illustration with radial glow and centered thin ring
        // -------------------------------------------------------------
        val illustrationSize = (180 * density).toInt()
        val iconTileSize = (104 * density).toInt()

        val illustrationFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(illustrationSize, illustrationSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        // Custom view to draw the radial glow and 180dp thin white 12% ring
        val ringGlowView = object : View(context) {
            private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                color = Color.argb(31, 255, 255, 255) // White 12% alpha
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val radius = (width - ringPaint.strokeWidth) / 2f

                // Soft white radial gradient glow
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cx, cy, radius,
                        Color.argb(45, 255, 255, 255),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, radius, glowPaint)

                // Thin ring centered on icon (does NOT cross title text)
                canvas.drawCircle(cx, cy, radius, ringPaint)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(illustrationSize, illustrationSize)
        }
        illustrationFrame.addView(ringGlowView)

        // 104dp rounded-square tile (radius 30dp, #0E0E0E, #2A2A2A border)
        val iconTile = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconTileSize, iconTileSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30 * density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * density).toInt(), Color.parseColor("#2A2A2A"))
            }
        }

        // Inside tile: real app icon in grayscale, or globe, or settings gear
        val appImageView = ImageView(context).apply {
            val iconSize = (56 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER

            val grayscaleAppDrawable = getAppIconGrayscale(context, decision)
            if (grayscaleAppDrawable != null) {
                setImageDrawable(grayscaleAppDrawable)
            } else {
                when (decision) {
                    is ProtectionDecision.BlockWebsite -> {
                        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_globe))
                    }
                    is ProtectionDecision.LockSettings, is ProtectionDecision.BlockUninstall -> {
                        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_settings))
                    }
                    else -> {
                        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_globe))
                    }
                }
            }
        }
        iconTile.addView(appImageView)

        // 34dp white circle badge at bottom-right corner (3dp black border, black lock glyph)
        val lockBadgeSize = (34 * density).toInt()
        val lockBadge = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(lockBadgeSize, lockBadgeSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke((3 * density).toInt(), Color.BLACK)
            }

            val lockIconView = ImageView(context).apply {
                val glyphSize = (16 * density).toInt()
                layoutParams = FrameLayout.LayoutParams(glyphSize, glyphSize).apply {
                    gravity = Gravity.CENTER
                }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_lock_badge))
            }
            addView(lockIconView)
        }
        iconTile.addView(lockBadge)

        illustrationFrame.addView(iconTile)
        centerContainer.addView(illustrationFrame)

        // -------------------------------------------------------------
        // Title (30sp bold, one line, ellipsize if long)
        // -------------------------------------------------------------
        titleTextView = TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding((12 * density).toInt(), (28 * density).toInt(), (12 * density).toInt(), 0)
        }
        centerContainer.addView(titleTextView)

        // -------------------------------------------------------------
        // Subtitle ("{policy} · {min} min left" or "Protected by FOCAL · ask a parent")
        // -------------------------------------------------------------
        subtitleTextView = TextView(context).apply {
            text = subtitle
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 15f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, (6 * density).toInt(), 0, (32 * density).toInt())
        }
        centerContainer.addView(subtitleTextView)

        // -------------------------------------------------------------
        // Motivational GlassCard (radius 20dp, #0E0E0E, #262626 border)
        // -------------------------------------------------------------
        val motivationCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * density).toInt(), (18 * density).toInt(), (24 * density).toInt(), (18 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * density).toInt(), Color.parseColor("#262626"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins((12 * density).toInt(), 0, (12 * density).toInt(), (20 * density).toInt())
            }
        }

        quoteTextView = TextView(context).apply {
            text = selectedQuote
            setTextColor(Color.WHITE)
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        motivationCard.addView(quoteTextView)

        statsTextView = TextView(context).apply {
            text = "Blocked $blockedCount times this session"
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(0, (6 * density).toInt(), 0, 0)
            visibility = if (isFocusActive) View.VISIBLE else View.GONE
        }
        motivationCard.addView(statsTextView)
        centerContainer.addView(motivationCard)

        // Bottom spacer to balance center alignment
        val bottomSpacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.2f
            )
        }
        centerContainer.addView(bottomSpacer)

        contentLayout.addView(centerContainer)

        // -------------------------------------------------------------
        // Bottom Buttons: Optional SecondaryButton + PrimaryButton
        // -------------------------------------------------------------
        val buttonsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (12 * density).toInt())
            }
        }

        if (onParentUnlockClick != null) {
            val secondaryBtn = Button(context).apply {
                text = "Parent unlock"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 26 * density
                    setColor(Color.TRANSPARENT)
                    setStroke((1 * density).toInt(), Color.parseColor("#3A3A3A"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (52 * density).toInt()
                ).apply {
                    setMargins((8 * density).toInt(), 0, (8 * density).toInt(), (12 * density).toInt())
                }
                setOnClickListener {
                    try {
                        val appIntent = Intent(context, com.example.myapplication.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("open", "child_home")
                        }
                        context.startActivity(appIntent)
                    } catch (_: Exception) {}
                    dismissOverlay(context)
                    onParentUnlockClick()
                }
            }
            buttonsContainer.addView(secondaryBtn)
        }

        // PrimaryButton "Back to focus" (or "Go back")
        val primaryBtnText = if (isFocusActive) "Back to focus" else "Go back"
        val primaryButton = Button(context).apply {
            text = primaryBtnText
            setTextColor(Color.BLACK)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 27 * density
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setColors(intArrayOf(Color.WHITE, Color.parseColor("#DEDEDE")))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (54 * density).toInt()
            ).apply {
                setMargins((8 * density).toInt(), 0, (8 * density).toInt(), 0)
            }
            setOnClickListener {
                try {
                    val appIntent = Intent(context, com.example.myapplication.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open", "child_home")
                    }
                    context.startActivity(appIntent)
                } catch (_: Exception) {}
                dismissOverlay(context)
                onBackToFocusClick()
            }
        }
        buttonsContainer.addView(primaryButton)

        contentLayout.addView(buttonsContainer)
        rootLayout.addView(contentLayout)

        try {
            windowManager.addView(rootLayout, wmParams)
            overlayView = rootLayout

            // Fade in: exactly 180ms
            ObjectAnimator.ofFloat(rootLayout, View.ALPHA, 0f, 1f).apply {
                duration = 180
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("BlockOverlayManager", "Failed to add overlay window: ${e.message}", e)
        }
    }

    private fun getAppIconGrayscale(context: Context, decision: ProtectionDecision): Drawable? {
        val packageName = when (decision) {
            is ProtectionDecision.BlockApp -> decision.packageName
            else -> null
        } ?: return null

        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName).mutate()
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            drawable.colorFilter = ColorMatrixColorFilter(matrix)
            drawable
        } catch (_: Exception) {
            null
        }
    }

    fun isOverlayShowing(): Boolean = overlayView != null

    fun dismissOverlay(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val currentView = overlayView ?: return
        overlayView = null
        titleTextView = null
        subtitleTextView = null
        quoteTextView = null
        statsTextView = null

        try {
            ObjectAnimator.ofFloat(currentView, View.ALPHA, currentView.alpha, 0f).apply {
                duration = 150
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        try {
                            windowManager.removeView(currentView)
                        } catch (_: Exception) {}
                    }
                })
                start()
            }
        } catch (_: Exception) {
            try {
                windowManager.removeView(currentView)
            } catch (_: Exception) {}
        }
    }
}
