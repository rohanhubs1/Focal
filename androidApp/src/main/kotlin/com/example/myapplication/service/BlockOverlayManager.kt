package com.example.myapplication.service

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.ProtectionDecision

object BlockOverlayManager {

    private var overlayView: View? = null
    private var titleTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var statsTextView: TextView? = null

    fun showOverlay(
        context: Context,
        title: String,
        subtitle: String,
        decision: ProtectionDecision,
        onBackToFocusClick: () -> Unit
    ) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        // Single Overlay Instance — update content if already showing
        if (overlayView != null && titleTextView != null && subtitleTextView != null) {
            titleTextView?.text = title
            subtitleTextView?.text = subtitle
            val blockedCount = FocusEngine.instance.blockedAttemptsCount
            statsTextView?.text = "Blocked $blockedCount times this session"
            return
        }

        val layoutParams = WindowManager.LayoutParams(
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

        // Root container: Fullscreen Black (#000000) with subtle white radial glow
        val rootLayout = FrameLayout(context).apply {
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setColors(intArrayOf(Color.parseColor("#141414"), Color.parseColor("#000000")))
            }
            background = bgDrawable
            alpha = 0f // Start transparent for 150ms fade-in
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            setPadding(60, 120, 60, 100)
        }

        // Icon Tile: 96dp (#0E0E0E, 28dp radius, 1dp #2A2A2A border)
        val iconTileSize = (96 * context.resources.displayMetrics.density).toInt()
        val tileParams = LinearLayout.LayoutParams(iconTileSize, iconTileSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setMargins(0, 0, 0, 48)
        }
        val iconTile = FrameLayout(context).apply {
            this.layoutParams = tileParams
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28 * context.resources.displayMetrics.density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * context.resources.displayMetrics.density).toInt(), Color.parseColor("#2A2A2A"))
            }
        }

        val badgeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        val lockBadge = TextView(context).apply {
            text = "🔒"
            textSize = 36f
            gravity = Gravity.CENTER
            this.layoutParams = badgeParams
        }
        iconTile.addView(lockBadge)
        contentLayout.addView(iconTile)

        // Title 28sp bold white (NO RED!)
        titleTextView = TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        contentLayout.addView(titleTextView)

        // Subtitle 15sp #8A8A8A
        subtitleTextView = TextView(context).apply {
            text = subtitle
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        contentLayout.addView(subtitleTextView)

        // Card #0E0E0E 16dp radius, 1dp #2A2A2A border
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 60)
        }
        val motivationCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * context.resources.displayMetrics.density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * context.resources.displayMetrics.density).toInt(), Color.parseColor("#2A2A2A"))
            }
            this.layoutParams = cardParams
        }

        val quoteText = TextView(context).apply {
            text = "Small focus now, big wins later."
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        motivationCard.addView(quoteText)

        val blockedCount = FocusEngine.instance.blockedAttemptsCount
        statsTextView = TextView(context).apply {
            text = "Blocked $blockedCount times this session"
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        motivationCard.addView(statsTextView)
        contentLayout.addView(motivationCard)

        // White button "Back to focus" (52dp height, 14dp radius, 20dp side margins)
        val buttonHeight = (52 * context.resources.displayMetrics.density).toInt()
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            buttonHeight
        ).apply {
            setMargins(20, 0, 20, 0)
        }
        val backButton = Button(context).apply {
            text = "Back to focus"
            setTextColor(Color.BLACK)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * context.resources.displayMetrics.density
                setColor(Color.WHITE)
            }
            this.layoutParams = btnParams
            setOnClickListener {
                try {
                    android.widget.Toast.makeText(context.applicationContext, "Stay focused", android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
                dismissOverlay(context)
                onBackToFocusClick()
            }
        }
        contentLayout.addView(backButton)

        rootLayout.addView(contentLayout)

        try {
            windowManager.addView(rootLayout, layoutParams)
            overlayView = rootLayout

            // Fast 80ms smooth fade-in animation for instant <100ms display
            ObjectAnimator.ofFloat(rootLayout, View.ALPHA, 0f, 1f).apply {
                duration = 80
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("BlockOverlayManager", "Failed to add overlay window: ${e.message}", e)
        }
    }

    fun isOverlayShowing(): Boolean = overlayView != null

    fun dismissOverlay(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val currentView = overlayView ?: return
        overlayView = null
        titleTextView = null
        subtitleTextView = null
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
