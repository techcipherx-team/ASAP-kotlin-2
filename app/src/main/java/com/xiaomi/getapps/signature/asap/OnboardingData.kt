package com.xiaomi.getapps.signature.asap

/**
 * Centralized data management for onboarding screens
 * To add new onboarding screens, simply add them to the screens list below
 */
object OnboardingData {

    /**
     * List of all onboarding screens
     * Add new screens here by adding OnboardingScreen(R.drawable.your_image)
     */
    val screens = listOf(
        OnboardingScreen(R.drawable.on_boarding_1),
        OnboardingScreen(R.drawable.on_boarding_3),
        OnboardingScreen(R.drawable.on_boarding_21),
        OnboardingScreen(R.drawable.on_boarding_4),
        OnboardingScreen(R.drawable.on_boarding_5),
        // Add more screens here as needed:
        // OnboardingScreen(R.drawable.on_boarding_3),
        // OnboardingScreen(R.drawable.on_boarding_4),
        // etc...
    )

    /**
     * Get the total number of onboarding screens
     */
    val totalScreens: Int
        get() = screens.size

    /**
     * Check if this is the last screen
     */
    fun isLastScreen(position: Int): Boolean {
        return position == screens.size - 1
    }

    /**
     * Get screen at specific position
     */
    fun getScreen(position: Int): OnboardingScreen? {
        return if (position in 0 until screens.size) {
            screens[position]
        } else {
            null
        }
    }
}

/**
 * Data class representing a single onboarding screen
 */
data class OnboardingScreen(
    val backgroundImage: Int
)
