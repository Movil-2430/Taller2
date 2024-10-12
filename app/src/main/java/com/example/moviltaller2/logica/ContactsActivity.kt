package com.example.moviltaller2.logica

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.moviltaller2.R
import com.example.moviltaller2.datos.Data
import com.example.moviltaller2.modelo.ContactsAdapter

class ContactsActivity : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        mlista = findViewById<ListView>(R.id.listaContactos)
        mProjection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        mContactsAdapter = ContactsAdapter(this, null, 0)
        mlista?.adapter = mContactsAdapter

        inicializar()

    }

    private fun initView(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            mCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
            )
            mContactsAdapter?.changeCursor(mCursor)
        }
    }

    private fun inicializar(){
        requestPermission(this, Manifest.permission.READ_CONTACTS, "Se necesita permiso para leer los contactos", Data.MY_PERMISSIONS_REQUEST_READ_CONTACTS)
    }

    private fun requestPermission(context: Activity, permiso: String, justificacion: String, idCode: Int) {
        //val textView = findViewById<TextView>(R.id.resultadosContactosTextView)
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no ha sido concedido, lo solicitamos
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        } else {
            initView()
            // Si el permiso ya ha sido concedido, iniciamos la actividad
            //textView.text = "Permiso concedido"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Data.MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initView()
                }
                return
            }

            else -> {

            }
        }
    }
}