package com.example.testingapp

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.testingapp.databinding.ActivityImageTakingBinding
import java.io.InputStream

class ImageTaking : AppCompatActivity() {
    private lateinit var binding: ActivityImageTakingBinding
    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageTakingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.selectImage.setImageURI(it)
            if (it != null) {
                uri = it
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            if (uri != null) {
                val bitmap: Bitmap? = uriToBitmap(this, uri!!)
                val fileName: String? = getFileName(this, uri!!)
                if (bitmap != null && fileName != null) {
                    savePhoto(fileName, bitmap)
                } else {
                    Toast.makeText(this, "Profile pic updation unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun savePhoto(fileName: String, bitmap: Bitmap) {
        try {
            val finalFileName = if (fileName.endsWith(".jpg", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.jpg"
            }

            openFileOutput(finalFileName, MODE_PRIVATE).use { stream ->
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                    Toast.makeText(this, "Profile pic updated successfully", Toast.LENGTH_SHORT).show()

                    // Save the file name to a file
                    val fileNameStorage = openFileOutput("profilePicFileName.txt", MODE_PRIVATE)
                    fileNameStorage.write(finalFileName.toByteArray())
                    fileNameStorage.close()

                    val intent = Intent(this, Home::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val contentResolver: ContentResolver = context.contentResolver
            inputStream = contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }

    @SuppressLint("Range")
    private fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            it.moveToFirst()
            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }
}
