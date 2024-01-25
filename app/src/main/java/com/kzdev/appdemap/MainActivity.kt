package com.kzdev.appdemap

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingMap: ActivityMapBinding
    private lateinit var bindingMapWithSearch: ActivityMapWithSearchBinding
    private val customAdapter = CustomAdapter(mutableListOf()){
        onItemClickListener(it)
    }


    private val viewModel: GeocoderViewModel by viewModels { GeocoderViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMapWithSearch = ActivityMapWithSearchBinding.inflate(layoutInflater)
        setContentView(bindingMapWithSearch.root)
        configMap()
        setupRv()
        setupView()
    }

    override fun onStart() {
        super.onStart()
        observer()
    }

    private fun setupView() {

        bindingMapWithSearch.searchBar.textView.maxLines = 2

        bindingMapWithSearch.btOk.setOnClickListener {
            val map = bindingMapWithSearch.map
            val intent = imputValues(map.mapCenter.latitude, map.mapCenter.longitude)
            startActivity(intent)
        }

        bindingMapWithSearch.searchView.editText.setOnEditorActionListener { _, _, _ ->
            bindingMapWithSearch.searchBar.setText(bindingMapWithSearch.searchView.text)
            bindingMapWithSearch.searchView.hide()
            false
        }

        bindingMapWithSearch.searchView.editText.addTextChangedListener {
            bindingMapWithSearch.progress.visibility = View.VISIBLE
            viewModel.findAddressByText(it.toString())
        }
    }

    private fun setupRv() {
        bindingMapWithSearch.rvAddress.adapter = customAdapter
        bindingMapWithSearch.rvAddress.layoutManager = LinearLayoutManager(this)

    }

    private fun configMap() {
        val map = bindingMapWithSearch.map
        map.setMultiTouchControls(true)
        Configuration.getInstance().userAgentValue = BuildConfig.LIBRARY_PACKAGE_NAME
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(
                this, ACCESS_FINE_LOCATION
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
        }
        val myMapListener = MyMapListener {
            viewModel.findAddress(map.mapCenter.latitude, map.mapCenter.longitude)
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

    private fun onItemClickListener(address: Address) {
        bindingMapWithSearch.searchBar.setText(address.getAddressLine(0))
        bindingMapWithSearch.searchView.hide()

        val map = bindingMapWithSearch.map

        map.controller.animateTo( GeoPoint(address.latitude, address.longitude))

    }

    private fun observer() {
        viewModel.address.observe(this) {
            bindingMapWithSearch.searchBar.setText(it)
            //bindingMap.textView.text = it
        }
        viewModel.listAddress.observe(this) {
            Log.i("Test", it?.size.toString())
            if (it != null) {
                customAdapter.updateList(it)
            }
            bindingMapWithSearch.progress.visibility = View.INVISIBLE
        }
    }
}
