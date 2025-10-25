package com.xiaomi.getapps.signature.asap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.getapps.signature.asap.databinding.ItemBrandBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class BrandAdapter(
    private val brands: MutableList<Brand>,
    private val onEmailClick: (Brand) -> Unit = {},
    private val onFavoriteClick: (Brand) -> Unit = {},
    private val onBrandLogoClick: (Brand) -> Unit = {}
) : RecyclerView.Adapter<BrandAdapter.BrandViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        val binding = ItemBrandBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BrandViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BrandViewHolder, position: Int) {
        holder.bind(brands[position])
    }

    override fun getItemCount(): Int = brands.size

    inner class BrandViewHolder(
        private val binding: ItemBrandBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(brand: Brand) {
            binding.tvBrandName.text = brand.name
            binding.tvBrandCategory.text = brand.category
            
            // Load image from URL if available, otherwise use resource
            if (brand.logoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(brand.logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_launcher_foreground) // Default placeholder
                    .error(R.drawable.ic_launcher_foreground) // Error fallback
                    .into(binding.ivBrandLogo)
            } else if (brand.logoResource != 0) {
                binding.ivBrandLogo.setImageResource(brand.logoResource)
            } else {
                binding.ivBrandLogo.setImageResource(R.drawable.ic_launcher_foreground)
            }
            
            // Update favorite icon
            val favoriteIcon = if (brand.isFavorite) {
                R.drawable.red_heart
            } else {
                R.drawable.plane_heart
            }
            binding.ivFavorite.setImageResource(favoriteIcon)
            
            // Set click listeners
            binding.btnEmail.setOnClickListener {
                onEmailClick(brand)
            }
            
            binding.ivFavorite.setOnClickListener {
                brand.isFavorite = !brand.isFavorite
                notifyItemChanged(adapterPosition)
                onFavoriteClick(brand)
            }
            
            // Add click listener for brand logo to open Instagram popup
            binding.ivBrandLogo.setOnClickListener {
                android.util.Log.d("BrandAdapter", "Brand logo clicked: ${brand.name}")
                onBrandLogoClick(brand)
            }
        }
    }
    
    /**
     * Update the brands list (useful for search functionality)
     */
    fun updateBrands(newBrands: List<Brand>) {
        brands.clear()
        brands.addAll(newBrands)
        notifyDataSetChanged()
    }
}
