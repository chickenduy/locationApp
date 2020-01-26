package com.chickenduy.locationApp.ui.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.data.database.entity.ActivitiesDetailed
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesListAdapter internal constructor(context: Context) :
    RecyclerView.Adapter<ActivitiesListAdapter.ActivityViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    //    private var activitiesList = emptyList<Activities>() // Cached copy of words
    private var activitiesDetailedList = emptyList<ActivitiesDetailed>()
    private val format = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item_row, parent, false)
        return ActivityViewHolder(itemView)
    }

//    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
//        val current = activitiesList[position]
//        holder.itemDate.text = "${Date(current.timestamp)}"
//        holder.itemDescription.text = "transition: ${if(current.enter == 0) "enter" else "leave\n"} activity: ${
//        when(current.type) {
//            0 -> "vehicle"
//            1 -> "on bicycle"
//            2 -> "on foot"
//            3 -> "still"
//            4 -> "unknown"
//            5 -> "tilting"
//            6 -> "N/A"
//            7 -> "walking"
//            8 -> "running"
//            else -> "something went wrong"
//        }
//        }"
//    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val current = activitiesDetailedList[position]
        holder.itemDate.text =
            "${format.format(Date(current.start))} - ${format.format(Date(current.end))}"
        holder.itemDescription.text = "activity: ${
        when (current.type) {
            0 -> "vehicle"
            1 -> "on bicycle"
            2 -> "on foot"
            3 -> "still"
            4 -> "unknown"
            5 -> "tilting"
            6 -> "N/A"
            7 -> "walking"
            8 -> "running"
            else -> "something went wrong"
        }
        }"
    }

//    internal fun setList(activitiesList: List<Activities>) {
//        this.activitiesList = activitiesList
//        notifyDataSetChanged()
//    }

    internal fun setList(activitiesDetailedList: List<ActivitiesDetailed>) {
        this.activitiesDetailedList = activitiesDetailedList
        notifyDataSetChanged()
    }

//    override fun getItemCount() = activitiesList.size

    override fun getItemCount() = activitiesDetailedList.size

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemDate: TextView = itemView.findViewById(R.id.itemDate)
        val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
    }
}