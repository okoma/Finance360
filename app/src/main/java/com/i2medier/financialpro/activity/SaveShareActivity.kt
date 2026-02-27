package com.i2medier.financialpro.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.util.ShareUtil

class SaveShareActivity : AppCompatActivity() {
    private lateinit var toolBar: Toolbar
    private lateinit var img: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_share)

        init()
        setUpToolbar()

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))
        adAdmob.FullscreenAd(this)
    }

    private fun init() {
        toolBar = findViewById(R.id.toolBar)
        img = findViewById(R.id.img)

        val shareFile = ShareUtil.Path_File
        if (shareFile != null && shareFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(shareFile.absolutePath)
            img.setImageBitmap(myBitmap)
        }

        findViewById<View>(R.id.btnShare).setOnClickListener {
            val file = ShareUtil.Path_File ?: return@setOnClickListener
            val uriForFile: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent().apply {
                action = "android.intent.action.SEND"
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("android.intent.extra.SUBJECT", "")
                putExtra("android.intent.extra.TEXT", "")
                putExtra("android.intent.extra.STREAM", uriForFile)
            }
            try {
                startActivity(Intent.createChooser(intent, "Share Screenshot"))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpToolbar() {
        toolBar.setTitle(getString(R.string.loan_affordability_calculator))
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
