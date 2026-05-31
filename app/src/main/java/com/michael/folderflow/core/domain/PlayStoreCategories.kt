package com.michael.folderflow.core.domain

object PlayStoreCategories {
    val appCategories = listOf(
        category("Art and Design", "A", "#EC4899", "art", "design", "draw", "paint", "sketch", "coloring", "canvas"),
        category("Auto and Vehicles", "V", "#F97316", "auto", "vehicle", "car", "bike", "fuel", "garage", "parking", "road"),
        category("Beauty", "B", "#F43F5E", "beauty", "makeup", "hair", "salon", "style", "skin", "cosmetic"),
        category("Books and Reference", "R", "#8B5CF6", "book", "reader", "reference", "dictionary", "wiki", "thesaurus", "kindle"),
        category("Business", "B", "#0F766E", "business", "office", "document", "remote", "job", "invoice", "scanner", "work"),
        category("Comics", "C", "#D946EF", "comic", "manga", "webtoon"),
        category("Communications", "C", "#2563EB", "chat", "message", "messenger", "dialer", "call", "browser", "mail", "email", "contact", "whatsapp", "telegram", "signal", "meet", "zoom", "teams", "slack"),
        category("Dating", "D", "#E11D48", "dating", "date", "match", "relationship", "tinder", "bumble"),
        category("Education", "E", "#7C3AED", "education", "learn", "study", "school", "course", "language", "exam", "classroom"),
        category("Entertainment", "E", "#9333EA", "entertainment", "stream", "movie", "tv", "show", "hulu", "netflix", "disney"),
        category("Events", "E", "#EA580C", "event", "ticket", "concert", "festival", "booking", "seat"),
        category("Finance", "F", "#16A34A", "bank", "finance", "pay", "wallet", "crypto", "stock", "invest", "money", "card", "cash", "ledger", "paypal", "venmo", "chase", "tax"),
        category("Food and Drink", "F", "#CA8A04", "food", "drink", "recipe", "restaurant", "delivery", "coffee", "wine", "menu", "uber eats", "doordash"),
        category("Health and Fitness", "H", "#DC2626", "health", "fit", "fitness", "diet", "doctor", "workout", "run", "sleep", "heart", "gym", "meditation", "yoga", "calm", "strava"),
        category("House and Home", "H", "#92400E", "home", "house", "apartment", "rent", "mortgage", "real estate", "decor"),
        category("Libraries and Demo", "L", "#64748B", "library", "demo", "sample", "developer preview"),
        category("Lifestyle", "L", "#14B8A6", "lifestyle", "wedding", "party", "fashion", "habit", "journal"),
        category("Maps and Navigation", "M", "#0284C7", "map", "maps", "navigation", "gps", "transit", "taxi", "ride", "uber", "lyft", "waze"),
        category("Medical", "M", "#0891B2", "medical", "medicine", "clinical", "drug", "pharmacy", "patient", "hospital"),
        category("Music and Audio", "M", "#DB2777", "music", "audio", "radio", "podcast", "spotify", "sound", "playlist"),
        category("News and Magazines", "N", "#475569", "news", "magazine", "newspaper", "headline", "journal"),
        category("Parenting", "P", "#F59E0B", "parent", "baby", "pregnancy", "child", "kids"),
        category("Personalization", "P", "#7C3AED", "wallpaper", "theme", "launcher", "ringtone", "lock screen", "keyboard"),
        category("Photography", "P", "#0EA5E9", "photo", "camera", "gallery", "image", "editor", "photoshop", "lightroom"),
        category("Productivity", "P", "#0D9488", "productivity", "note", "task", "todo", "calendar", "backup", "drive", "docs", "sheet", "notion", "trello"),
        category("Shopping", "S", "#E11D48", "shop", "store", "buy", "cart", "amazon", "ebay", "market", "target", "walmart", "aliexpress", "coupon"),
        category("Social", "S", "#2563EB", "social", "facebook", "twitter", "instagram", "mastodon", "discord", "linkedin", "tiktok", "snapchat", "reddit"),
        category("Sports", "S", "#65A30D", "sport", "sports", "score", "fantasy", "league", "team", "match"),
        category("Tools", "T", "#4F46E5", "tool", "tools", "utility", "file", "terminal", "compiler", "debug", "test", "benchmark", "cleaner", "optimizer", "settings", "assistant"),
        category("Travel and Local", "T", "#0891B2", "travel", "trip", "flight", "hotel", "local", "city", "tour", "booking", "airbnb"),
        category("Video Players and Editors", "V", "#7C2D12", "video", "player", "editor", "vlc", "youtube", "reels", "shorts"),
        category("Weather", "W", "#0284C7", "weather", "forecast", "radar", "storm", "temperature")
    )

    val gameCategories = listOf(
        category("Games", "G", "#F97316"),
        category("Action", "A", "#DC2626", "action", "shooter", "battle", "fight"),
        category("Adventure", "A", "#16A34A", "adventure", "quest", "explore", "minecraft", "mine", "craft"),
        category("Arcade", "A", "#EA580C", "arcade", "subway", "surfers"),
        category("Board", "B", "#7C3AED", "board", "chess"),
        category("Card", "C", "#0F766E", "card", "poker", "solitaire"),
        category("Casino", "C", "#B45309", "casino", "slots"),
        category("Casual", "C", "#DB2777", "casual"),
        category("Educational", "E", "#2563EB", "educational"),
        category("Music", "M", "#BE185D", "music game", "rhythm"),
        category("Puzzle", "P", "#9333EA", "puzzle", "sudoku", "candy", "crush"),
        category("Racing", "R", "#EF4444", "racing", "race", "car racing"),
        category("Role Playing", "R", "#6D28D9", "role playing", "rpg"),
        category("Simulation", "S", "#0D9488", "simulation", "simulator"),
        category("Strategy", "S", "#4F46E5", "strategy", "tower defense"),
        category("Trivia", "T", "#CA8A04", "trivia", "quiz"),
        category("Word", "W", "#64748B", "word")
    )

    val all = appCategories + gameCategories
    private val byName = all.associateBy { it.name.lowercase() }

    fun metadataFor(name: String): PlayStoreCategory {
        return byName[name.lowercase()] ?: category(name, name.firstOrNull()?.uppercase() ?: "A", "#4F46E5")
    }

    fun classify(packageName: String, label: String, broadCategory: String?): String {
        val combined = "$packageName $label".lowercase()
        all.firstOrNull { category ->
            category.keywords.any { keyword -> combined.contains(keyword) }
        }?.let { return it.name }

        return broadCategory ?: "Tools"
    }

    private fun category(
        name: String,
        icon: String,
        color: String,
        vararg keywords: String
    ) = PlayStoreCategory(name, icon, color, keywords.toList())
}

data class PlayStoreCategory(
    val name: String,
    val icon: String,
    val color: String,
    val keywords: List<String>
)
