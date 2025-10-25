# Main Page UI - Easy Customization Guide

## How to Easily Change Graphics and Add Items

### 🎨 **Changing Brand Logos**

1. **Add your brand logo image** to: `app/src/main/res/drawable/`
2. **Name it**: `brand_yourname.png` (e.g., `brand_nike.png`)
3. **Update MainPageData.kt** and change the `logoResource`:

```kotlin
Brand(
    name = "Your Brand",
    category = "Most Popular",
    logoResource = R.drawable.brand_yourname, // ← Change this
    isFavorite = false
)
```

### ➕ **Adding New Brands**

Open `MainPageData.kt` and add to the `brands` list:

```kotlin
val brands = listOf(
    // Existing brands...
    Brand(
        name = "New Brand Name",
        category = "Most Popular", // or any category
        logoResource = R.drawable.your_brand_logo,
        isFavorite = false
    ),
    // Add more here...
)
```

### 🏷️ **Adding New Categories**

Open `MainPageData.kt` and add to the `categories` list:

```kotlin
val categories = listOf(
    // Existing categories...
    Category("🎯", "Trending", false), // emoji, name, selected
    Category("💎", "Premium", false),
    // Add more here...
)
```

### 🎨 **Changing UI Colors**

#### **Category Button Colors:**
- **Selected**: Edit `category_background.xml` → change `android:color`
- **Unselected**: Edit `category_background_unselected.xml` → change `android:color`

#### **Email Button Color:**
- Edit `email_button_background.xml` → change `android:color`

#### **Search Bar:**
- Edit `search_background.xml` → change colors and border

### 🔧 **Current Features**

✅ **Horizontal scrollable categories** (Popular, Favorites, Spotlight)  
✅ **Vertical scrollable brands list**  
✅ **Search functionality** (searches brand names and categories)  
✅ **Favorite toggle** (heart icon with red/outline states)  
✅ **Email buttons** for each brand  
✅ **Header with hamburger menu and lightning icon**  
✅ **Easy customization** - just edit one file!

### 📁 **File Structure**

```
MainPageData.kt           - ADD NEW BRANDS/CATEGORIES HERE!
main.xml                  - Main layout
item_brand.xml           - Individual brand item layout
item_category.xml        - Individual category item layout
CategoryAdapter.kt       - Handles category display
BrandAdapter.kt          - Handles brand display
MainActivity.kt          - Main controller
```

### 💡 **Quick Tips**

- **Brand logos** should be square (1:1 ratio) for best display
- **Categories** support emojis for icons
- **Search** works on both brand names and categories
- **Favorites** are automatically saved during the session
- **All lists are fully scrollable** horizontally and vertically

### 🚀 **To Add More Brands:**

1. Add brand logo to `/drawable/`
2. Add one entry to `MainPageData.kt`
3. That's it! The app handles everything else automatically! 🎉
