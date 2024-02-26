package com.example.landmarkremake.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.compose.ui.text.toLowerCase
import androidx.recyclerview.widget.RecyclerView
import com.example.landmarkremake.databinding.ItemSearchBinding
import com.example.landmarkremake.model.Note
import java.util.Locale

class AdapterSearch():RecyclerView.Adapter<AdapterSearch.ViewHolder>(), Filterable{
    private var filteredList:List<Note> = listOf()
    private var listData:List<Note> = listOf()
    private var onClick:(note:Note) -> Unit = {}
    class ViewHolder(val itemSearchBinding:ItemSearchBinding):RecyclerView.ViewHolder(itemSearchBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemSearchBinding = ItemSearchBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(itemSearchBinding)
    }

    fun setData(listData:List<Note>){
        this.filteredList = listData
        this.listData = listData
    }

    fun onItemClick(onClick:(note:Note)->Unit){
        this.onClick = onClick
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position>=0 && position <filteredList.size){
            val currentItem = filteredList[position]
            holder.itemSearchBinding.itemSearchTvName.text = currentItem.note
            holder.itemView.setOnClickListener {
                onClick.invoke(currentItem)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchText = constraint.toString().lowercase(Locale.getDefault())
                filteredList = if (searchText.isEmpty()) {
                    listData
                } else {
                    listData.filter { it.note.lowercase(Locale.getDefault()).contains(searchText) }
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<Note>
                notifyDataSetChanged()
            }
        }
    }


}