package com.ajithvgiri.cataract.camera

import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.ajithvgiri.cataract.api.ApiService
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class NeuralTalkAnalyzer : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimestamp = 0L

    private val _showDialog = MutableLiveData<Boolean>()
    private val _showLoading = MutableLiveData<Boolean>()
    private val _description = MutableLiveData<String>()
    var analyse = false
    val showDialog = _showDialog
    val showLoading = _showLoading
    val description = _description

    private val apiService by lazy {
        ApiService.create()
    }

    // Create a storage reference from our app
    private lateinit var mStorageRef: StorageReference

    /**
     * Helper extension function used to extract a byte array from an
     * image plane buffer
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    private fun Image.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()
        // Calculate the average luma no more often than every second
        if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(5) && analyse) {
            // Since format in ImageAnalysis is YUV, image.planes[0]
            // contains the Y (luminance) plane
            val buffer = image.planes[0].buffer

            // Extract bitmap from image proxy
            val bitmap = image.image?.toBitmap()
            bitmap?.let {
                analyse = false
                uploadImageToServer(it)
            }

            // Extract image data from callback object
            val data = buffer.toByteArray()
            // Convert the data into an array of pixel values
            val pixels = data.map { it.toInt() and 0xFF }
            // Compute average luminance for the image
            val luma = pixels.average()
            // Log the new luma value
            Log.d("CameraXApp", "Average luminosity: $luma")
            // Update timestamp of last analyzed frame
            lastAnalyzedTimestamp = currentTimestamp
        }
    }

    private fun uploadImageToServer(bitmap: Bitmap) {
        _showLoading.postValue(true)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        mStorageRef = FirebaseStorage.getInstance().reference

        val imageReference = mStorageRef.child("images/${System.currentTimeMillis()}.jpg")
        var uploadTask = imageReference.putBytes(data)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                println("downloadable path from firestore ${downloadUri.toString()}")
                getTextFromImage(downloadUri.toString())
            }
        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
        }.addOnFailureListener {
            _showLoading.postValue(false)
            Log.e("Firebase", "Exception from firebase ${it.localizedMessage}")
        }
    }

    private fun getTextFromImage(image: String) {
        val requestHashMap = HashMap<String, String>()
        requestHashMap["image"] = image
        apiService.run {
            neuraltalk(requestHashMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        _showLoading.postValue(false)
                        _showDialog.postValue(true)
                        Log.d("CameraX", "response from api ${result.output}")
                        _description.postValue(result.output)
                    },
                    { error ->
                        _showLoading.postValue(false)
                        _showDialog.postValue(true)
                        _description.postValue(error.localizedMessage)
                    }
                )
        }
    }
}