package com.example.moviltaller2.logica

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.moviltaller2.BuildConfig
import com.example.moviltaller2.R
import com.example.moviltaller2.databinding.ActivityMapBinding
import com.example.moviltaller2.modelo.LocationRegistry
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.time.LocalDateTime


class MapActivity : AppCompatActivity(), GeocoderFragment.OnLocationSelectedListener {

    private val reportedMeters: Int = 30
    private val normalZoom = 18.0
    private val extraZoom = 20.0
    private val locationUpdateInterval:Long = 10000
    private val minLocationUpdateInterval:Long = 5000
    private val minLight = 1500
    private val fileLocationsRegistryName = "locations.json"
    private val noAvaliableMarkerName = "Cargando dirección..."

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: MapView
    private lateinit var geocoderFragment: GeocoderFragment
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    private lateinit var currentLocationMarker: Marker
    private lateinit var mapController: IMapController
    private lateinit var currentLocation: Location
    private lateinit var selectedLocation: Location
    private lateinit var selectedLocationMarker: Marker

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener
    private var lightSensorInitialized = false

    private val getLocationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if(result.resultCode == RESULT_OK){
            startLocationUpdates()
        }else{
            Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = binding.osmMap
        mapController = map.controller
        roadManager = OSRMRoadManager(this, "ANDROID")

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        initUserLocation()
    }

    private fun initUserLocation(){
        try {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = it
                    currentLocationMarker = Marker(map)
                    currentLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.user_marker_icon)
                    updateMarker(it)
                    adjustMarkerSize(currentLocationMarker)

                    initActivity()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
            Log.e("Location", "Error: ${e.message}")
        }
    }

    private fun initActivity(){
        initUI()
        initLocationCallBack()
        checkLocationSettings()
    }

    private fun initLocationCallBack(){
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null &&
                    currentLocation.distanceTo(location) > reportedMeters) {

                    updateMarker(location)
                    currentLocation = location
                    writeLocationToFile(location)
                }
            }
        }
    }

    private fun updateMarker(location: Location){
        GeoPoint(location.latitude, location.longitude).let {
            mapController.setZoom(normalZoom)
            mapController.setCenter(it)
            currentLocationMarker.position = it
            currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            currentLocationMarker.title = "Ubicación actual"
            map.invalidate() // Refresh the map
            Log.d("Location", "Location updated: ${location.latitude}, ${location.longitude}")
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun checkLocationSettings(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if ((e as ApiException).statusCode == CommonStatusCodes.RESOLUTION_REQUIRED){
                val resolvable = e as ResolvableApiException
                val isr = IntentSenderRequest.Builder(resolvable.resolution).build()
                getLocationSettings.launch(isr)
            }else{
                Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,locationUpdateInterval).apply {
            setMinUpdateIntervalMillis(minLocationUpdateInterval)
        }.build()

    private fun adjustMarkerSize(marker: Marker) {
        val zoomLevel = map.zoomLevelDouble
        val scaleFactor = zoomLevel / 20.0 // Adjust the divisor to control scaling
        val icon = marker.icon
        icon?.setBounds(0, 0, (icon.intrinsicWidth * scaleFactor).toInt(), (icon.intrinsicHeight * scaleFactor).toInt())
        marker.icon = icon
    }

    private fun initUI(){
        initMap()
        initFragment()
        initLuminositySensor()
        initButtons()
    }

    private fun initMap(){
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(currentLocationMarker)
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if(uiManager.nightMode == UiModeManager.MODE_NIGHT_YES)
            binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)


        initMapListeners()
        initMapHelpers()
    }

    private fun initMapListeners(){
        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                adjustMarkerSize(currentLocationMarker)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                adjustMarkerSize(currentLocationMarker)
                return true
            }
        })
    }

    private fun initMapHelpers(){
        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                initLongClickLocation(p)
                return false
            }
        }))
    }

    private fun initLongClickLocation(p: GeoPoint?){
        if(p != null){
            selectedLocation = Location("")
            selectedLocation.latitude = p.latitude
            selectedLocation.longitude = p.longitude

            initSelectedLocationMarker(p, noAvaliableMarkerName)
        }
    }

    private fun initFragment(){
        geocoderFragment = GeocoderFragment()
        supportFragmentManager.beginTransaction().replace(R.id.geocoderFragment, geocoderFragment).commit()
    }

    private fun initLuminositySensor(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
            lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val light = event.values[0]
                    if(light < minLight){
                        binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    }else{
                        binding.osmMap.overlayManager.tilesOverlay.setColorFilter(null)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            lightSensorInitialized = true
        }
        else {
            Log.d("Luminosity", "No light sensor available")
        }
    }

    private fun initButtons(){
        binding.ibMakeRoute.setOnClickListener {
            createRoute(GeoPoint(currentLocation.latitude, currentLocation.longitude),
                GeoPoint(selectedLocation.latitude, selectedLocation.longitude))
        }
    }

    private fun createRoute(start: GeoPoint, finish: GeoPoint){
        CoroutineScope(Dispatchers.IO).launch {
            val routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            val road = roadManager.getRoad(routePoints)

            withContext(Dispatchers.Main) {
                if (binding.osmMap != null) {
                    roadOverlay?.let { binding.osmMap.overlays.remove(it) }
                    roadOverlay = RoadManager.buildRoadOverlay(road)
                    roadOverlay?.outlinePaint?.color = Color.RED
                    roadOverlay?.outlinePaint?.strokeWidth = 10f
                    binding.osmMap.overlays.add(roadOverlay)
                    val distanciaStr = String.format("%.2f", road.mLength)
                    Toast.makeText(this@MapActivity, "Distancia ruta: $distanciaStr km", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun writeLocationToFile(location: Location){
        val file = File(baseContext.getExternalFilesDir(null), fileLocationsRegistryName)
        val jsonString = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        val jsonArray = if (jsonString.isNotEmpty()) JSONArray(jsonString) else JSONArray()

        val currentTime = LocalDateTime.now()
        val locationRegistry = LocationRegistry(location.latitude, location.longitude, currentTime)
        val json = locationRegistry.toJSON()
        jsonArray.put(json)
        file.writeText(jsonArray.toString(), Charsets.UTF_8)
        Log.d("Location", "Location saved: $json")
    }

    private fun initSelectedLocationMarker(p: GeoPoint, name:String){
        if(::selectedLocationMarker.isInitialized) map.overlays.remove(selectedLocationMarker)
        selectedLocationMarker = Marker(map)
        geocoderFragment.returnAdress(p.latitude, p.longitude)
        selectedLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.location_marker_icon)
        selectedLocationMarker.position = p
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        selectedLocationMarker.title = name

        mapController.setZoom(extraZoom)
        mapController.setCenter(p)
        map.overlays.add(selectedLocationMarker)
        roadOverlay?.let { binding.osmMap.overlays.remove(it) }
        map.invalidate()

        binding.ibMakeRoute.visibility = VISIBLE
        binding.ibMakeRoute.isClickable = true

        val distancia = currentLocation.distanceTo(selectedLocation) / 1000
        val distanciaString = String.format("%.2f", distancia)
        Toast.makeText(this, "Distancia en línea recta a la ubicación: $distanciaString km", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        mapController = map.controller
        mapController.setZoom(normalZoom)

        if (lightSensorInitialized) sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()

        if (lightSensorInitialized) sensorManager.unregisterListener(lightSensorListener)
    }

    private fun stopLocationUpdates() {
        if (mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }

    override fun onLocationSelected(latitud: Double?, longitud: Double?, query: String) {
        if(latitud != null && longitud != null){
            val geoPoint = GeoPoint(latitud, longitud)
            selectedLocation = Location("")
            selectedLocation.latitude = latitud
            selectedLocation.longitude = longitud
            initSelectedLocationMarker(geoPoint, query)
        }else{
            Toast.makeText(this, "No se encontró la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAdressRequested(adress: String) {
        selectedLocationMarker.title = adress
    }
}