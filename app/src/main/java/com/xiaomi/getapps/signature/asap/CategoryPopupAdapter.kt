package com.xiaomi.getapps.signature.asap

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.getapps.signature.asap.databinding.ItemPopupCategoryBinding

class CategoryPopupAdapter(
    private val categories: MutableList<PopupCategory>,
    private val onCategoryClick: (PopupCategory) -> Unit = {}
) : RecyclerView.Adapter<CategoryPopupAdapter.PopupCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupCategoryViewHolder {
        val binding = ItemPopupCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PopupCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopupCategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class PopupCategoryViewHolder(
        private val binding: ItemPopupCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: PopupCategory) {
            val context = binding.root.context
            binding.btnPopupCategory.text = category.name

            // Set background based on selection
            if (category.isSelected) {
                // Use button_background.jpg image for selected state
                val imageDrawable = ContextCompat.getDrawable(context, MainPageData.SELECTED_BUTTON_BACKGROUND)
                binding.cardButton.background = imageDrawable
                binding.btnPopupCategory.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                // Use light grey background for unselected state
                binding.cardButton.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                binding.btnPopupCategory.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }

            // Handle click
            binding.btnPopupCategory.setOnClickListener {
                categories.forEach { it.isSelected = false }
                category.isSelected = true
                notifyDataSetChanged()
                onCategoryClick(category)
            }
        }
    }
}
