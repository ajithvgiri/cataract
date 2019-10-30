package com.ajithvgiri.cataract

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ajithvgiri.cataract.api.ApiService
import com.ajithvgiri.cataract.utils.*
import com.ajithvgiri.cataract.utils.PermissionHandler.galleryPermission
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE_STORAGE = 100
        const val IMAGE_REQUEST_CODE = 101
    }

    private lateinit var view: View
    private lateinit var mStorageRef: StorageReference
    private var uploadUri: Uri? = null

    private val apiService by lazy {
        ApiService.create()
    }
    var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mStorageRef = FirebaseStorage.getInstance().reference

        view = window.decorView.findViewById(android.R.id.content)



        buttonChoose.setOnClickListener {
            checkPermission()
        }

        buttonUpload.setOnClickListener {
            if (uploadUri != null) {
                uploadUri?.let {
                    uploadImage(it)
                }
            } else {
                view.snack("Please choose some phone") {
                    action("Choose") {
                        checkPermission()
                    }
                }
            }
        }
    }

    private fun checkPermission() {
        PermissionHandler.checkPermission(this, galleryPermission) { result ->
            when (result) {
                CheckPermissionResult.PermissionGranted -> {
                    pickImage()
                }
                CheckPermissionResult.PermissionDisabled -> {
                    ActivityCompat.requestPermissions(
                        this,
                        galleryPermission,
                        PERMISSION_REQUEST_CODE_STORAGE
                    )
                }
                CheckPermissionResult.PermissionAsk -> {
                    ActivityCompat.requestPermissions(
                        this,
                        galleryPermission,
                        PERMISSION_REQUEST_CODE_STORAGE
                    )
                }
                CheckPermissionResult.PermissionPreviouslyDenied -> {
                    // displayAlert(permissionRequestAlert)
                    ActivityCompat.requestPermissions(
                        this,
                        galleryPermission,
                        PERMISSION_REQUEST_CODE_STORAGE
                    )
                }
            }
        }
    }

    //Permission Request Result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_STORAGE -> {
                val perms = HashMap<String, Int>()
                // Initialize the map with both permissions
                perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] =
                    PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices)
                        perms[permissions[i]] = grantResults[i]
                    // Check for both permissions
                    if (perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
                        pickImage()
                    } else {
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                        //                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            || ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        ) {
//                            showDialogOK("Service Permissions are required for this app",
//                                DialogInterface.OnClickListener { dialog, which ->
//                                    when (which) {
//                                        DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
//                                        DialogInterface.BUTTON_NEGATIVE ->
//                                            // proceed with logic by disabling the related features or quit the app.
//                                            finish()
//                                    }
//                                })
                        } else {
                            view.snack("You need to give some mandatory permissions to continue. Do you want to go to app settings?") {}
                            //proceed with logic by disabling the related features or quit the app.
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                    }
                }
            }
        }
    }

    private fun pickImage() {
        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                uploadUri = uri
                val imageFile = uriToImageFile(uri)
                imageView.setImageURI(uri)
                println("image uploadUri path ${imageFile?.path}")
            }
        }
    }


    private fun uploadImage(uploadUri: Uri) {
        progressBar.visibility = View.VISIBLE
        val imageReference = mStorageRef.child("images/${uploadUri.lastPathSegment}")
        val uploading = imageReference.putFile(uploadUri)

        val urlTask = uploading.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                view.snack("Image Uploaded Successfully") {

                }
                println("downloadable path from firestore $downloadUri")
                getInfoFromImage(downloadUri.toString())
            }
        }
    }


    private fun getInfoFromImage(image: String) {
        val hashMap = HashMap<String, String>()
        hashMap.put("image", image)

        disposable = apiService.neuraltalk(hashMap)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    println("result from server ${result.output}")
                    progressBar.visibility = View.GONE
                    textView.text = result.output
                },
                { error ->
                    println("error from server ${error.localizedMessage}")
                    progressBar.visibility = View.GONE
                    view.snack("${error.localizedMessage}") {

                    }
                }
            )
    }
}
