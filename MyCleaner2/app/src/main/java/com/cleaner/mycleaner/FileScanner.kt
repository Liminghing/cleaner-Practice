package com.cleaner.mycleaner

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.preference.PreferenceManager
import android.widget.ProgressBar
import android.widget.TextView
import com.cleaner.mycleaner.R
import com.cleaner.mycleaner.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.Locale

class FileScanner(private val path: File, private var context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var res: Resources
    private lateinit var gui: ActivityMainBinding
    private var filesRemoved = 0
    private var kilobytesTotal: Long = 0
    private var delete = false
    private var emptyDir = false
    private var autoWhite = true
    private var corpse = false
    private var invalid = false
    private val listFiles: List<File>
        get() = getListFiles(path)
    private fun getListFiles(parentDirectory: File): List<File> {
        val inFiles = mutableListOf<File>()
        parentDirectory.listFiles()?.forEach { file ->
            if (!isWhiteListed(file) && file.isFile) {
                inFiles.add(file)
            } else if (file.isDirectory) {
                if (!autoWhite || !autoWhiteList(file)) inFiles.add(file)
                inFiles.addAll(getListFiles(file))
            }
        }
        return inFiles
    }
    private fun isWhiteListed(file: File): Boolean {
        for (path in WhitelistActivity.getWhiteList(preferences)) when {
            path.equals(file.absolutePath, ignoreCase = true) ||
                    path.equals(file.name, ignoreCase = true) -> return true
        }
        return false
    }
    private fun autoWhiteList(file: File): Boolean {
        protectedFileList.forEach { protectedFile ->
            if (file.name.lowercase(Locale.getDefault()).contains(protectedFile) && !WhitelistActivity.getWhiteList(
                    preferences
                ).contains(file.absolutePath.lowercase(Locale.getDefault()))) {
                WhitelistActivity.getWhiteList(preferences).toMutableList().add(file.absolutePath.lowercase(Locale.getDefault()))
                preferences.edit().putStringSet("whitelist", HashSet(
                    WhitelistActivity.getWhiteList(
                        preferences
                    )
                )).apply()
                return true
            }
        }
        return false
    }
    private fun filter(file: File?): Boolean {
        if (file == null) return false
        if (invalid && filterInvalidMedia(file)) {
            return true
        }
        if (corpse && file.parentFile?.parentFile?.name == "Android" && file.parentFile?.name == "data" && file.name != ".nomedia" && file.name !in installedPackages) {
            return true
        }
        if (file.isDirectory && isDirectoryEmpty(file) && emptyDir) {
            return true
        }
        for (filter in filters) {
            if (file.absolutePath.lowercase(Locale.getDefault()).matches(filter.lowercase(Locale.getDefault()).toRegex())) {
                return true
            }
        }
        return false
    }
    private fun filterInvalidMedia(file: File?): Boolean {
        if (file == null) return false
        if (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "png") {
            return !isImageValid(file)
        }
        return false
    }
    private fun isImageValid(file: File): Boolean {
        val bitmap: Bitmap? = BitmapFactory.decodeFile(file.absolutePath)
        return bitmap != null
    }
    private val installedPackages: List<String> by lazy {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
    }
    private fun isDirectoryEmpty(directory: File): Boolean {
        return directory.list()?.isEmpty() ?: false
    }
    fun setUpFilters(generic: Boolean, aggressive: Boolean, apk: Boolean, archive: Boolean): FileScanner {
        val folders: MutableList<String> = ArrayList()
        val files: MutableList<String> = ArrayList()
        setResources(context.resources)
        if (archive) {
            files.addAll(listOf(*res.getStringArray(R.array.archive_filter_files)))
        }
        if (generic) {
            folders.addAll(listOf(*res.getStringArray(R.array.generic_filter_folders)))
            files.addAll(listOf(*res.getStringArray(R.array.generic_filter_files)))
        }
        if (aggressive) {
            folders.addAll(listOf(*res.getStringArray(R.array.aggressive_filter_folders)))
            files.addAll(listOf(*res.getStringArray(R.array.aggressive_filter_files)))
        }
        filters.clear()
        for (folder in folders) filters.add(getRegexForFolder(folder))
        for (file in files) filters.add(getRegexForFile(file))
        if (apk) filters.add(getRegexForFile(".apk"))
        return this
    }
    fun startScan(): Long {


        isRunning = true
        var cycles: Byte = 0
        var maxCycles: Byte = 1
        var foundFiles: List<File>
        if (preferences.getBoolean("double_checker", false)) maxCycles = 10
        if (!delete) maxCycles = 1

        while (cycles < maxCycles) {
            (context as MainActivity?)!!.displayText("Running Cycle" + " " + (cycles + 1) + "/" + maxCycles)
            foundFiles = listFiles
            //gui.progressBar.max = gui.progressBar.max + foundFiles.size
            var tv: TextView?
            for (file in foundFiles) {
                if (filter(file)) {
                    tv = (context as MainActivity?)!!.displayDeletion(file)
                    kilobytesTotal += file.length()
                    if (delete) {
                        ++filesRemoved
                        if (!file.delete()) {
                            (context as MainActivity?)!!.runOnUiThread {
                                tv.setTextColor(
                                    Color.GRAY
                                )
                            }
                        }
                    }
                }
                (context as MainActivity?)!!.runOnUiThread {
                    //gui.progressBar.progress = gui.progressBar.progress + 1
                }
                //val scanPercent = gui.progressBar.progress * 100.0 / gui.progressBar.max
                (context as MainActivity?)!!.runOnUiThread {
                    //gui.textView2.text = String.format(Locale.US, "%.0f", scanPercent) + "%"
                    //gui.textView2.text = context.getString(R.string.status_running) + " " + String.format(Locale.US, "%.0f", scanPercent) + "%"
                }
            }
            (context as MainActivity?)!!.displayText("Finished Cycle" + " " + (cycles + 1) + "/" + maxCycles)
            if (filesRemoved == 0) break
            filesRemoved = 0
            ++cycles

        }

        isRunning = false
        return kilobytesTotal
    }
    private fun getRegexForFolder(folder: String): String {
        return ".*(\\\\|/)$folder(\\\\|/|$).*"
    }
    private fun getRegexForFile(file: String): String {
        return ".+" + file.replace(".", "\\.") + "$"
    }
    fun setGUI(gui: ActivityMainBinding?): FileScanner {
        this.gui = gui!!
        return this
    }
    fun setResources(res: Resources?): FileScanner {
        this.res = res!!
        return this
    }
    fun setEmptyDir(emptyDir: Boolean): FileScanner {
        this.emptyDir = emptyDir
        return this
    }
    fun setDelete(delete: Boolean): FileScanner {
        this.delete = delete
        return this
    }
    fun setInvalid(invalid: Boolean): FileScanner {
        this.invalid = invalid
        return this
    }
    fun setCorpse(corpse: Boolean): FileScanner {
        this.corpse = corpse
        return this
    }
    fun setAutoWhite(autoWhite: Boolean): FileScanner {
        this.autoWhite = autoWhite
        return this
    }
    fun setContext(context: Context?): FileScanner {
        this.context = context!!
        return this
    }
    companion object {
        @JvmField
        var isRunning = false
        private val filters = ArrayList<String>()
        private val protectedFileList = arrayOf("backup", "copy", "copies", "important", "do_not_edit")
    }
    init {
        WhitelistActivity.getWhiteList(preferences)
    }
}
