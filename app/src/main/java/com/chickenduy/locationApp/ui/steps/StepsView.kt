package com.chickenduy.locationApp.ui.steps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R

class StepsView : AppCompatActivity() {

    private lateinit var stepsViewModel: StepsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stepsview)

        val recyclerView = findViewById<RecyclerView>(R.id.steps_recyclerView)
        val adapter = StepsListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        stepsViewModel = ViewModelProvider(this).get(StepsViewModel::class.java)

        stepsViewModel.allSteps.observe(this, Observer { Steps ->
            // Update the cached copy of the words in the adapter.
            Steps?.let { adapter.setList(it) }
        })
    }
}
