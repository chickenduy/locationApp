package com.chickenduy.locationApp.ui.gps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.data.database.entity.GPS
import java.util.*

class GPSListAdapter internal constructor(context: Context) : RecyclerView.Adapter<GPSListAdapter.GPSViewHolder>()  {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var gpsList = emptyList<GPS>() // Cached copy of words

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GPSViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item_row, parent, false)
        return GPSViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GPSViewHolder, position: Int) {
        val current = gpsList[position]
        holder.itemDate.text = "${Date(current.timestamp)}"
        holder.itemDescription.text = "lat:${current.latitude} lon:${current.longitude}"
    }

    internal fun setWords(gpsList: List<GPS>) {
        this.gpsList = gpsList
        notifyDataSetChanged()
    }

    override fun getItemCount() = gpsList.size

    inner class GPSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemDate: TextView = itemView.findViewById(R.id.itemDate)
        val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
    }
}