package com.example.remusocrlogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.remusocrlogger.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    lateinit var binding: ActivityMainBinding
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.apply {
            captureImage.setOnClickListener {
                selectImage()
                textView.text = ""
            }
            detectTextImageBtn.setOnClickListener {
                processImage()
            }
            buttonCopy.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OCR Text", textView.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "Text copied to clipboard", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val extras: Bundle? = data?.extras
            imageBitmap = extras?.get("data") as Bitmap
            if (imageBitmap != null) {
                binding.imageView.setImageBitmap(imageBitmap)
            }
        }
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uri: Uri? = data?.data
            try {
                uri?.let { imageUri ->
                    imageBitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        imageUri
                    )
                    binding.imageView.setImageBitmap(imageBitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectImage() {
        val items = arrayOf<CharSequence>("Take photo", "Choose from gallery", "Cancel")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Add photo")
        builder.setItems(items) { dialog, item ->
            when {
                items[item] == "Take photo" -> {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                        takePictureIntent.resolveActivity(packageManager)?.also {
                            cameraActivityResultLauncher.launch(takePictureIntent)
                        }
                    }
                }
                items[item] == "Choose from gallery" -> {
                    Intent(Intent.ACTION_PICK).also { galleryIntent ->
                        galleryIntent.type = "image/*"
                        galleryActivityResultLauncher.launch(galleryIntent)
                    }
                }
                items[item] == "Cancel" -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }

    private fun processImage() {
        if (imageBitmap != null) {
            val image = imageBitmap?.let {
                InputImage.fromBitmap(it, 0)
            }
            image?.let {
                recognizer.process(it)
                    .addOnSuccessListener { visionText ->
                        binding.textView.setText(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "Please select photo", Toast.LENGTH_SHORT).show()
        }
    }
}