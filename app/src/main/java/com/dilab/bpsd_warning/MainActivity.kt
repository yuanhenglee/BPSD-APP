package com.dilab.bpsd_warning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dilab.bpsd_warning.databinding.ActivityMainBinding
import com.dilab.bpsd_warning.databinding.NavHeaderMainBinding
import com.dilab.bpsd_warning.ui.LoginActivity
import com.dilab.bpsd_warning.ui.AccountFragment
import com.dilab.bpsd_warning.ui.LogFragment
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        checkPermission()
        createNotificationChannel()
        attachFragments()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            setContentView(R.layout.activity_main)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setSupportActionBar(binding.appBarMain.toolbar)
            val drawerLayout: DrawerLayout = binding.drawerLayout
            val navView: NavigationView = binding.navView
            val navController = findNavController(R.id.nav_host_fragment_content_main)

            // setup the email in the nav header
            var headerBinding = NavHeaderMainBinding.bind(navView.getHeaderView(0))
            headerBinding.emailTextView.text = currentUser.email

            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_account, R.id.nav_log
                ), drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)

            if (intent.getBooleanExtra("openLogFragment", false)) {
                navController.navigate(R.id.nav_log)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MainActivity", "checkPermission: no permission, try to request permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.POST_NOTIFICATIONS"),
                1
            )
        }
    }

    private fun createNotificationChannel() {
        val name = "BPSD"
        val descriptionText = "BPSD Warning"
        val channel = NotificationChannel("BPSD", name, NotificationManager.IMPORTANCE_HIGH ).apply {
            description = descriptionText
            enableVibration(true)
            enableLights(true)
                lightColor = Color.RED
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun attachFragments() {
        val logFragment = LogFragment()
        val accountFragment = AccountFragment()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.nav_host_fragment_content_main, logFragment)
            add(R.id.nav_host_fragment_content_main, accountFragment)
            hide(logFragment)
            hide(accountFragment)
            commit()
        }
    }
}