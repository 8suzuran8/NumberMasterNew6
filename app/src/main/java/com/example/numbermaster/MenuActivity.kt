package com.example.numbermaster

import android.animation.*
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.core.view.*
import com.example.numbermaster.databinding.ActivityMenuBinding
import kotlin.concurrent.timer

class MenuActivity : NumberMasterActivity() {
    private var activityIntents: MutableMap<Int, Intent?> = mutableMapOf(
        R.id.menu_start_button to null,
        R.id.menu_quest_button to null,
        R.id.menu_input_button to null,
        R.id.menu_ranking_button to null,
        R.id.menu_howto_button to null,
        R.id.menu_story_button to null,
        R.id.menu_thanks_hint_button to null,
    )

    private var initialed = false
    private var menuSize: Int? = null
    private var menuY: Float? = null

    // prevボタンで戻った時には実行されない
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base)

        this.activityIntents = mutableMapOf(
            R.id.menu_start_button to Intent(application, GameActivity::class.java),
            R.id.menu_input_button to Intent(application, InputActivity::class.java),
            R.id.menu_ranking_button to Intent(application, RankingActivity::class.java),
            R.id.menu_howto_button to Intent(application, HowtoActivity::class.java),
            R.id.menu_story_button to Intent(application, StoryActivity::class.java),
            R.id.menu_thanks_hint_button to Intent(application, ThxActivity::class.java),
        )

        this.convertIntentExtraToGlobalActivityInfo()
        for (key in this.activityIntents.keys) {
            this.convertGlobalActivityInfoToIntentExtra(this.activityIntents[key]!!)
        }

        val that = this
        this.menuSize = (this.globalActivityInfo["meta:rootLayoutShort"]!!.toFloat() - (this.globalActivityInfo["meta:otherSize"]!!.toFloat() * 4)).toInt()

        this.menuY = if (this.resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ((this.globalActivityInfo["meta:rootLayoutLong"]!!.toFloat()) / 2) - (this.menuSize!! / 2) - this.globalActivityInfo["meta:otherSize"]!!.toFloat()
        } else {
            0F
        }

        val dbHelper = NumberMasterOpenHelper(this)
        val settings = dbHelper.loadSettings()

        val accessibility : AccessibilityManager = this.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager

        val inflateRootLayout = findViewById<FrameLayout>(R.id.root_layout)
        val activityLayout = layoutInflater.inflate(R.layout.activity_menu, inflateRootLayout)
        val layoutBinding: ActivityMenuBinding = ActivityMenuBinding.bind(activityLayout).apply {
            if (that.resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                titleImage.pivotX = (that.globalActivityInfo["meta:rootLayoutShort"]!!.toFloat() / 2) - that.globalActivityInfo["meta:otherSize"]!!.toFloat()
                titleImage.pivotY = 0F
            } else {
                titleImage.pivotX = 0F
                titleImage.pivotY = (that.globalActivityInfo["meta:rootLayoutShort"]!!.toFloat() / 2) - that.globalActivityInfo["meta:otherSize"]!!.toFloat()
            }
            menuLayout.setPadding(that.globalActivityInfo["boardFrameWidth"]!!.toFloat().toInt())
            menuLayout.layoutParams.width = that.menuSize!!
            menuLayout.layoutParams.height = that.menuSize!!
            menuLayout.translationY = that.menuY!!
            if (!accessibility.isEnabled) {
                titleImage.stateListAnimator =
                    AnimatorInflater.loadStateListAnimator(that, R.xml.animate_menu_title)
                menuLayout.stateListAnimator =
                    AnimatorInflater.loadStateListAnimator(that, R.xml.animate_menu_menu)
            } else {
                titleImage.alpha = 1.0F
                titleImage.scaleX = 0.4F
                titleImage.scaleY = 0.4F
                menuLayout.alpha = 1.0F
            }

            if (settings.containsKey("add_icon_read") && settings["add_icon_read"]!!.toInt() == 0) {
                menuHowtoButton.setImageResource(R.drawable.add_icon)
                val imageMatrix = menuHowtoButton.imageMatrix.apply {
                    val oneMenuSize = that.menuSize!! / 3
                    val position = (oneMenuSize - (oneMenuSize * 0.4)).toFloat()
                    postScale(0.2F, 0.2F)
                    postTranslate(position, position)
                }
                menuHowtoButton.imageMatrix = imageMatrix
            }

            // 1メニューのサイズを1/3にする
            for (rowIndex in 0 until menuLayout.childCount) {
                val rowLayout = menuLayout.getChildAt(rowIndex) as TableRow
                for (imageButtonIndex in 0 until rowLayout.childCount) {
                    rowLayout.getChildAt(imageButtonIndex).apply {
                        layoutParams.height = that.globalActivityInfo["numberPanelSize:1"]!!.toFloat().toInt()
                        layoutParams.width = that.globalActivityInfo["numberPanelSize:1"]!!.toFloat().toInt()
                    }
                }
            }
        }

        val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            setMargins(that.globalActivityInfo["meta:otherSize"]!!.toFloat().toInt())
        }
        addContentView(layoutBinding.rootLayout, layoutParams)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val that = this

        this.menuY = if (this.resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ((this.globalActivityInfo["meta:rootLayoutLong"]!!.toFloat()) / 2) - (that.menuSize!! / 2) - this.globalActivityInfo["meta:otherSize"]!!.toFloat()
        } else {
            0F
        }

        findViewById<TableLayout>(R.id.menu_layout).apply {
            translationY = that.menuY!!.toFloat()
        }

        findViewById<ImageView>(R.id.title_image).apply {
            if (that.resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                pivotX = (that.globalActivityInfo["meta:rootLayoutShort"]!!.toFloat() / 2) - that.globalActivityInfo["meta:otherSize"]!!.toFloat()
                pivotY = 0F
            } else {
                pivotX = 0F
                pivotY = (that.globalActivityInfo["meta:rootLayoutShort"]!!.toFloat() / 2) - that.globalActivityInfo["meta:otherSize"]!!.toFloat()
            }
        }
    }

    fun buttonClickListener(view: View) {
        val menuLayout = findViewById<TableLayout>(R.id.menu_layout)

        // メニューのスケールが変化中なら処理しない
        if (menuLayout.alpha < 1.0) return

        // メニュー内の押した部分をアクティブにし、それ以外を非アクティブにする。
        for (rowIndex in 0 until menuLayout.childCount) {
            val rowLayout = menuLayout.getChildAt(rowIndex) as TableRow
            for (imageButtonIndex in 0 until rowLayout.childCount) {
                val imageButton = rowLayout.getChildAt(imageButtonIndex) as ImageButton
                if (view.id == imageButton.id) {
                    imageButton.setBackgroundResource(+R.drawable.menu_active)
                } else {
                    imageButton.setBackgroundResource(0)
                }
            }
        }

        // 存在しないメニューを押したら、ここで終了
        if (view.id != R.id.menu_quest_button) {
            if (!this.activityIntents.contains(view.id) || this.activityIntents[view.id] == null) {
                if (view.id != R.id.prev_button) {
                    return
                }
            }
        }

        when (view.id) {
            R.id.prev_button -> {
                this.finishAffinity()
                return
            }
            R.id.menu_start_button -> {
                val that = this
                findViewById<TableLayout>(R.id.menu_layout).apply {
                    stateListAnimator = AnimatorInflater.loadStateListAnimator(that, R.xml.animate_menu_togame_menu)
                }

                findViewById<ImageView>(R.id.title_image).apply {
                    stateListAnimator = AnimatorInflater.loadStateListAnimator(that, R.xml.animate_menu_togame_title)
                }

                // 上記animation時間1000 + マージン500
                timer(name = "toGame", initialDelay = 1500, period = 1500) {
                    Handler(Looper.getMainLooper()).post {
                        this.cancel()
                        startActivity(that.activityIntents[view.id])
                    }
                }
                return
            }
            R.id.menu_quest_button -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(this.getString(R.string.quest_link)))
                startActivity(intent)
            }
            else -> {
                startActivity(this.activityIntents[view.id])
                return
            }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return

        val that = this

        if (this.initialed) {
            findViewById<ImageView>(R.id.title_image).apply {
                alpha = 1F
            }
            findViewById<TableLayout>(R.id.menu_layout).apply {
                translationY = that.menuY!!
            }
        } else {
            this.initialed = true
        }
    }
}