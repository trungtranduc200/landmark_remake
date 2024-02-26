package com.example.landmarkremake

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.landmarkremake.adapter.AdapterSearch
import com.example.landmarkremake.callback.OnGetListNoteListener
import com.example.landmarkremake.callback.OnSaveNoteListener
import com.example.landmarkremake.databinding.ActivityMainBinding
import com.example.landmarkremake.model.Note
import com.example.landmarkremake.repository.NoteRepository
import com.example.landmarkremake.utils.DialogUtils
import com.example.landmarkremake.viewmodel.NoteViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        getCurrentLocation()

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

        binding.actMainSvAddress.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus){
                binding.actMainRlSearchAddress.visibility = View.VISIBLE
            }else{
                binding.actMainRlSearchAddress.visibility = View.GONE
            }
        }

        binding.actMainButtonMoveCurrentLocation.setOnClickListener {
            moveCameraToCurrentLocation()
        }

        binding.map.setOnClickListener{
            binding.actMainSvAddress.clearFocus()
        }
    }

    private fun moveCameraToCurrentLocation() {
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun getCurrentLocation(){
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                permissionCode
            )
            return
        }
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                Toast.makeText(this, currentLocation.latitude.toString() + "" + currentLocation.longitude.toString(), Toast.LENGTH_SHORT).show();
                val supportMapFragment =
                    (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)
                supportMapFragment?.getMapAsync(this@MainActivity)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        googleMap.uiSettings.isMyLocationButtonEnabled = true

        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val markerOptions = MarkerOptions().position(latLng).title("Current Location")
        p0.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        p0.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        currentMarker = p0.addMarker(markerOptions)

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

    override fun onSaveNoteSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            binding.actMainSvAddress.setQuery(it.note,false)
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