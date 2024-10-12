package com.example.moviltaller2

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.moviltaller2.databinding.ActivityMainBinding
import com.example.moviltaller2.datos.Data
import com.example.moviltaller2.logica.ContactsActivity
import com.example.moviltaller2.logica.MapActivity
import com.example.moviltaller2.logica.PhotoActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI(){
        initButtons()
    }

    private fun initButtons(){
        binding.ibCamara.setOnClickListener {
            Intent(this, PhotoActivity::class.java).also {
                startActivity(it)
            }
        }

        binding.ibContacts.setOnClickListener {
            Intent(this, ContactsActivity::class.java).also {
                startActivity(it)
            }
        }

        binding.ibMap.setOnClickListener {
            requestPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION, Data.MY_PERMISSIONS_LOCATION)
        }
    }

    private fun requestPermission(context: Activity, permiso: String, idCode: Int) {
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no ha sido concedido, lo solicitamos
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        } else {
            // Si el permiso ya ha sido concedido, iniciamos la actividad
            startRequestedIntent(idCode)
        }
    }

    private fun startRequestedIntent(requestCode: Int){
        when(requestCode){
            Data.MY_PERMISSIONS_LOCATION -> {
                Intent(this, MapActivity::class.java).also {
                    startActivity(it)
                }
            }
            Data.MY_PERMISSIONS_REQUEST_CAMERA -> {
                //Intent(this, CameraActivity::class.java).also {
                //    startActivity(it)
                //}
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Data.MY_PERMISSIONS_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startRequestedIntent(requestCode)
                } else {
                    Toast.makeText(this, "Permiso denegado es necesario", Toast.LENGTH_SHORT).show()
                }
                return
            }

            Data.MY_PERMISSIONS_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startRequestedIntent(requestCode)
                } else {
                    Toast.makeText(this, "Permiso denegado es necesario", Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> {

            }
        }
    }
}