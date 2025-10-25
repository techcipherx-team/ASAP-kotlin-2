package com.xiaomi.getapps.signature.asap

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.getapps.signature.asap.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit = {}
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvCategoryIcon.text = category.icon
            binding.tvCategoryName.text = category.name

            val context = binding.root.context

            // Change background based on selection
            if (category.isSelected) {
                binding.root.setBackgroundResource(R.drawable.category_background) // black background

                // Set text and icon color to white
                binding.tvCategoryIcon.setTextColor(Color.WHITE)
                binding.tvCategoryName.setTextColor(Color.WHITE)
            } else {
                binding.root.setBackgroundResource(R.drawable.category_background_unselected) // white background

                // Set text and icon color to black
                binding.tvCategoryIcon.setTextColor(Color.BLACK)
                binding.tvCategoryName.setTextColor(Color.BLACK)
            }

            // Handle click
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}
