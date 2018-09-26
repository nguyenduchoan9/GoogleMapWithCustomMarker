package solution.com.googlemap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private lateinit var googleMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private var mLastKnownLocation: Location? = null

    private var mPOIs = LinkedList<POI>()
    private val mLocationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                if (action == ProviderReceiver.Location_Update) {
                    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        updateLocationUI()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, IntentFilter(ProviderReceiver.Location_Update))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> updateLocationUI()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            googleMap = map
            googleMap.setOnCameraIdleListener(this)
            if (isGrantedLocationPermission()) {
                requestLocationPermission()
            } else {
                updateLocationUI()
            }
        }
    }

    override fun onCameraIdle() {
        val bounds = googleMap.projection.visibleRegion.latLngBounds
        val sw = "${bounds.southwest.latitude},${bounds.southwest.longitude}"
        val ne = "${bounds.northeast.latitude},${bounds.northeast.longitude}"
        fetchPOIs(sw, ne)
    }

    private fun isGrantedLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
    }

    private fun updateLocationUI() {
        googleMap?.let {
            try {
                if (isGrantedLocationPermission()) {
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    getDeviceLocation()
                } else {
                    googleMap.isMyLocationEnabled = false
                    googleMap.uiSettings.isMyLocationButtonEnabled = false
                    mLastKnownLocation = null
                    requestLocationPermission()
                }
            } catch (exception: SecurityException) {
                exception.printStackTrace()
            }
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mLastKnownLocation = task.result
                    mLastKnownLocation?.let {
                        val coordinate = LatLng(it.latitude, it.longitude)
                        val currentLocation = CameraUpdateFactory.newLatLng(coordinate)
                        googleMap.moveCamera(currentLocation)
                    }
                    if (null == mLastKnownLocation) {
                        Toast.makeText(this@MainActivity, "Please turn or your location", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (exception: SecurityException) {
            exception.printStackTrace()
        }

    }

    private fun fetchPOIs(sw: String, ne: String) {
        RetrofitUtils.retrofit.create(POIApi::class.java)
                .fetchPOI(sw, ne).enqueue(object : Callback<List<POI>> {
                    override fun onFailure(call: Call<List<POI>>?, t: Throwable?) {

                    }

                    override fun onResponse(call: Call<List<POI>>?, response: Response<List<POI>>?) {
                        response?.let {
                            if (response.isSuccessful)
                                displayPOIs(response.body()!!)
                        }
                    }
                })
    }

    private fun displayPOIs(POIs: List<POI>) {
        for (poi in POIs) {
            if (!isPOIExistent(poi)) {
                googleMap.addMarker(MarkerOptions()
                        .position(LatLng(poi.getLangtitudeInDouble(), poi.getLongtitudeInDouble()))
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromBitmap(createMarker(poi))))
                mPOIs.add(poi)
            }
        }
    }

    private fun createMarker(poi: POI): Bitmap {
        val vMarker = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.marker, null)
        val label = vMarker.findViewById(R.id.tvLabel) as TextView
        label.text = poi.name
        val ivMarker = vMarker.findViewById(R.id.ivMarker) as ImageView
        val icon = if (poi.isAirport()) R.drawable.ic_airport else R.drawable.ic_hotel
        ivMarker.setImageDrawable(ContextCompat.getDrawable(this, icon))
        return createDrawableFromView(vMarker)
    }

    private fun createDrawableFromView(view: View): Bitmap {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        view.draw(canvas)
        view.drawingCache.recycle()

        return bitmap
    }

    private fun isPOIExistent(newPoi: POI): Boolean {
        if (mPOIs.size == 0) return false
        for (poi in mPOIs) {
            if (poi.getLangtitudeInDouble() == newPoi.getLangtitudeInDouble()
                    && poi.getLongtitudeInDouble() == newPoi.getLongtitudeInDouble())
                return true
        }
        return false
    }
}