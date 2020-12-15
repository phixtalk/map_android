package com.mapapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.GeoApiContext
import com.mapapp.constants.AppConstants.CALL_PERMISSION
import com.mapapp.constants.AppConstants.FASTEST_INTERVAL
import com.mapapp.constants.AppConstants.GPS_REQUEST
import com.mapapp.constants.AppConstants.PERMISSION_ID
import com.mapapp.constants.AppConstants.UPDATE_INTERVAL
import com.mapapp.constants.AppConstants.ZOOM_LEVEL
import com.mapapp.databinding.ActivityMainBinding
import com.mapapp.utils.LocationUtils.checkPermissions
import com.mapapp.utils.LocationUtils.isGPSEnabled
import com.mapapp.utils.LocationUtils.turnGPSOn
import com.mapapp.utils.setRoundedStrokeBackground
import com.mapapp.utils.showDialog
import com.mapapp.utils.toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), OnMapReadyCallback, KodeinAware {

    override val kodein by kodein()
    private val factory: ViewModelFactory by instance()

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MapViewModel

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mGeoApiContext: GeoApiContext

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewBinding()
        setContentView(binding.root)
        setupViewModel()
        setupButtonDisplay()
        setupButtonClick()
        initialize()
        setupLocationCallbacks()
        getDeviceLocation()
    }

    private fun setupViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, factory).get(MapViewModel::class.java)
    }

    private fun setupButtonDisplay() {
        binding.btnStart.setRoundedStrokeBackground(100f, R.color.btn_blue)
    }

    private fun setupButtonClick() {
        binding.btnStart.setOnClickListener {
            if (viewModel.mLatLng.size == 2){
                val startMarker = viewModel.mMapMarkers[0]
                val toPosition = viewModel.mMapMarkers[1].position

                val carMarker = addCarMarker(startMarker.position)
                animateCar(mMap, carMarker, toPosition, false)
            }else{
                toast("We need 2 points to animate car.")
            }
        }
    }

    private fun initialize(){
        if (checkPermissions(this)) {
            if (isGPSEnabled(this)) initializeMap()
            else turnGPSOn(this)
        } else {
            requestPermissions()
        }
    }

    private fun initializeMap() {
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGeoApiContext = GeoApiContext.Builder()
            .apiKey(getString(R.string.google_map_api_key))
            .build()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap.isMyLocationEnabled = true

        setupMapClickListener()
        setUpMarkerDragListener()
    }

    private fun setupMapClickListener(){
        if(::mMap.isInitialized){
            mMap.setOnMapClickListener {
                addNewMarker(it)
            }
        }
    }

    private fun setUpMarkerDragListener(){
        if(::mMap.isInitialized){
            mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(p0: Marker?) {}
                override fun onMarkerDrag(p0: Marker?) {}
                override fun onMarkerDragEnd(mk: Marker?) {
                    updateMapMarkerData(mk!!) //update with new marker location
                    setLatLngFromMarkerData()
                    if (viewModel.mLatLng.size == 2) {
                        removeAllPolylineData()
                        drawPolylineRoute(viewModel.mLatLng)
                    }
                }
            })
        }
    }

    private fun setupLocationCallbacks() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation

                val cameraPos = CameraPosition.Builder()
                    .target(LatLng(location?.latitude!!, location.longitude))
                    .zoom(ZOOM_LEVEL)
                    .build()

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos))
            }
        }
    }

    private fun updateMapMarkerData(mk: Marker) {
        val index = viewModel.mMapMarkers.indexOfFirst { it.title == mk.title }
        index.let { viewModel.mMapMarkers.set(it, mk) }
    }

    private fun setLatLngFromMarkerData() {
        viewModel.mLatLng = ArrayList()//reset lat-lng
        viewModel.mMapMarkers.map {
            viewModel.mLatLng.add(
                LatLng(it.position.latitude, it.position.longitude)
            )
        }
    }

    private fun clearMapData(){
        removeMarkers()
        removeLatLng()
        removeAllPolylineData()
    }

    private fun addNewMarker(lng: LatLng) {
        if (viewModel.mMapMarkers.size>= 2) clearMapData()

        var title = getString(R.string.route_a)
        if(viewModel.mMapMarkers.size > 0) title = getString(R.string.route_b)

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(lng)
                .title(title)
                .draggable(true)
        )
        marker.showInfoWindow()

        viewModel.mMapMarkers.add(marker)
        viewModel.mLatLng.add(lng)

        if (viewModel.mLatLng.size == 2) drawPolylineRoute(viewModel.mLatLng)
    }

    private fun drawPolylineRoute(latLngList: ArrayList<LatLng>) {
        val options = PolylineOptions().width(10f).color(Color.BLUE)
        for (z in 0 until latLngList.size) {
            val point: LatLng = latLngList[z]
            options.add(point)
        }
        val polyline: Polyline = mMap.addPolyline(options)

        polyline.color = ContextCompat.getColor(this, R.color.primary)
        polyline.isClickable = true
        viewModel.mPolyLinesData.add(PolylineData(polyline))
    }

    private fun removeMarkers(){
        if(viewModel.mMapMarkers.size > 0){
            for(marker in viewModel.mMapMarkers){
                marker.remove()
            }
            viewModel.mMapMarkers.clear()
        }
    }

    private fun removeCarMarkers() {
        if(viewModel.mCarMarkers.size > 0){
            for(marker in viewModel.mCarMarkers){
                marker.remove()
            }
            viewModel.mCarMarkers.clear()
        }
    }

    private fun removeLatLng(){
        if(viewModel.mLatLng.size > 0){
            viewModel.mLatLng.clear()
            viewModel.mLatLng = ArrayList()
        }
    }

    private fun removeAllPolylineData(){
        if(viewModel.mPolyLinesData.size > 0){
            for(polylineData in viewModel.mPolyLinesData){
                polylineData.polyline?.remove()
            }
            viewModel.mPolyLinesData.clear()
            viewModel.mPolyLinesData = ArrayList()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_ID -> {
                var manualRequest = false
                var index = permissions.size - 1
                while (index >= 0) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        manualRequest = true
                    }
                    --index
                }
                initialize()
            }
            CALL_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toast(getString(R.string.call_permission_granted))
                } else {
                    toast(getString(R.string.call_permission_failed))
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GPS_REQUEST) {
                initializeMap()
                getDeviceLocation()
            }
        }else{
            if (requestCode == GPS_REQUEST) {
                showDialog(
                    this,
                    getString(R.string.enable_gps),
                    getString(R.string.enable_gps_message)
                ) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        }
    }

    private fun getDeviceLocation() {
        val mLocationRequestHighAccuracy = LocationRequest()
        mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
        mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL

        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ){
            return
        }

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequestHighAccuracy, mLocationCallback,
            Looper.myLooper()
        ) // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(this, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun addCarMarker(location: LatLng): Marker {
        removeCarMarkers()
        val carLocation  = bitmapDescriptorFromVector(R.drawable.car)

        val marker =  mMap.addMarker(
                MarkerOptions()
                        .icon(carLocation)
                        .position(location)
                        .flat(true)
        )

        viewModel.mCarMarkers.add(marker)
        return marker
    }

    private fun animateCar(map: GoogleMap, marker: Marker, destination: LatLng,
                           hideMarker: Boolean) {
        val start = SystemClock.uptimeMillis()
        val proj = map.projection
        val startPoint: Point = proj.toScreenLocation(marker.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration: Long = 10000
        val interpolator: Interpolator = LinearInterpolator()

        val handle = Handler(Looper.getMainLooper())
        handle.post(object : Runnable{
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * destination.longitude + (1 - t) * startLatLng.longitude
                val lat = t * destination.latitude + (1 - t) * startLatLng.latitude

                val newPosition = LatLng(lat, lng)
                marker.position = newPosition

                //calculate and set bearing
                val bearing = bearingBetweenLocations(newPosition, destination)
                marker.rotation = bearing.toFloat()

                //redraw polyline
                removeAllPolylineData()
                val newPosList = arrayListOf(newPosition,destination)
                drawPolylineRoute(newPosList)

                if (t < 1.0) handle.postDelayed(this, 30) //marker is not yet at destination
                else marker.isVisible = !hideMarker //marker is at destination, decide whether to keep it visible or hide it
            }
        })
    }

    private fun bearingBetweenLocations(latLng1: LatLng, latLng2: LatLng): Double {
        val PI = 3.14159
        val lat1: Double = latLng1.latitude * PI / 180
        val long1: Double = latLng1.longitude * PI / 180
        val lat2: Double = latLng2.latitude * PI / 180
        val long2: Double = latLng2.longitude * PI / 180
        val dLon = long2 - long1
        val y = sin(dLon) * cos(lat2)
        val x =
                cos(lat1) * sin(lat2) - (sin(lat1)
                        * cos(lat2) * cos(dLon))
        var brng = atan2(y, x)
        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        return brng
    }
}