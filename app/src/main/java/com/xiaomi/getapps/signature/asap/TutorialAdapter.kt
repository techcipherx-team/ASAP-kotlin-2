package com.xiaomi.getapps.signature.asap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.getapps.signature.asap.databinding.ItemTutorialBinding

class TutorialAdapter(
    private val tutorials: MutableList<Tutorial>,
    private val onTutorialClick: (Tutorial) -> Unit = {}
) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val binding = ItemTutorialBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TutorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(tutorials[position])
    }

    override fun getItemCount(): Int = tutorials.size

    inner class TutorialViewHolder(
        private val binding: ItemTutorialBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tutorial: Tutorial) {
            // Set only the image (since all text/layouts are removed)
            binding.ivTutorialImage.setImageResource(tutorial.imageResource)

            // Handle click
            binding.root.setOnClickListener {
                onTutorialClick(tutorial)
            }
        }
    }

    /**
     * Update the tutorials list
     */
    fun updateTutorials(newTutorials: List<Tutorial>) {
        tutorials.clear()
        tutorials.addAll(newTutorials)
        notifyDataSetChanged()
    }
}
