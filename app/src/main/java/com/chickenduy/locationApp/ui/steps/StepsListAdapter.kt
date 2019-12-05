package com.chickenduy.locationApp.ui.steps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.data.database.entity.Steps
import java.util.*

class StepsListAdapter internal constructor(context: Context) : RecyclerView.Adapter<StepsListAdapter.StepsViewHolder>()  {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var stepsList = emptyList<Steps>() // Cached copy of words

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item_row, parent, false)
        return StepsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        val current = stepsList[position]
        holder.itemDate.text = "${Date(current.timestamp)}"
        holder.itemDescription.text = "steps: ${current.steps}"
    }

    internal fun setList(StepsList: List<Steps>) {
        this.stepsList = StepsList
        notifyDataSetChanged()
    }

    override fun getItemCount() = stepsList.size

    inner class StepsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemDate: TextView = itemView.findViewById(R.id.itemDate)
        val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
    }
}