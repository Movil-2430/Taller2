package com.example.moviltaller2.logica

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.moviltaller2.R
import com.example.moviltaller2.datos.Data
import java.io.File
import java.io.IOException

class PhotoActivity : AppCompatActivity() {
    //variables globales para ir viendo el estado de las fases para solicitar permisos
    private lateinit var btnFotografia: Button
    private lateinit var btnGaleria: Button
    // acceder al mensaje que nos dice si ya se aceptaron los permisos
    private lateinit var txtMensaje: TextView

    private lateinit var imgFoto: ImageView
    //ruta de la imagen
    private lateinit var rutaImagen:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        //asignar valores a los botones
        btnFotografia= findViewById(R.id.btnFotografia)
        imgFoto =findViewById(R.id.imgFoto)
        btnGaleria = findViewById(R.id.btnGaleria)


        btnFotografia.setOnClickListener{
            //se le piden los permisos de la camara
            checkAndRequestCameraPermission()

        }
        btnGaleria.setOnClickListener{

            checkAndRequestGalleryPermission()
        }


    }

    private fun checkAndRequestGalleryPermission() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            openGallery()  // Permiso ya concedido, abre la galería
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                Data.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,2)
    }


    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            openCamara()  // Permiso ya concedido, accede a la camara
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                Data.MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
    }

    private fun openCamara() {


        //abre la camara
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // cear archivo temporal para luego almacenarlo en el dispositivo
        var imagen : File?=null

        //tener un catch por si hay un error

        try {
            imagen= crearImagen()
        } catch (ex: IOException){
            Log.e("Error",ex.toString())
        }

        //fileprovider es una clase especializada para compartir archivos de manera segura en ves de pasarl la uri copleta que expone la seguridad
        //si la imagen ya se creo
        if(imagen!=null){
            //
            val fotoUri = FileProvider.getUriForFile(
                this,
                //paquete donde se esta ejecutando
                "com.example.moviltaller2",
                imagen
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT,fotoUri)
        }

        startActivityForResult(intent,Data.MY_PERMISSIONS_REQUEST_CAMERA)

    }

    private fun crearImagen(): File? {

        val nombre ="foto_"
        //  referencia al directorio donde se guardarán las imágenes,
        val directorio=getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //aqui se crea la imagen  temporal único en el directorio(carpeta) dado.
        val imagen = File.createTempFile(nombre,".jpg",directorio)
        //Este metodo de absolutePath es para obtener toda la ruta desde la raiz
        rutaImagen=imagen.absolutePath
        return imagen

    }

    //onActivityResult: Es un método de la clase Activity que se llama cuando una actividad secundaria
    //que se inició con startActivityForResult termina y devuelve un resultado.
    // se sabe que ya se tomo la foto, se creo una archivo de imagen temporal para guardar esa foto y
    //ahora se se asigna esa imagen temporal al imgbitmap y muestra la imagen decodifica en el imageview del la interfaz

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode== Data.MY_PERMISSIONS_REQUEST_CAMERA && resultCode== RESULT_OK){
            // Decodifica la imagen con opciones para ajustar el tamaño
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true  // Primero solo obtiene las dimensiones
            BitmapFactory.decodeFile(rutaImagen, options)

            // Ajusta el tamaño de la imagen si es muy grande
            options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
            options.inJustDecodeBounds = false  // Ahora decodifica la imagen a un tamaño ajustado

            // Cargar la imagen en el ImageView con la resolución ajustada
            val imgBitmap = BitmapFactory.decodeFile(rutaImagen, options)
            guardarEnGaleria(imgBitmap)
            imgFoto.setImageBitmap(imgBitmap)
        }

        if (requestCode== Data.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE && resultCode== RESULT_OK){
            if (data != null && data.data != null) {
                val imageUri = data.data  // Obtén la URI de la imagen seleccionada
                imgFoto.setImageURI(imageUri)  // Muestra la imagen seleccionada en el ImageView
            }
        }

    }

    private fun guardarEnGaleria(imgBitmap: Bitmap){
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "imagen_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        contentResolver.openOutputStream(uri!!).use { outputStream ->
            if (outputStream != null) {
                imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calcula el inSampleSize como una potencia de 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Data.MY_PERMISSIONS_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido, abre la cámara
                    try {
                        openCamara()
                    } catch (e: Exception) {
                        Log.e("CameraError", "Error al abrir la cámara: ${e.message}")
                    }
                } else {
                    // Permiso denegado, muestra un mensaje o maneja el caso
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
            Data.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido, abre la galería
                    openGallery()
                } else {
                    // Permiso denegado, muestra un mensaje o maneja el caso
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
            // Puedes manejar otros permisos aquí si es necesario
        }
    }

}