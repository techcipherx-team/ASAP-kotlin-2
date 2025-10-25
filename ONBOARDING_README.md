# Onboarding Screens - Easy Setup Guide

## How to Add New Onboarding Screens

Adding new onboarding screens is now super easy! Just follow these simple steps:

### Step 1: Add Your Image
1. Place your new onboarding image in: `app/src/main/res/drawable/`
2. Name it something like: `on_boarding_3.png`, `on_boarding_4.png`, etc.

### Step 2: Update OnboardingData.kt
1. Open: `app/src/main/java/com/xiaomi/getapps/signature/asap/OnboardingData.kt`
2. Add your new screen to the `screens` list:

```kotlin
val screens = listOf(
    OnboardingScreen(R.drawable.on_boarding_1),
    OnboardingScreen(R.drawable.on_boarding_2),
    OnboardingScreen(R.drawable.on_boarding_3), // ← Add this line
    OnboardingScreen(R.drawable.on_boarding_4), // ← Add this line
    // Add more as needed...
)
```

### That's it! 🎉

The app will automatically:
- Display your new screens in the onboarding flow
- Handle navigation between all screens
- Show the continue button on each screen
- Navigate to MainActivity after the last screen

## Current Features
- ✅ Fullscreen display (no headers/status bars)
- ✅ Swipe navigation between screens
- ✅ Continue button with custom image
- ✅ Automatic progression to MainActivity
- ✅ Easy to add new screens
- ✅ Clean, organized code structure

## File Structure
```
OnboardingActivity.kt     - Main onboarding controller
OnboardingAdapter.kt      - Handles screen display
OnboardingData.kt         - Centralized screen management (ADD NEW SCREENS HERE!)
activity_onboarding.xml   - Main layout with ViewPager2 and button
item_onboarding.xml       - Individual screen layout
```

## Tips
- Keep images in PNG format for best quality
- Use high-resolution images for better display on all devices
- Images will automatically scale to fit the screen using `centerCrop`
