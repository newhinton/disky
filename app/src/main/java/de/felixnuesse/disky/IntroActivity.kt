package de.felixnuesse.disky

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import de.felixnuesse.disky.ui.appintro.IdentifiableAppIntroFragment
import de.felixnuesse.disky.ui.appintro.SlideLeaveInterface
import de.felixnuesse.disky.utils.PermissionManager

class IntroActivity : AppIntro(), SlideLeaveInterface {

    companion object {
        const val INTRO_PREFERENCES = "IntroPreferences"
        const val intro_v1_0_0_completed = "intro_v1_0_0_completed"

        private const val SLIDE_ID_WELCOME = "SLIDE_ID_WELCOME"
        private const val SLIDE_ID_STORAGE = "SLIDE_ID_STORAGE"
        private const val SLIDE_ID_NOTIFICATIONS = "SLIDE_ID_NOTIFICATIONS"
        private const val SLIDE_ID_USAGEACCESS = "SLIDE_ID_USAGEACCESS"
        private const val SLIDE_ID_SUCCESS = "SLIDE_ID_SUCCESS"
    }

    private var mPermissions = PermissionManager(this)
    private var color = R.color.intro_color1

    private var notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        mPermissions.registerInitialRequestNotificationPermission(this)
    } else {
        null
    }

    private var mNotificationsRequested = false


    override fun onResume() {
        enableEdgeToEdge()
        super.onResume()
        setImmersiveMode()
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        setImmersiveMode()
        showStatusBar(true)
        isWizardMode = true
        isColorTransitionsEnabled = true

        // dont allow the intro to be bypassed
        isSystemBackButtonLocked = true

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(
            IdentifiableAppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_welcome_title),
                description = getString(R.string.intro_slide_welcome_description),
                imageDrawable = R.drawable.undraw_the_search,
                backgroundColorRes = color,
                id = SLIDE_ID_WELCOME,
                callback = this
            ))
        switchColor()

        if(!mPermissions.grantedStorage()) {
            addSlide(
                IdentifiableAppIntroFragment.createInstance(
                    title = getString(R.string.intro_storage_title),
                    description = getString(R.string.intro_storage_description),
                    backgroundColorRes = color,
                    imageDrawable = R.drawable.undraw_unlock,
                    id = SLIDE_ID_STORAGE,
                    callback = this
                ))
            switchColor()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !mPermissions.grantedNotifications()) {
            addSlide(
                IdentifiableAppIntroFragment.createInstance(
                    title = getString(R.string.intro_slide_notification_title),
                    description = getString(R.string.intro_slide_notification_description),
                    imageDrawable = R.drawable.undraw_push_notifications,
                    backgroundColorRes = color,
                    id = SLIDE_ID_NOTIFICATIONS,
                    callback = this
                ))
            switchColor()
        }

        if (!mPermissions.grantedUsageStats()) {
            addSlide(
                IdentifiableAppIntroFragment.createInstance(
                    title = getString(R.string.intro_slide_usage_title),
                    description = getString(R.string.intro_slide_usage_description),
                    imageDrawable = R.drawable.undraw_push_notifications,
                    backgroundColorRes = color,
                    id = SLIDE_ID_USAGEACCESS,
                    callback = this
                ))
            switchColor()
        }

        addSlide(
            IdentifiableAppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_done_title),
                description = getString(R.string.intro_slide_done_description),
                backgroundColorRes = color,
                imageDrawable = R.drawable.undraw_verified,
                id = SLIDE_ID_SUCCESS,
                callback = this
            ))
        switchColor()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        endIntro()
    }


    private fun endIntro() {
        val sharedPref = applicationContext.getSharedPreferences(INTRO_PREFERENCES, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean(intro_v1_0_0_completed, true)
            apply()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun allowSlideLeave(id: String): Boolean {
        return when(id) {
            SLIDE_ID_STORAGE -> mPermissions.grantedStorage()
            SLIDE_ID_USAGEACCESS -> mPermissions.grantedUsageStats()
            SLIDE_ID_NOTIFICATIONS -> mNotificationsRequested
            else -> true
        }
    }

    @SuppressLint("InlinedApi") // If the permission is not reqired, notificationPermission is null anyway.
    override fun onSlideLeavePrevented(id: String) {
        when(id) {
            SLIDE_ID_STORAGE -> mPermissions.requestStorage(this)
            SLIDE_ID_USAGEACCESS -> mPermissions.requestUsageStats(this)
            SLIDE_ID_NOTIFICATIONS -> {
                notificationPermission?.launch(Manifest.permission.POST_NOTIFICATIONS)
                mNotificationsRequested = true
            }
            else -> {}
        }
    }

    private fun switchColor() {
        if(color == R.color.intro_color1) {
            color = R.color.intro_color2
        } else {
            color = R.color.intro_color1
        }
    }
}