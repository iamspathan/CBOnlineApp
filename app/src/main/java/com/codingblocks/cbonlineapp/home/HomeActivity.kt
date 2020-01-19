package com.codingblocks.cbonlineapp.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.codingblocks.cbonlineapp.AboutActivity
import com.codingblocks.cbonlineapp.BuildConfig
import com.codingblocks.cbonlineapp.CBOnlineApp
import com.codingblocks.cbonlineapp.LoginActivity
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.home.mycourses.MyCoursesFragment
import com.codingblocks.cbonlineapp.jobs.JobsActivity
import com.codingblocks.cbonlineapp.notifications.NotificationsActivity
import com.codingblocks.cbonlineapp.settings.SettingsActivity
import com.codingblocks.cbonlineapp.util.Components
import com.codingblocks.cbonlineapp.util.PreferenceHelper.Companion.ACCESS_TOKEN
import com.codingblocks.cbonlineapp.util.extensions.getPrefs
import com.codingblocks.cbonlineapp.util.extensions.observeOnce
import com.codingblocks.cbonlineapp.util.extensions.observer
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.nav_header_home.view.*
import kotlinx.android.synthetic.main.report_dialog.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.singleTop
import org.jetbrains.anko.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    AnkoLogger, View.OnClickListener, DrawerLayout.DrawerListener {

    private var updateUIReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            invalidateOptionsMenu()
        }
    }
    private var filter = IntentFilter()

    private val viewModel by viewModel<HomeActivityViewModel>()

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewModel.prefs = getPrefs()

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        drawer_layout.addDrawerListener(this)
        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            setUpFragment()
        }
        filter.addAction("com.codingblocks.notification")

        // adding label to nav drawer items
//        nav_view.menu.getItem(3).setActionView(R.layout.menu_new)
    }

    private fun setUpFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        if (viewModel.prefs.SP_ACCESS_TOKEN_KEY != ACCESS_TOKEN) {
            // Update User Token on Login
//            if (JWTUtils.isExpired(viewModel.prefs.SP_JWT_TOKEN_KEY))
            viewModel.refreshToken()
            val navMenu = nav_view.menu
            navMenu.findItem(R.id.nav_my_courses).isVisible = true
            nav_view.setCheckedItem(R.id.nav_my_courses)
            transaction.replace(R.id.fragment_holder, MyCoursesFragment()).commit()
            setUser()
        } else {
            transaction.replace(R.id.fragment_holder, HomeFragment()).commit()
        }
        nav_view.getHeaderView(0).login_button.setOnClickListener(this)
    }

    private fun setUser() {
        if (viewModel.prefs.SP_USER_IMAGE.isNotEmpty())
            nav_view.getHeaderView(0).nav_header_imageView.apply {
                setOnClickListener(this@HomeActivity)
                Picasso.get().load(viewModel.prefs.SP_USER_IMAGE)
                    .placeholder(R.drawable.defaultavatar).fit().into(this)
            }
        nav_view.getHeaderView(0).login_button.apply {
            text = resources.getString(R.string.logout)
        }
        val navMenu = nav_view.menu
        navMenu.findItem(R.id.nav_my_courses).isVisible = true
    }

    override fun onStart() {
        super.onStart()
        val data = this.intent.data
        if (data != null && data.isHierarchical) {
            if (data.getQueryParameter("code") != null) {
                fetchToken(data)
            }
        }
        checkForUpdates()
    }

    private fun checkForUpdates() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                // For a flexible update, use AppUpdateType.FLEXIBLE
                appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)
            ) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    IMMEDIATE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    1001
                )
            }
        }
    }

    private fun fetchToken(data: Uri) {
        val grantCode = data.getQueryParameter("code") as String
        if (viewModel.prefs.SP_ACCESS_TOKEN_KEY == ACCESS_TOKEN) {
            viewModel.fetchToken(grantCode)
            viewModel.fetchTokenProgress.observeOnce {
                if (it) {
                    fetchUser()
                    Toast.makeText(this@HomeActivity, "Logged In", Toast.LENGTH_SHORT).show()
                } else {
                    Components.showConfirmation(this@HomeActivity, "verify")
                }
            }
        }
    }

    private fun fetchUser() {
        viewModel.getMe()
        viewModel.getMeProgress.observer(this) {
            if (it) {
                (supportFragmentManager.findFragmentById(R.id.fragment_holder) as MyCoursesFragment?)?.refreshCourses()
                setUser()
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if (viewModel.doubleBackToExitPressedOnce) {
                finishAffinity()
                return
            }
            viewModel.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({
                viewModel.doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_all_courses -> {
                changeFragment("All Courses")
            }
            R.id.nav_home -> {
                changeFragment("Home")
            }
            R.id.nav_my_courses -> {
                changeFragment("My Courses")
            }
            R.id.nav_whatsapp -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setPackage("com.whatsapp")
                intent.data = Uri.parse("https://wa.me/919811557517")
                if (packageManager.resolveActivity(intent, 0) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Please install whatsApp", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_preferences -> {
                startActivity(intentFor<SettingsActivity>().singleTop())
            }
            R.id.nav_jobs -> {
                startActivity(intentFor<JobsActivity>().singleTop())
            }
            R.id.nav_contactUs -> {
                startActivity(intentFor<AboutActivity>().singleTop())
            }
            R.id.report_bug -> {
                showReportDialog()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showReportDialog() {
        val reportDialog =
            AlertDialog.Builder(this).create()
        val formView = layoutInflater.inflate(R.layout.report_dialog, null)
        formView.okBtn.setOnClickListener {
            when {
                formView.titleEdtv.editText?.text.isNullOrEmpty() -> {
                    formView.titleEdtv.apply {
                        isErrorEnabled = true
                        error = "Cannot be Empty"
                    }
                }
                formView.descriptionEdtv.editText?.text.isNullOrEmpty() -> {
                    formView.descriptionEdtv.apply {
                        isErrorEnabled = true
                        error = "Cannot be Empty"
                    }
                }
                else -> {
                    val data = hashMapOf(
                        "title" to formView.titleEdtv.editText?.text.toString(),
                        "description" to formView.descriptionEdtv.editText?.text.toString(),
                        "oneauth-id" to viewModel.prefs.SP_ONEAUTH_ID,
                        "device" to Build.MODEL,
                        "version" to Build.VERSION.SDK_INT,
                        "app-version" to BuildConfig.VERSION_CODE

                    )
                    reportDialog.dismiss()

                    db.collection("Reports")
                        .add(data)
                        .addOnSuccessListener {
                            drawer_layout.snackbar("Bug has been reported !!")
                        }.addOnFailureListener {
                            toast("There was some error reporting the bug,PLease Try Again")
                        }
                }
            }
        }
        reportDialog.apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setView(formView)
            setCancelable(true)
            show()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun changeFragment(filter: String) {
        when (filter) {
            "All Courses" -> viewModel.mFragmentToSet = AllCourseFragment()
            "Home" -> viewModel.mFragmentToSet = HomeFragment()
            "My Courses" -> viewModel.mFragmentToSet = MyCoursesFragment()
        }
        onBackPressed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val data = this.intent.data
        if (data != null && data.isHierarchical) {
            if (data.getQueryParameter("code") != null) {
                fetchToken(data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(updateUIReceiver, filter)
        invalidateOptionsMenu()

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        IMMEDIATE,
                        this,
                        1001
                    )
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_notifications, menu)
//        val menuItem = menu.findItem(R.id.action_notifications)
//        if (viewModel.getNotificationCount() == 0) {
//            menuItem.icon = resources.getDrawable(R.drawable.ic_notification)
//        } else {
//            menuItem.icon = resources.getDrawable(R.drawable.ic_notification_active)
//        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_notifications -> {
            startActivity(intentFor<NotificationsActivity>())
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateUIReceiver)
    }

    private fun removeShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java) as ShortcutManager
            shortcutManager.removeAllDynamicShortcuts()
        }
    }

    // Navigation Drawer Methods
    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

    override fun onDrawerClosed(drawerView: View) {
        if (viewModel.mFragmentToSet != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_holder, viewModel.mFragmentToSet ?: HomeFragment())
                .commit()
            viewModel.mFragmentToSet = null
        }
    }

    override fun onDrawerOpened(drawerView: View) {}

    private fun confirmLogout() {
        Components.showConfirmation(this, "logout") {
            if (it) {
                if (viewModel.prefs.SP_ACCESS_TOKEN_KEY != ACCESS_TOKEN) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        removeShortcuts()
                    }
                    viewModel.invalidateToken()
                }
                CBOnlineApp.mInstance.clearApplicationData()
                startActivity(intentFor<LoginActivity>().singleTop())
                finish()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.login_button -> {
                if (viewModel.prefs.SP_ACCESS_TOKEN_KEY == ACCESS_TOKEN) {
                    startActivity(intentFor<LoginActivity>().singleTop())
                    finish()
                } else
                    confirmLogout()
            }
            R.id.nav_header_imageView -> Components.openChrome(
                this,
                "https://account.codingblocks.com"
            )
        }
    }
}
