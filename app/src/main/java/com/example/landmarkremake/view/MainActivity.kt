package com.example.landmarkremake.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.landmarkremake.R
import com.example.landmarkremake.adapter.AdapterSearch
import com.example.landmarkremake.callback.OnGetListNoteListener
import com.example.landmarkremake.callback.OnSaveNoteListener
import com.example.landmarkremake.databinding.ActivityMainBinding
import com.example.landmarkremake.model.Note
import com.example.landmarkremake.utils.DialogUtils
import com.example.landmarkremake.viewmodel.NoteViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnSaveNoteListener,
    OnGetListNoteListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private val permissionCode = 101
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentMarker: Marker? = null
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var adapterSearch: AdapterSearch
    private var fusedLocationProviderClientInstance = false
    private var moveCameraCurrentLocationFirst = true
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    override fun onResume() {
        super.onResume()
        if (isLocationEnabled()) {
            if (fusedLocationProviderClientInstance) {
                requestLocationUpdates()
            }
        } else {
            showLocationSettingsDialog()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showLocationSettingsDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.apply {
            setTitle("Location is not enabled")
            setMessage("Please enable location to use app")
            setCancelable(false)
            setPositiveButton("Enable location") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            setNegativeButton("Cancel") { _, _ ->
                finish()
            }
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun init() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        val supportMapFragment =
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)
        supportMapFragment?.getMapAsync(this@MainActivity)
        bindEvent()
    }

    private fun bindEvent() {
        binding.actMainSvAddress.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapterSearch.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapterSearch.filter.filter(newText)
                return true
            }
        })

        binding.actMainSvAddress.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.actMainRlSearchAddress.visibility = View.VISIBLE
            } else {
                binding.actMainRlSearchAddress.visibility = View.GONE
            }
        }

        binding.actMainButtonMoveCurrentLocation.setOnClickListener {
            moveCameraToCurrentLocation()
        }

        binding.map.setOnClickListener {
            binding.actMainSvAddress.clearFocus()
        }
    }

    private fun moveCameraToCurrentLocation() {
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> {
                requestLocationUpdates()
            }
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback, Looper.myLooper()
            )
            googleMap.isMyLocationEnabled = true
        } else {
            checkLocationPermission()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        googleMap.uiSettings.isMyLocationButtonEnabled = true
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(100).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val locationList = locationResult.locations
                if (locationList.size > 0) {
                    val location = locationList[locationList.size - 1]
                    currentLocation = location
                    currentMarker?.remove()
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val markerOptions = MarkerOptions().position(latLng).title("Current Location")
                    currentMarker = p0.addMarker(markerOptions)

                    if (moveCameraCurrentLocationFirst) {
                        p0.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                        p0.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        moveCameraCurrentLocationFirst = false
                    }
                }
            }
        }
        fusedLocationProviderClientInstance = true
        requestLocationUpdates()

        p0.setOnMarkerClickListener(GoogleMap.OnMarkerClickListener {
            if (it == currentMarker) {
                DialogUtils.createNote(this, "", "", true) { note ->
                    GlobalScope.launch(Dispatchers.Main) {
                        noteViewModel.saveNote(
                            Note(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                note.note,
                                note.username
                            ), this@MainActivity
                        )
                    }
                }
            } else {
                val note = it.tag as Note
                DialogUtils.createNote(this, note.username, note.note, false) {
                }
            }
            return@OnMarkerClickListener true
        })

        GlobalScope.launch(Dispatchers.Main) {
            noteViewModel.getData(this@MainActivity)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            permissionCode
                        )
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    permissionCode
                )
            }
        }
    }

    private fun addMarker(note: Note) {
        val latLng = LatLng(note.latitude, note.longitude)
        val markerOptions =
            MarkerOptions().position(latLng).snippet(note.note).title(note.note).icon(
                BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_location
                    )
                )
            )
        val oldMarker = googleMap.addMarker(markerOptions)
        oldMarker?.tag = note
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSaveNoteSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        GlobalScope.launch(Dispatchers.Main) {
            noteViewModel.getData(this@MainActivity)
        }
    }

    override fun onSaveNoteError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onGetNoteSuccess(data: List<Note>) {
        for (item in data) {
            addMarker(item)
        }
        adapterSearch = AdapterSearch()
        adapterSearch.setData(data)
        adapterSearch.onItemClick {
            binding.actMainSvAddress.setQuery(it.note, false)
            binding.actMainSvAddress.clearFocus()
            val latLng = LatLng(it.latitude, it.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 25f))
        }
        binding.actMainRlSearchAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.actMainRlSearchAddress.adapter = adapterSearch
    }

    override fun onGetNoteError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}