package com.gisid.imagelabelingdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector

import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import java.io.IOException
import java.io.InputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    var SELECT_PHOTO_REQUEST_CODE = 100;
    var ASK_PERMISSION_REQUEST_CODE = 101;
    var TAG = MainActivity::class.java.name;
    lateinit var mDetector: FirebaseVisionLabelDetector;
    lateinit var mTextView: TextView;
    lateinit var mImageView: ImageView;
    lateinit var mLayout: View;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        mTextView = findViewById(R.id.textView)
        mImageView = findViewById(R.id.imageView)
        mLayout = findViewById(R.id.main_layout)
        mDetector = FirebaseVision.getInstance().getVisionLabelDetector()
        fab.setOnClickListener { view ->
            checkPermissions()
        }
    }
    fun checkPermissions(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(mLayout, R.string.storage_access_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, object: View.OnClickListener{
                    override fun onClick(p0: View?) {
                        ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            ASK_PERMISSION_REQUEST_CODE);
                    }
                }).show()
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    ASK_PERMISSION_REQUEST_CODE);
            }
        } else {
            openGallery();
        }
    }
    fun openGallery(){
        var photoIntent = Intent(Intent.ACTION_PICK)
        photoIntent.type = "image/*"
        startActivityForResult(photoIntent, SELECT_PHOTO_REQUEST_CODE)
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null){
            var uri = data.data
            try {
//                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                var inputStream = this.getContentResolver().openInputStream(uri!!);
                var bmp = BitmapFactory.decodeStream(inputStream);
                if( inputStream != null ) inputStream.close();
//                var source  =ImageDecoder.createSource(contentResolver, uri!!)
                mImageView.setImageBitmap(bmp)
                mTextView.setText("")

            } catch (e: IOException){
                e.printStackTrace();
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            ASK_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    openGallery();
                } else {
                    // Permission denied. Handle appropriately e.g. you can disable the
                    // functionality that depends on the permission.
                }
                return
            }

        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_process -> {
                processImage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    fun processImage(){
        if(mImageView.drawable == null){
            // ImageView has no image
            Snackbar.make(mLayout, R.string.select_image, Snackbar.LENGTH_SHORT).show();
        } else {
            var bitmap = (mImageView.getDrawable() as BitmapDrawable).bitmap;
            var image = FirebaseVisionImage.fromBitmap(bitmap)

            mDetector.detectInImage(image)
                .addOnSuccessListener(object: OnSuccessListener<List<FirebaseVisionLabel>>{
                    override fun onSuccess(p0: List<FirebaseVisionLabel>?) {
                        var sb = StringBuilder();
                        for (label in p0!!) {
                            val text = label.getLabel()
                            val entityId = label.getEntityId()
                            val confidence = label.getConfidence()
                            sb.append("Label: $text; Confidence: $confidence; Entity ID: $entityId\n")
                        }
                        mTextView.text = sb
                    }
                })
                .addOnFailureListener(object: OnFailureListener{
                    override fun onFailure(e: Exception) {
                        Log.e(TAG, "Image labelling failed " + e);
                    }
                })
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mDetector.isInitialized != null) {
            try {
                mDetector.close()
            } catch (e: IOException) {
                Log.e(TAG, "Exception thrown while trying to close Image Labeling Detector: $e")
            }

        }
    }
}
