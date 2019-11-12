package com.chickenduy.locationApp.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R

class ActivitiesView : AppCompatActivity(){

    private lateinit var activitiesViewModel: ActivitiesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activitiesview)

        val recyclerView = findViewById<RecyclerView>(R.id.activities_recyclerview)
        val adapter = ActivitiesListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        activitiesViewModel = ViewModelProvider(this).get(ActivitiesViewModel::class.java)

        activitiesViewModel.allActivities.observe(this, Observer { activities ->
            // Update the cached copy of the words in the adapter.
            activities?.let { adapter.setList(it) }
        })

    }
}