package com.b866f.launcher

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.b866f.launcher.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppInfo> = emptyList()
    private var favoritePackages: MutableList<String> = mutableListOf()
    private var accentColor = Color.parseColor("#3B82F6")

    private val wallpaperPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            prefs.edit().putString("custom_wallpaper", uri.toString()).apply()
            applyWallpaper(uri.toString())
            Toast.makeText(this, "Wallpaper berhasil diubah", Toast.LENGTH_SHORT).show()
        }
    }

    private val hideSidebarRunnable = Runnable { hideSidebar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("launcher_cfg", MODE_PRIVATE)

        accentColor = Color.parseColor(prefs.getString("theme_color", "#3B82F6"))
        binding.heroText.setTextColor(accentColor)

        allApps = loadInstalledApps()
        FavoriteManager.ensureDefaultFavorites(prefs, allApps.map { it.packageName })
        favoritePackages = FavoriteManager.getFavorites(prefs)

        setupClock()
        setupSidebar()
        setupFavorites()

        binding.btnTheme.setOnClickListener { showThemeDialog() }
        binding.btnWallpaper.setOnClickListener { openWallpaperPicker() }
        binding.btnManageFavorites.setOnClickListener { showFavoriteEditorDialog() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        binding.btnHideSidebar.setOnClickListener { hideSidebar() }

        val existingWallpaper = prefs.getString("custom_wallpaper", null)
        applyWallpaper(existingWallpaper)

        scheduleSidebarHide()
    }

    private fun setupSidebar() {
        binding.btnSidebarToggle.setOnClickListener {
            if (binding.sideBarContainer.translationX == 0f) {
                hideSidebar()
            } else {
                showSidebar()
            }
        }
    }

    private fun showSidebar() {
        binding.sideBarContainer.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(220)
            .start()
        scheduleSidebarHide()
    }

    private fun hideSidebar() {
        binding.sideBarContainer.animate()
            .translationX(-binding.sideBarContainer.width.toFloat())
            .alpha(0f)
            .setDuration(220)
            .start()
    }

    private fun scheduleSidebarHide() {
        handler.removeCallbacks(hideSidebarRunnable)
        handler.postDelayed(hideSidebarRunnable, 4000)
    }

    private fun setupFavorites() {
        val favoriteApps = getFavoriteApps()
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(this, 5)
        binding.favoriteRecyclerView.adapter = FavoriteAdapter(favoriteApps, accentColor) { app ->
            startActivity(app.launchIntent)
        }
    }

    private fun getFavoriteApps(): List<AppInfo> {
        val selected = favoritePackages
        return allApps.filter { it.packageName in selected }
            .sortedBy { favoritePackages.indexOf(it.packageName) }
    }

    private fun showFavoriteEditorDialog() {
        val packageNames = allApps.map { it.packageName }
        val appNames = allApps.map { it.appName }.toTypedArray()
        val selected = favoritePackages.toSet()
        val checked = BooleanArray(packageNames.size) { selected.contains(packageNames[it]) }

        AlertDialog.Builder(this)
            .setTitle("Kelola Aplikasi Favorit")
            .setMultiChoiceItems(appNames, checked) { _, index, isChecked ->
                val pkg = packageNames[index]
                if (isChecked) {
                    if (!favoritePackages.contains(pkg)) {
                        favoritePackages.add(pkg)
                    }
                } else {
                    favoritePackages.remove(pkg)
                }
            }
            .setPositiveButton("Simpan") { _, _ ->
                FavoriteManager.saveFavorites(prefs, favoritePackages)
                setupFavorites()
                Toast.makeText(this, "Daftar favorit disimpan", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showThemeDialog() {
        val colors = arrayOf(
            "#3B82F6", "#7C3AED", "#10B981", "#F59E0B",
            "#E11D48", "#14B8A6", "#F97316", "#0EA5E9"
        )

        val names = arrayOf(
            "Biru", "Ungu", "Hijau", "Kuning",
            "Merah", "Teal", "Jingga", "Sky"
        )

        AlertDialog.Builder(this)
            .setTitle("Pilih Tema")
            .setItems(names) { _, index ->
                accentColor = Color.parseColor(colors[index])
                prefs.edit().putString("theme_color", colors[index]).apply()
                binding.heroText.setTextColor(accentColor)

                val adapter = binding.favoriteRecyclerView.adapter as? FavoriteAdapter
                adapter?.updateAccent(accentColor)
            }
            .show()
    }

    private fun openWallpaperPicker() {
        wallpaperPicker.launch("image/*")
    }

    private fun applyWallpaper(uriString: String?) {
        if (uriString.isNullOrBlank()) {
            binding.wallpaperView.setImageDrawable(null)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                gradientType = GradientDrawable.LINEAR_GRADIENT
                colors = intArrayOf(
                    Color.parseColor("#020F1E"),
                    Color.parseColor("#0A1630"),
                    Color.parseColor("#132652")
                )
                cornerRadius = 0f
            }
            binding.wallpaperView.background = bg
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                binding.wallpaperView.setImageBitmap(bitmap)
                binding.wallpaperView.scaleType = androidx.appcompat.widget.AppCompatImageView.ScaleType.CENTER_CROP
                binding.wallpaperView.background = null
                return
            }
        } catch (_: Exception) {
        }

        binding.wallpaperView.setImageDrawable(null)
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            gradientType = GradientDrawable.LINEAR_GRADIENT
            colors = intArrayOf(
                Color.parseColor("#020F1E"),
                Color.parseColor("#0A1630"),
                Color.parseColor("#132652")
            )
            cornerRadius = 0f
        }
        binding.wallpaperView.background = bg
    }

    private fun setupClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        val updateTime = Runnable {
            binding.timeText.text = timeFormat.format(Date())
        }

        updateTime.run()
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateTime.run()
                handler.postDelayed(this, 60000)
            }
        }, 60000)
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launchIntent, 0)

        val result = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (!seen.add(packageName)) continue

            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            val intent = pm.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                result.add(AppInfo(packageName, appName, icon, intent))
            }
        }

        return result.sortedBy { it.appName.lowercase(Locale.getDefault()) }
    }
}
