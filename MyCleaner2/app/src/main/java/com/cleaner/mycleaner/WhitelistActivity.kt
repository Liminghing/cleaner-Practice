package com.cleaner.mycleaner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.cleaner.mycleaner.R

class WhitelistActivity : AppCompatActivity() {
    //private lateinit var binding: ActivityWhitelistBinding
    private var whiteList: ArrayList<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivityWhitelistBinding.inflate(layoutInflater)
        //setContentView(binding.root)
        //binding.buttonAddToWhitelist.setOnClickListener { addToWhiteList() }
        getWhiteList(MainActivity.preferences)
        //loadViews()
    }

    private fun addToWhiteList() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        mGetContent.launch(intent)
    }
    private var mGetContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val document = DocumentFile.fromTreeUri(this, uri)
                val path = getDirectoryPath(document)
                if (path != null && !whiteList.contains(path)) {
                    whiteList.add(path)
                    MainActivity.preferences?.edit()?.putStringSet(getString(R.string.key_whitelist), HashSet(whiteList))?.apply()
                }
            }
        }
    }
    private fun getDirectoryPath(document: DocumentFile?): String? {
        if (document == null || !document.isDirectory || !document.canRead()) {
            return null
        }
        val documentUri = document.uri
        val contentResolver = applicationContext.contentResolver
        val cursor = contentResolver.query(documentUri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            val pathIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            var path = it.getString(pathIndex)
            if (path.startsWith("primary:")) {
                path = path.substringAfter("primary:")
            }
            return path
        }
        return null
    }
    companion object {
        private var whiteList: ArrayList<String> = ArrayList()
        fun getWhiteList(preferences: SharedPreferences?): List<String?> {
            if (whiteList.isEmpty()) {
                if (preferences != null) {
                    whiteList = (preferences.getStringSet("whitelist", emptySet())?.toList()?.toMutableList() ?: ArrayList()) as ArrayList<String>
                }
                whiteList.remove("[")
                whiteList.remove("]")
            }
            return whiteList
        }
    }
}
