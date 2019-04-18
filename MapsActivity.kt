package com.reuniware.gpstosms

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.reuniware.gpstosms.Utils.Companion.getLastKnownAddress
import com.reuniware.gpstosms.Utils.Companion.getLastKnownLocation
import com.reuniware.gpstosms.Utils.Companion.getLastKnownLocationDate
import com.reuniware.gpstosms.Utils.Companion.storeLastKnownAddress
import com.reuniware.gpstosms.Utils.Companion.storeLastKnownLocation
import com.reuniware.gpstosms.Utils.Companion.storeLastKnownLocationDate
import kotlinx.android.synthetic.main.activity_maps.*
import java.text.DateFormat
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    lateinit var mAdView : AdView

    private lateinit var mMap: GoogleMap
    //private lateinit var database: DatabaseReference
    //val TAG = "GpsToSms"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorAccelerometer != null) sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //FirebaseApp.initializeApp(this)
        btnStartLoc.isEnabled = true
        btnStopLoc.isEnabled = false
        checkAutoZoom.isChecked = true

        initMap()
        initUI()

        btnStartLoc.setOnClickListener {
            startLoc()
        }

        btnStopLoc.setOnClickListener {
            stopLoc()
        }

        btnSettings.setOnClickListener {
            val addRdv = Intent(this@MapsActivity, SettingsActivity::class.java)
            startActivity(addRdv)
        }
    }

    //debut test
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            //Log.d(TAG, "onSensorChanger ACCELEROMETER")
            val accelX = event.values[0]
            val accelY = event.values[1]
            val accelZ = event.values[2]

            //textViewAdView.text = "aX=${accelX.toString()}\r\naY=${accelY.toString()}\r\naZ=${accelZ.toString()}"
        }
    }

    private fun updateCamera(bearing : Float) {
        val oldPos = mMap.getCameraPosition()
        val pos = CameraPosition.builder(oldPos).bearing(bearing).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos))

        //textViewAdView.text = "Bearing=$bearing"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    // fin test

    private fun initUI() {
        updateTextViewAddress()
    }


    private fun startLoc() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    //showDialog(this@MapsActivity, TAG, "last know loc = ${location?.latitude} + ${location?.longitude}")
                    if (location != null){
                        lastKnowLoc = LatLng(location.latitude, location.longitude)
                        addMarkerToLocation(lastKnowLoc)
                    }
                }
            val locationRequest = LocationRequest()
            locationRequest.interval = 10000
            locationRequest.fastestInterval = 10000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (location in locationResult.locations){
                        //showDialog(this@MapsActivity, "locationResult", "location=${location.latitude};${location.longitude}")
                        addMarkerToLocation(LatLng(location.latitude, location.longitude))
                        val speed = location.speed
                        updateCamera(location.bearing)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            btnStartLoc.isEnabled = false
            btnStopLoc.isEnabled = true
        }catch (e:SecurityException){}

    }

    private fun stopLoc() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)

            btnStartLoc.isEnabled = true
            btnStopLoc.isEnabled = false
        }
        catch (e: Exception){}
    }

    override fun onResume() {
        updateTextViewAddress()
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                initMap()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                showDialog(this@MapsActivity, TAG, response?.error?.message.toString(), true)
            }
        }*/
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.*/
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnowLoc = LatLng(0.0, 0.0)
    private lateinit var locationCallback: LocationCallback
    private var maxZoom = 18f
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        try {
            mMap.isMyLocationEnabled = true
        }catch (e: SecurityException) {
        }

    }

    fun addMarkerToLocation(latLng: LatLng) {

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        if (checkAutoZoom.isChecked){
            if (mMap.maxZoomLevel>=maxZoom)
                mMap.animateCamera(CameraUpdateFactory.zoomTo(maxZoom))
            else
                mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.maxZoomLevel))
        }

        storeLastKnownLocation(applicationContext, latLng)

        val date = Date()
        val strDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)
        storeLastKnownLocationDate(applicationContext, strDate)

        try {
            val geoCoder = Geocoder(this, Locale.getDefault())
            val addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.count() > 0) {
                val fullAddress = addresses[0].getAddressLine(0)
                storeLastKnownAddress(applicationContext, fullAddress)
            }
        } catch(e: Throwable) {
            //textViewAddress.text = strDate + " - ${latLng.latitude}, ${latLng.longitude}" + "\r\n " + "Adresse non disponible" + " "
        }

        updateTextViewAddress()
    }

    private fun updateTextViewAddress() {
        val latLng = getLastKnownLocation(applicationContext)
        val address = getLastKnownAddress(applicationContext)
        if (latLng.latitude == 0.0 && latLng.longitude == 0.0 && getLastKnownAddress(applicationContext) == "")
            textViewAddress.visibility = View.INVISIBLE
        else {
            textViewAddress.visibility = View.VISIBLE
            textViewAddress.text =
                getLastKnownLocationDate(applicationContext) + " - ${latLng.latitude}, ${latLng.longitude}" + "\r\n " + address + " "
        }
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /*private fun signIn() {
        // Choose authentication providers
        val providers = arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build()/*,AuthUI.IdpConfig.GoogleBuilder().build(),AuthUI.IdpConfig.FacebookBuilder().build(),AuthUI.IdpConfig.TwitterBuilder().build()*/)
        // Create and launch sign-in intent
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(),RC_SIGN_IN)
    }*/

    /*private fun signOut() {
        // Pour déconnecter l'utilisateur
        FirebaseAuth.getInstance().signOut()
    }*/

    /*private fun writeDataToFirestore() {
        val db = FirebaseFirestore.getInstance()
        // Create a new user with a first and last name
        val user1 = HashMap<String, Any>()
        user1["first"] = "Ada"
        user1["last"] = "Lovelace"
        user1["born"] = 1815

        // Add a new document with a generated ID
        db.collection("users")
            .add(user1)
            .addOnSuccessListener { documentReference ->
                //Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
                showDialog(this, TAG, "DocumentSnapshot added with ID: " + documentReference.id, false)
            }
            .addOnFailureListener { e ->
                //Log.w(TAG, "Error adding document", e)
                showDialog(this, TAG, "Error adding document" + e.message, false)
            }

        // Create a new user with a first, middle, and last name
        val user2 = HashMap<String, Any>()
        user2["first"] = "Alan"
        user2["middle"] = "Mathison"
        user2["last"] = "Turring"
        user2["born"] = 1912

        // Add a new document with a generated ID
        db.collection("users")
            .add(user2)
            .addOnSuccessListener { documentReference ->
                //Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
                showDialog(this, TAG, "DocumentSnapshot added with ID: " + documentReference.id, false)
            }
            .addOnFailureListener { e ->
                //Log.w(TAG, "Error adding document", e)
                showDialog(this, TAG, "Error adding document" + e.message, false)
            }

    }*/



    private lateinit var alertDialogBuilder : android.app.AlertDialog.Builder

    // Dialog d'affichage de message Ok puis redirection sur liste des comptes
    private fun showDialog(context: Context, title: String, message: String, closeAppOnClickOnOk: Boolean) {
        alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, _ ->
            dialog.cancel()
            if (closeAppOnClickOnOk){
                finish()
            }
        } )
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    /*companion object {
        private const val RC_SIGN_IN = 123
    }*/

}
