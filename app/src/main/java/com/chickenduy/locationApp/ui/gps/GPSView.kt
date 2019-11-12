package com.chickenduy.locationApp.ui.gps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R

class GPSView : AppCompatActivity() {

    private lateinit var gpsViewModel: GPSViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gpsview)

        val recyclerView = findViewById<RecyclerView>(R.id.gps_recyclerView)
        val adapter = GPSListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        gpsViewModel = ViewModelProvider(this).get(GPSViewModel::class.java)

        gpsViewModel.allGPS.observe(this, Observer { gps ->
            // Update the cached copy of the words in the adapter.
            gps?.let { adapter.setWords(it) }
        })
    }
}
