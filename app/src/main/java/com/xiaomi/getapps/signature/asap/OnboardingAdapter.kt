package com.xiaomi.getapps.signature.asap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.getapps.signature.asap.databinding.ItemOnboardingBinding

class OnboardingAdapter(
    private val onboardingScreens: List<OnboardingScreen>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingScreens[position])
    }

    override fun getItemCount(): Int = onboardingScreens.size

    class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(screen: OnboardingScreen) {
            binding.backgroundImage.setImageResource(screen.backgroundImage)
        }
    }
}
