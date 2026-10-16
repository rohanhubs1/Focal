package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.ProtectionStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BlockActivity : ComponentActivity() {

    companion object {
        private const val TAG = "BlockActivity"

        var isShowing: Boolean = false
            private set

        var currentBlockedPackage: String? = null
            private set

        var activeInstance: BlockActivity? = null
            private set

        fun closeIfShowing(context: Context) {
            activeInstance?.let { activity ->
                activity.runOnUiThread {
                    activity.navigateToChildHomeAndFinish()
                }
            }
        }
    }

    private val MOTIVATIONAL_QUOTES = listOf(
        "Small focus now, big wins later.",
        "Stay in the zone. You've got this.",
        "One task at a time.",
        "Distractions are temporary. Focus is forever.",
        "Great things take uninterrupted time."
    )

    private var titleTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var statsTextView: TextView? = null
    private var quoteTextView: TextView? = null
    private var appImageView: ImageView? = null
    private var focusMonitorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        isShowing = true
        activeInstance = this

        val title = intent.getStringExtra("EXTRA_TITLE") ?: intent.getStringExtra("EXTRA_TARGET_NAME") ?: "App is blocked"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: intent.getStringExtra("EXTRA_POLICY_NAME") ?: "Protected by FOCAL · ask a parent"
        val packageName = intent.getStringExtra("EXTRA_PACKAGE_NAME")
        val isParentUnlockAllowed = intent.getBooleanExtra("EXTRA_IS_PARENT_UNLOCK_ALLOWED", false)
        currentBlockedPackage = packageName

        val isFocusActive = FocusEngine.instance.isFocusActive
        val blockedCount = FocusEngine.instance.blockedAttemptsCount
        val density = resources.displayMetrics.density

        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((24 * density).toInt(), (60 * density).toInt(), (24 * density).toInt(), (32 * density).toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val centerContainer = LinearLayout(this).apply {
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

        val topSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.2f
            )
        }
        centerContainer.addView(topSpacer)

        // 180dp illustration frame with ring & glow
        val illustrationSize = (180 * density).toInt()
        val iconTileSize = (104 * density).toInt()

        val illustrationFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(illustrationSize, illustrationSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        val ringGlowView = object : View(this) {
            private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                color = Color.argb(31, 255, 255, 255)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val radius = (width - ringPaint.strokeWidth) / 2f

                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cx, cy, radius,
                        Color.argb(45, 255, 255, 255),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, radius, glowPaint)
                canvas.drawCircle(cx, cy, radius, ringPaint)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(illustrationSize, illustrationSize)
        }
        illustrationFrame.addView(ringGlowView)

        // 104dp tile
        val iconTile = FrameLayout(this).apply {
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

        appImageView = ImageView(this).apply {
            val iconSize = (56 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            loadAppIcon(packageName)
        }
        iconTile.addView(appImageView)

        // 34dp white lock badge
        val lockBadgeSize = (34 * density).toInt()
        val lockBadge = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(lockBadgeSize, lockBadgeSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke((3 * density).toInt(), Color.BLACK)
            }

            val lockIconView = ImageView(this@BlockActivity).apply {
                val glyphSize = (16 * density).toInt()
                layoutParams = FrameLayout.LayoutParams(glyphSize, glyphSize).apply {
                    gravity = Gravity.CENTER
                }
                setImageDrawable(ContextCompat.getDrawable(this@BlockActivity, R.drawable.ic_lock_badge))
            }
            addView(lockIconView)
        }
        iconTile.addView(lockBadge)
        illustrationFrame.addView(iconTile)
        centerContainer.addView(illustrationFrame)

        // Title
        titleTextView = TextView(this).apply {
            this.text = title
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding((12 * density).toInt(), (28 * density).toInt(), (12 * density).toInt(), 0)
        }
        centerContainer.addView(titleTextView)

        // Subtitle
        subtitleTextView = TextView(this).apply {
            this.text = subtitle
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 15f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, (6 * density).toInt(), 0, (32 * density).toInt())
        }
        centerContainer.addView(subtitleTextView)

        // Motivation card
        val motivationCard = LinearLayout(this).apply {
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

        quoteTextView = TextView(this).apply {
            text = MOTIVATIONAL_QUOTES.first()
            setTextColor(Color.WHITE)
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        motivationCard.addView(quoteTextView)

        statsTextView = TextView(this).apply {
            text = "Blocked $blockedCount times this session"
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(0, (6 * density).toInt(), 0, 0)
            visibility = if (isFocusActive) View.VISIBLE else View.GONE
        }
        motivationCard.addView(statsTextView)
        centerContainer.addView(motivationCard)

        val bottomSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.2f
            )
        }
        centerContainer.addView(bottomSpacer)

        contentLayout.addView(centerContainer)

        val buttonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (12 * density).toInt())
            }
        }

        if (isParentUnlockAllowed) {
            val parentUnlockButton = Button(this).apply {
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
                    FocalRepository.instance.showParentUnlockScreen = true
                    navigateToChildHomeAndFinish()
                }
            }
            buttonsContainer.addView(parentUnlockButton)
        }

        val primaryBtnText = if (isFocusActive) "Back to focus" else "Go back"
        val backButton = Button(this).apply {
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
                navigateToChildHomeAndFinish()
            }
        }
        buttonsContainer.addView(backButton)
        contentLayout.addView(buttonsContainer)
        rootLayout.addView(contentLayout)

        setContentView(rootLayout)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToChildHomeAndFinish()
            }
        })

        // Real-time flow monitor: if focus ends while block screen is open, close it immediately and open Child Home
        lifecycleScope.launch {
            ProtectionStateRepository.instance.state.collect { pState ->
                if (!pState.focusActive && !isFinishing) {
                    navigateToChildHomeAndFinish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
        activeInstance = this

        if (!FocusEngine.instance.isFocusActive && !isFinishing) {
            navigateToChildHomeAndFinish()
            return
        }

        focusMonitorJob?.cancel()
        focusMonitorJob = lifecycleScope.launch {
            while (isActive) {
                delay(250)
                if (!FocusEngine.instance.isFocusActive && !isFinishing) {
                    navigateToChildHomeAndFinish()
                    break
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        focusMonitorJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        if (activeInstance == this) {
            activeInstance = null
        }
        currentBlockedPackage = null
        focusMonitorJob?.cancel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateContentFromIntent(intent)
    }

    private fun updateContentFromIntent(intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: intent.getStringExtra("EXTRA_TARGET_NAME") ?: "App is blocked"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: intent.getStringExtra("EXTRA_POLICY_NAME") ?: "Protected by FOCAL · ask a parent"
        val pkg = intent.getStringExtra("EXTRA_PACKAGE_NAME")

        currentBlockedPackage = pkg
        titleTextView?.text = title
        subtitleTextView?.text = subtitle

        val isFocusActive = FocusEngine.instance.isFocusActive
        val blockedCount = FocusEngine.instance.blockedAttemptsCount
        if (isFocusActive) {
            statsTextView?.visibility = View.VISIBLE
            statsTextView?.text = "Blocked $blockedCount times this session"
        } else {
            statsTextView?.visibility = View.GONE
        }

        loadAppIcon(pkg)
    }

    private fun loadAppIcon(pkg: String?) {
        var appIcon: android.graphics.drawable.Drawable? = null
        if (!pkg.isNullOrBlank()) {
            try {
                val d = packageManager.getApplicationIcon(pkg).mutate()
                val matrix = ColorMatrix().apply { setSaturation(0f) }
                d.colorFilter = ColorMatrixColorFilter(matrix)
                appIcon = d
            } catch (_: Exception) {}
        }

        if (appIcon != null) {
            appImageView?.setImageDrawable(appIcon)
        } else {
            appImageView?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_globe))
        }
    }

    fun navigateToChildHomeAndFinish() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open", "child_home")
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity with open=child_home: ${e.message}", e)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateToChildHomeAndFinish()
    }
}
