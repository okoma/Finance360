package com.i2medier.financialpro.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Environment
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import com.i2medier.financialpro.activity.SaveShareActivity
import java.io.File
import java.io.FileOutputStream

object ShareUtil {
    @JvmField
    var Path_File: File? = null

    private fun createLoadingDialog(context: Context): AlertDialog {
        val progressBar = ProgressBar(context).apply { isIndeterminate = true }
        return AlertDialog.Builder(context)
            .setTitle("Saving...")
            .setView(progressBar)
            .setCancelable(false)
            .create()
    }

    @JvmStatic
    fun print(context: Context, scrollView: ScrollView, str: String) {
        val loadingDialog = createLoadingDialog(context)
        loadingDialog.show()
        val bitmapFromView = getBitmapFromView(
            scrollView,
            scrollView.getChildAt(0).height,
            scrollView.getChildAt(0).width
        )
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .absolutePath + "/Financial Calculator"
            )
            if (!file.exists()) {
                file.mkdir()
            }
            val ts = (System.currentTimeMillis() / 1000).toString()
            val str2 = "${str}_${ts}.jpg"
            var file2 = File(file, str2)
            if (file2.exists()) {
                file2.delete()
                file2 = File(file, str2)
            }
            val fileOutputStream = FileOutputStream(file2)
            bitmapFromView.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            loadingDialog.dismiss()

            Path_File = file2
            val shareSaveIntent = Intent(context, SaveShareActivity::class.java)
            context.startActivity(shareSaveIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            loadingDialog.dismiss()
        }
    }

    private fun getBitmapFromView(view: View, i: Int, i2: Int): Bitmap {
        val createBitmap = Bitmap.createBitmap(i2, i, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(createBitmap)
        val background: Drawable? = view.background
        if (background != null) {
            background.draw(canvas)
        } else {
            canvas.drawColor(-1)
        }
        view.draw(canvas)
        return createBitmap
    }
}
