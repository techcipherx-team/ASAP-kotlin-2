package com.xiaomi.getapps.signature.asap

/**
 * Easy-to-modify data structure for the main page
 * To add new categories or brands, simply update the lists below
 */
object MainPageData {
    
    /**
     * Categories for the horizontal scrollable list
     * To add new categories, add them to this list
     */
    val categories = listOf(
        Category("🔥", "Popular", true), // true = selected
        Category("❤️", "Favorites", false),
        Category("⭐", "Spotlight", false),
        Category("⭐", "Tutorials", false),
        // Add more categories here:
        // Category("🎯", "Trending", false),
        // Category("💎", "Premium", false),
    )
    
    /**
     * Brands for the vertical scrollable list
     * To add new brands, add them to this list
     */
    val brands = listOf(
        Brand(
            name = "Skims",
            category = "Most Popular",
            logoResource = R.drawable.skims, // Add your brand logo here
            isFavorite = false
        ),
        Brand(
            name = "Fenty Beauty",
            category = "Most Popular", 
            logoResource = R.drawable.fenty, // Add your brand logo here
            isFavorite = true
        ),
        Brand(
            name = "Kylie Cosmetics",
            category = "Most Popular",
            logoResource = R.drawable.kylie, // Add your brand logo here
            isFavorite = false
        ),
        Brand(
            name = "Rhode",
            category = "Most Popular",
            logoResource = R.drawable.rhode, // Add your brand logo here
            isFavorite = false
        ),
        Brand(
            name = "Lululemon",
            category = "Most Popular",
            logoResource = R.drawable.lululemon, // Add your brand logo here
            isFavorite = true
        ),
        // Add more brands here:
        Brand(
            name = "Glow Beauty",
            category = "Most Popular",
            logoResource = R.drawable.glow, // Add your brand logo
            isFavorite = true
        ),
        Brand(
            name = "Moon Skincare",
            category = "Most Popular",
            logoResource = R.drawable.moon, // Add your brand logo
            isFavorite = false
        ),
        Brand(
            name = "Salt Cosmetics",
            category = "Most Popular",
            logoResource = R.drawable.salt, // Add your brand logo
            isFavorite = false
        ),
        Brand(
            name = "Story Hydration",
            category = "Most Popular",
            logoResource = R.drawable.story, // Add your brand logo
            isFavorite = false
        ),
        Brand(
            name = "Sweet Swimwear",
            category = "Most Popular",
            logoResource = R.drawable.sweet, // Add your brand logo
            isFavorite = false
        ),
    )
    
    /**
     * Tutorials for the tutorials page
     * To add new tutorials, add them to this list
     */
    val tutorials = listOf(
        Tutorial(
            title = "INFLUENCER Q+A TIPS",
            subtitle = "Interactive engagement strategies",
            imageResource = R.drawable.tutorial_1, // Replace with your tutorial image
            backgroundColor = "#FF69B4" // Pink background
        ),
        Tutorial(
            title = "FULL-TIME MICROINFLUENCER??",
            subtitle = "Building your personal brand",
            imageResource = R.drawable.tutorial_2, // Replace with your tutorial image
            backgroundColor = "#FFA07A" // Light salmon background
        ),
        Tutorial(
            title = "FULL-TIME MICROINFLUENCER??",
            subtitle = "Building your personal brand",
            imageResource = R.drawable.tutorial_1, // Replace with your tutorial image
            backgroundColor = "#FFA07A" // Light salmon background
        ),
        Tutorial(
            title = "HOW TO GROW AS A MICRO INFLUENCER",
            subtitle = "Growth strategies and tips",
            imageResource = R.drawable.tutorial_3, // Replace with your tutorial image
            backgroundColor = "#FFB6C1" // Light pink background
        )
    )
    
    /**
     * Categories for the popup selection
     */
    val popupCategories = listOf(
        PopupCategory("Most Popular", true), // true = selected by default
        PopupCategory("Beauty and Skincare", false),
        PopupCategory("Health and Wellness", false),
        PopupCategory("Food and Drink", false),
        PopupCategory("Fashion", false),
        PopupCategory("Accessories", false),
        PopupCategory("Technology", false),
        PopupCategory("Lifestyle", false)
    )
    
    /**
     * Background image for selected popup category buttons
     * Change this to use a different background image
     */
    val SELECTED_BUTTON_BACKGROUND = R.drawable.button_background // Change this to your desired image
    
    /**
     * How it Works popup graphics - easily changeable
     */
    val HOW_IT_WORKS_EMAIL_ICON = R.drawable.ic_launcher_foreground // Change to your email icon
    val HOW_IT_WORKS_SELECT_ICON = R.drawable.ic_launcher_foreground // Change to your select/heart icon  
    val HOW_IT_WORKS_STAR_ICON = R.drawable.ic_launcher_foreground // Change to your star icon
    val HOW_IT_WORKS_CLOSE_BUTTON_BACKGROUND = R.drawable.button_background // Change to your button background
    
    /**
     * Brand detail popup - easily changeable
     */
    val BRAND_DETAIL_IMAGE = R.drawable.brand_details // Change to your brand detail image
    val BRAND_EMAIL_BUTTON_BACKGROUND = R.drawable.button_background // Change to your email button background
    
    /**
     * Feedback popup - easily changeable
     */
    val FEEDBACK_LOGO = R.drawable.logo // Change to your logo image
    val FEEDBACK_YES_BUTTON_BACKGROUND = R.drawable.button_background // Change to your yes button background
}

/**
 * Data class for categories
 */
data class Category(
    val icon: String,
    val name: String,
    var isSelected: Boolean
)

/**
 * Data class for brands - updated to match database schema
 */
data class Brand(
    val id: Int = 0,
    val name: String,
    val email: String = "",
    val instagram: String = "",
    val website: String = "",
    val category: String,
    val logoUrl: String = "", // Changed from logoResource to logoUrl
    val logoResource: Int = 0, // Keep for backward compatibility with existing images
    val isEnabled: Boolean = true,
    val isPopular: Boolean = false,
    val placeNumber: Int = 0,
    var isFavorite: Boolean = false
)

/**
 * Data class for tutorials
 */
data class Tutorial(
    val title: String,
    val subtitle: String,
    val imageResource: Int,
    val backgroundColor: String
)

/**
 * Data class for popup categories
 */
data class PopupCategory(
    val name: String,
    var isSelected: Boolean
)
