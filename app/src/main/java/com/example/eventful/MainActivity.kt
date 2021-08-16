package com.example.eventful

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.eventful.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import java.util.*

class EventDetails {
    lateinit var title: String
    lateinit var address: String
    lateinit var longitude: String
    lateinit var latitude: String
}

lateinit var CITY: String
lateinit var temp: String
lateinit var conditions: String
lateinit var iconURL: String
var listOfEvents = mutableListOf<EventDetails>()

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var API: String = "991c4746ce85b4c70a462313962cd22a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            val getLocationJob = async { getLocation(this@MainActivity) }
            CITY = getLocationJob.await()


//        main()


            // Instantiate the RequestQueue.
            val queue = Volley.newRequestQueue(this@MainActivity)
            val weatherurl =
                "https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=imperial&appid=$API"

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, weatherurl, null,
                { response ->
                    val main = response.getJSONObject("main")
                    temp = main.getString("temp") + "\u2103"
                    val weather = response.getJSONArray("weather")
                    conditions = weather.getJSONObject(0).getString("main")
                    val icon = weather.getJSONObject(0).getString("icon")
                    iconURL = "https://openweathermap.org/img/w/$icon.png"
                    Log.d("response", "Post API response = $response")
                },
                { error -> Log.d("error", "Post API error = $error") })
            queue.add(jsonObjectRequest)

            val textView = findViewById<TextView>(R.id.text)
            // ...

            // Instantiate the RequestQueue.
            val city = "Denver"
            val url =
                "https://app.ticketmaster.com/discovery/v2/events.json?city=$city&apikey=t3Pw9IXSSkl3TrReE9v2nrm238YfhoXb"


            // Request a string response from the provided URL.
            val jsonRequest = JsonObjectRequest(
                Request.Method.GET, url, null,
                { response ->
                    // Display the first 500 characters of the response string.
                    val jsonObject = response.getJSONObject("_embedded")
                    val eventsArr = jsonObject.getJSONArray("events")

                    for (i in 0 until eventsArr.length()) {
                        val event = eventsArr.getJSONObject(i)
                        val eventInstance = EventDetails()
                        val addressObject =
                            event.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0)


                        eventInstance.title = event.getString("name")
                        eventInstance.address =
                            addressObject.getJSONObject("address").optString("line1")
                        val longitude =
                            addressObject.getJSONObject("location").optString("longitude")
                        val latitude = addressObject.getJSONObject("location").optString("latitude")
                        eventInstance.longitude = longitude
                        eventInstance.latitude = latitude
                        Log.d("Latitude", "Latitude = $latitude")
                        Log.d("Longitude", "Longitude = $longitude")

                        listOfEvents.add(eventInstance)
                    }
                },
                { error -> Log.d("error", error.toString()) })
            queue.add(jsonRequest)


        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    fun main() = runBlocking {
        val job = launch {
            val getLocationJob = async { getLocation(this@MainActivity) }
            CITY = getLocationJob.await()
        }
        job.join()
    }

    private suspend fun getLocation(context: Context): String = withContext(Dispatchers.IO) {

        var lat: Double
        var lon: Double
        val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
        var addressList: List<Address?>
        var address = "Dallas"

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                        ) {fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                if (location != null) {
                                    lat = location.latitude
                                    lon = location.longitude
                                    addressList = geocoder.getFromLocation(lat, lon, 1)
                                    address = addressList[0]!!.locality
                                    Log.d("address", "address = $addressList")
                                    Log.d("address", "address = $address")
                                    CITY = address
                                }
                            }
                        }

                    }
                }

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        Log.d("address outside of call", "address = $address")
        address
    }

}