package com.example.myapplication.service

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.focal.app.engine.FocusEngine

class BlockActivity : ComponentActivity() {

    private val MOTIVATIONAL_QUOTES = listOf(
        "Small focus now, big wins later.",
        "Stay in the zone. You've got this.",
        "One task at a time.",
        "Distractions are temporary. Focus is forever.",
        "Great things take uninterrupted time."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val title = intent.getStringExtra("EXTRA_TITLE") ?: intent.getStringExtra("EXTRA_TARGET_NAME") ?: "App is blocked"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: intent.getStringExtra("EXTRA_POLICY_NAME") ?: "Protected by FOCAL · ask a parent"
        val packageName = intent.getStringExtra("EXTRA_PACKAGE_NAME")

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

        val appImageView = ImageView(this).apply {
            val iconSize = (56 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER

            var appIcon: android.graphics.drawable.Drawable? = null
            if (!packageName.isNullOrBlank()) {
                try {
                    val d = packageManager.getApplicationIcon(packageName).mutate()
                    val matrix = ColorMatrix().apply { setSaturation(0f) }
                    d.colorFilter = ColorMatrixColorFilter(matrix)
                    appIcon = d
                } catch (_: Exception) {}
            }

            if (appIcon != null) {
                setImageDrawable(appIcon)
            } else {
                setImageDrawable(ContextCompat.getDrawable(this@BlockActivity, R.drawable.ic_globe))
            }
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
        val titleTextView = TextView(this).apply {
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

        // Subtitle
        val subtitleTextView = TextView(this).apply {
            text = subtitle
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

        val quoteTextView = TextView(this).apply {
            text = MOTIVATIONAL_QUOTES.first()
            setTextColor(Color.WHITE)
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        motivationCard.addView(quoteTextView)

        val statsTextView = TextView(this).apply {
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
                try {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                } catch (_: Exception) {}
                finish()
            }
        }
        buttonsContainer.addView(backButton)
        contentLayout.addView(buttonsContainer)
        rootLayout.addView(contentLayout)

        setContentView(rootLayout)
    }

    override fun onBackPressed() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {}
        finish()
    }
}
