package com.kzdev.appdemap

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.kzdev.appdemap.databinding.ActivityMainBinding
import com.kzdev.appdemap.databinding.ActivityMapBinding
import com.kzdev.appdemap.databinding.ActivityMapWithSearchBinding
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingMap: ActivityMapBinding
    private lateinit var bindingMapWithSearch: ActivityMapWithSearchBinding

    private val viewModel: GeocoderViewModel by viewModels { GeocoderViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMapWithSearch = ActivityMapWithSearchBinding.inflate(layoutInflater)
        setContentView(bindingMapWithSearch.root)
        bindingMapWithSearch.searchBar.textView.maxLines = 2

        configMap()

        bindingMapWithSearch.searchView
            .editText
            .setOnEditorActionListener { _, _, _ ->
                bindingMapWithSearch.searchBar.setText(bindingMapWithSearch.searchView.text)
                bindingMapWithSearch.searchView.hide()
                false
            }

        bindingMapWithSearch.searchView
            .editText
            .addTextChangedListener {
                bindingMapWithSearch.progress.visibility = View.VISIBLE
                viewModel.findAddressByText(it.toString())
            }
    }

    override fun onStart() {
        super.onStart()
        observer()
    }

    private fun configMap() {
        val map = bindingMapWithSearch.map
        map.setMultiTouchControls(true)
        Configuration.getInstance().userAgentValue = BuildConfig.LIBRARY_PACKAGE_NAME

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            // Obtenha a última localização conhecida
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            // Verifique se a localização é válida

            // Use a localização atual como ponto inicial
            val startPoint =
                lastKnownLocation?.let { GeoPoint(it.latitude, lastKnownLocation.longitude) }

            // Restante do código permanece o mesmo
            val mapController = map.controller
            mapController.setZoom(20.0)
            mapController.setCenter(startPoint)
            val startMarker = Marker(map)
            startMarker.position = startPoint
            startMarker.isDraggable = true
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            map.overlays.add(startMarker)
            startMarker.title = "Localização Atual"
            map.invalidate()
        }

        val myMapListener = MyMapListener {
            viewModel.findAddress(map.mapCenter.latitude, map.mapCenter.longitude)
            val intent = imputValues(map.mapCenter.latitude, map.mapCenter.longitude)
            bindingMapWithSearch.btOk.setOnClickListener {
                startActivity(intent)
            }
        }

        val myDelayedMapListener = MyDelayedMapListener(myMapListener, 50) {
            bindingMapWithSearch.searchBar.setText("Carregando...")
        }
        map.addMapListener(myDelayedMapListener)

    }

    private fun imputValues(latitude: Double, longitude: Double): Intent {

        val intent = Intent(this, LatLongActivity::class.java)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        return intent
    }

    private fun observer() {

        val customAdapter = CustomAdapter(mutableListOf())
        bindingMapWithSearch.rvAddress.adapter = customAdapter
        bindingMapWithSearch.rvAddress.layoutManager = LinearLayoutManager(this)

        viewModel.address.observe(this) {
            bindingMapWithSearch.searchBar.setText(it)
            //bindingMap.textView.text = it
        }

        viewModel.geoPoint.observe(this) {
            Log.i("Test", it?.size.toString())
            val listString = it?.map { a -> a.getAddressLine(0) }
            Log.i("Test", listString.toString())
            listString?.let { it1 -> customAdapter.updateList(it1) }
            bindingMapWithSearch.progress.visibility = View.INVISIBLE
        }
    }
}