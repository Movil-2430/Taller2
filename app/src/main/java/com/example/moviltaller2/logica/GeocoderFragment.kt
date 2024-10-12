package com.example.moviltaller2.logica

import android.content.Context
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.example.moviltaller2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GeocoderFragment : Fragment() {

    private lateinit var listener: OnLocationSelectedListener
    private lateinit var geocoder: Geocoder

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnLocationSelectedListener
        geocoder = Geocoder(context, Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_geocoder, container, false)
        val editText = view.findViewById<EditText>(R.id.etGeocoder)

        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = editText.text.toString()
                returnCoordinates(query)
                true
            } else {
                false
            }
        }

        return view
    }

    fun returnAdress(altitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addressList = geocoder.getFromLocation(altitude, longitude, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    withContext(Dispatchers.Main) {
                        listener.onAdressRequested(address.getAddressLine(0))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        listener.onAdressRequested("")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun returnCoordinates(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addressList = geocoder.getFromLocationName(query, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    withContext(Dispatchers.Main) {
                        listener.onLocationSelected(address.latitude, address.longitude, query)
                    }
                } else{
                    withContext(Dispatchers.Main) {
                        listener.onLocationSelected(null, null, "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    interface OnLocationSelectedListener {
        fun onLocationSelected(latitud: Double?, longitud: Double?, query: String)
        fun onAdressRequested(adress: String)
    }

}