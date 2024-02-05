package com.sofiyashahi.imagetotext

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sofiyashahi.imagetotext.databinding.ActivityMainBinding
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CAMERA_CODE = 100
    private lateinit var bitmap: Bitmap
    private val TAG = "MainActivity"
    private var isCropActivityActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_CODE)
        }

        binding.btnCapture.setOnClickListener {
            isCropActivityActive = true
            CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(this)
        }

        binding.btnCopy.setOnClickListener {
            val scannedText = binding.textData.text.toString()
            copyToClipBoard(scannedText)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called")
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                val result: CropImage.ActivityResult? = CropImage.getActivityResult(data)
                if (result != null) {
                    val resultUri = result.uri
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
                        getTextFromImage(bitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    // Handle the case when result is null
                    Toast.makeText(this, "Crop failed", Toast.LENGTH_SHORT).show()
                }

            }
            isCropActivityActive = false
        }
    }

    private fun getTextFromImage(bitmap: Bitmap){
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText->
                processTextRecognitionResult(visionText)
            }
            .addOnFailureListener{ e->
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(visionText: Text){
        val resultText = visionText.text
        binding.textData.text = resultText
        binding.btnCapture.text = "Retake"
        binding.btnCopy.visibility = View.VISIBLE
    }

    private fun copyToClipBoard(text: String){
        val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied data", text)
        clipBoard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: called")
        if (isCropActivityActive) {
            Log.d(TAG, "onBackPressed: crop activity")
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
        } else {
            // If not, perform the default back press behavior
            super.onBackPressed()
        }
    }
}