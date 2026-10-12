package com.example.myapplication.service

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.focal.app.engine.FocusEngine

class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val title = intent.getStringExtra("EXTRA_TITLE") ?: intent.getStringExtra("EXTRA_TARGET_NAME") ?: "App is blocked"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: intent.getStringExtra("EXTRA_POLICY_NAME") ?: "Focus Session Active"

        val rootLayout = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setColors(intArrayOf(Color.parseColor("#141414"), Color.parseColor("#000000")))
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            setPadding(60, 120, 60, 100)
        }

        val iconTileSize = (96 * resources.displayMetrics.density).toInt()
        val iconTile = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconTileSize, iconTileSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, 48)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28 * resources.displayMetrics.density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#2A2A2A"))
            }
        }

        val lockBadge = TextView(this).apply {
            text = "🔒"
            textSize = 36f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        iconTile.addView(lockBadge)
        contentLayout.addView(iconTile)

        val titleTextView = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        contentLayout.addView(titleTextView)

        val subtitleTextView = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        contentLayout.addView(subtitleTextView)

        val motivationCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * resources.displayMetrics.density
                setColor(Color.parseColor("#0E0E0E"))
                setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#2A2A2A"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 60)
            }
        }

        val quoteText = TextView(this).apply {
            text = "Small focus now, big wins later."
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        motivationCard.addView(quoteText)

        val blockedCount = FocusEngine.instance.blockedAttemptsCount
        val statsTextView = TextView(this).apply {
            text = "Blocked $blockedCount times this session"
            setTextColor(Color.parseColor("#8A8A8A"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        motivationCard.addView(statsTextView)
        contentLayout.addView(motivationCard)

        val buttonHeight = (52 * resources.displayMetrics.density).toInt()
        val backButton = Button(this).apply {
            text = "Back to focus"
            setTextColor(Color.BLACK)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * resources.displayMetrics.density
                setColor(Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeight
            ).apply {
                setMargins(20, 0, 20, 0)
            }
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        contentLayout.addView(backButton)

        rootLayout.addView(contentLayout)
        setContentView(rootLayout)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
