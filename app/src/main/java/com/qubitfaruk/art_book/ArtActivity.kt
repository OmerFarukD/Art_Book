package com.qubitfaruk.art_book

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.decodeBitmap
import androidx.core.view.ContentInfoCompat
import com.google.android.material.snackbar.Snackbar
import com.qubitfaruk.art_book.databinding.ActivityArtBinding
import com.qubitfaruk.art_book.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {
    private lateinit var binding:ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var database:SQLiteDatabase
    var selectedBitmap : Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        val intent=intent
        val info=intent.getStringExtra("info")
        if (info.equals("new")){
            binding.artName.setText(" ")
            binding.artistName.setText("")
            binding.year.setText("")
            binding.button3.visibility=View.VISIBLE
            binding.imageView.setImageResource(R.drawable.asd)
        }else{
            binding.button3.visibility=View.INVISIBLE
            val selectedId=intent.getIntExtra("id",1)
            val cursor=database.rawQuery("SELECT * FROM arts WHERE id= ? ", arrayOf(selectedId.toString()))

            val artName=cursor.getColumnIndex("artName")
            val artistName=cursor.getColumnIndex("artistName")
            val year=cursor.getColumnIndex("year")
            val image=cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artName.setText(cursor.getString(artName))
                binding.artistName.setText(cursor.getString(artistName))
                binding.year.setText(cursor.getString(year))

                val byteArray=cursor.getBlob(image)
                val bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }
    }

    private fun makeSmallerBitmap(image: Bitmap,maximumSize:Int):Bitmap{
        var width=image.width
        var height=image.height

        val bitmapRational:Double =width.toDouble()/height.toDouble()

        if (bitmapRational>1){
            width=maximumSize
            val sclaedHeight=width/bitmapRational
            height=sclaedHeight.toInt()

        }else{
            height=maximumSize
            val scaledWidth=height*bitmapRational
            width=scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }


    fun saveButtonOnClicked(view: View){
        val artName=binding.artName.text.toString()
        val artistName=binding.artistName.text.toString()
        val year=binding.year.text.toString()

        if (selectedBitmap!=null){
            val smallBitmap=makeSmallerBitmap(selectedBitmap!!,300)
            val outputStream=ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray=outputStream.toByteArray()

            try{
                database.execSQL("CREATE TABLE IF NOT EXISTS arts" +
                        " (id INTEGER PRIMARY KEY, artName VARCHAR, artistName VARCHAR,year VARCHAR, image BLOB)")
                val sqlString="INSERT INTO arts (artName,artistName,year,image) VALUES (?,?,?,?)"
                val statement=database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
           }catch (e :Exception){
                e.printStackTrace()
            }

            val intent =Intent(this@ArtActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }


    }

    fun selectImage(view: View){
     if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
         if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
             Snackbar.make(view,"Galeriye erişmek için izin vermeniz gerekiyor.",Snackbar.LENGTH_INDEFINITE)
                 .setAction("İzin ver",View.OnClickListener {
                     permissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                 }).show()
         }else{
             permissionResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
         }

     }else{
         val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
         activityResultLauncher.launch(intentToGallery)

     }
    }

    private fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if (result.resultCode== RESULT_OK){
                val intentFromResult=result.data
                if (intentFromResult!=null){
                  val imageData=intentFromResult.data
                    if (imageData!=null){
                        try {
                            if (Build.VERSION.SDK_INT>=28){
                                val source=ImageDecoder.createSource(this@ArtActivity.contentResolver,imageData)
                                selectedBitmap=ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)

                            }
                            else{
                                selectedBitmap=MediaStore.Images.Media.getBitmap(this@ArtActivity.contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    }

                }
            }
        }

        permissionResultLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if (result){
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this@ArtActivity,"İzin Gerekli.",Toast.LENGTH_LONG).show()
            }
        }
    }
}