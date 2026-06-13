package com.example.iphonelauncher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var appsRecycler: RecyclerView
    private lateinit var dockRecycler: RecyclerView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable

    private val preferredDockPackages = listOf(
        "com.android.dialer",
        "com.android.contacts",
        "com.android.mms",
        "com.android.camera2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        dateText = findViewById(R.id.dateText)
        appsRecycler = findViewById(R.id.appsRecycler)
        dockRecycler = findViewById(R.id.dockRecycler)

        startClock()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun startClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                val now = Date()
                clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                dateText.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockRunnable)
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val allApps = resolveInfos.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
                ?: Intent().apply {
                    component = android.content.ComponentName(
                        packageName,
                        resolveInfo.activityInfo.name
                    )
                }
            AppInfo(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = packageName,
                icon = resolveInfo.loadIcon(pm),
                launchIntent = launchIntent
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }

        val dockApps = mutableListOf<AppInfo>()
        for (pkg in preferredDockPackages) {
            allApps.find { it.packageName == pkg }?.let { dockApps.add(it) }
        }
        var i = 0
        while (dockApps.size < 4 && i < allApps.size) {
            if (allApps[i] !in dockApps) {
                dockApps.add(allApps[i])
            }
            i++
        }

        val gridApps = allApps.filter { it !in dockApps }

        appsRecycler.layoutManager = GridLayoutManager(this, 4)
        appsRecycler.adapter = AppAdapter(
            apps = gridApps,
            showLabel = true,
            onAppClick = { app -> openApp(app) }
        )

        dockRecycler.layoutManager = GridLayoutManager(this, 4)
        dockRecycler.adapter = AppAdapter(
            apps = dockApps,
            showLabel = false,
            onAppClick = { app -> openApp(app) }
        )
    }

    private fun openApp(app: AppInfo) {
        try {
            startActivity(app.launchIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "לא ניתן לפתוח את ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        // לא עושים כלום
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }
}
