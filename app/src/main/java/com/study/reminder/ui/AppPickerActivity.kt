package com.study.reminder.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.study.reminder.R
import com.study.reminder.helper.WhitelistManager
import kotlinx.coroutines.*

class AppPickerActivity : AppCompatActivity() {

    private lateinit var rvApps: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AppPickerAdapter

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var allApps: List<AppInfo> = emptyList()

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?,
        var inWhitelist: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // 根据设置应用正确的主题
        val prefs = getSharedPreferences("study_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("opt_dark_theme", true)
        if (isDark) {
            setTheme(R.style.Theme_StudyReminder)
        } else {
            setTheme(R.style.Theme_StudyReminder_Light)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)
        title = "白名单设置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvApps = findViewById(R.id.rv_apps)
        etSearch = findViewById(R.id.et_search_app)
        progressBar = findViewById(R.id.progress_apps)

        adapter = AppPickerAdapter(
            onToggle = { info, checked ->
                if (checked) WhitelistManager.addPackage(this, info.packageName)
                else WhitelistManager.removePackage(this, info.packageName)
            }
        )
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                adapter.filter(if (q.isEmpty()) allApps else allApps.filter {
                    it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadApps()
    }

    private fun loadApps() {
        progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val whitelist = WhitelistManager.getWhitelist(this@AppPickerActivity)
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                // 获取所有有桌面图标的App（包含系统App），更全面
                val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

                resolveInfos.map { ri ->
                    val pkg = ri.activityInfo.packageName
                    AppInfo(
                        name = ri.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = try { ri.loadIcon(pm) } catch (e: Exception) { null },
                        inWhitelist = pkg in whitelist
                    )
                }
                .distinctBy { it.packageName }
                .sortedWith(compareByDescending<AppInfo> { it.inWhitelist }.thenBy { it.name })
            }
            allApps = apps
            adapter.filter(apps)
            progressBar.visibility = android.view.View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

class AppPickerAdapter(
    private val onToggle: (AppPickerActivity.AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.VH>() {

    private var items: List<AppPickerActivity.AppInfo> = emptyList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.iv_app_icon)
        val tvName: TextView = v.findViewById(R.id.tv_app_name)
        val tvPkg: TextView = v.findViewById(R.id.tv_app_pkg)
        val cbWhitelist: CheckBox = v.findViewById(R.id.cb_whitelist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_picker, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPkg.text = item.packageName
        holder.ivIcon.setImageDrawable(item.icon)
        holder.cbWhitelist.setOnCheckedChangeListener(null)
        holder.cbWhitelist.isChecked = item.inWhitelist
        holder.cbWhitelist.setOnCheckedChangeListener { _, checked ->
            item.inWhitelist = checked
            onToggle(item, checked)
        }
        holder.itemView.setOnClickListener {
            holder.cbWhitelist.isChecked = !holder.cbWhitelist.isChecked
        }
    }

    override fun getItemCount() = items.size

    fun filter(list: List<AppPickerActivity.AppInfo>) {
        items = list
        notifyDataSetChanged()
    }
}
