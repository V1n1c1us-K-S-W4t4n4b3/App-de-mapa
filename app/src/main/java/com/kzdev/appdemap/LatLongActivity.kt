package com.kzdev.appdemap

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.kzdev.appdemap.databinding.ActivityLatLongBinding

class LatLongActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLatLongBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLatLongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btBack.setOnClickListener { finish() }

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        Log.d("LatLongActivity", "Latitude: $latitude, Longitude: $longitude")

        binding.latitude.text = latitude.toString()
        binding.longitude.text = longitude.toString()

    }
}