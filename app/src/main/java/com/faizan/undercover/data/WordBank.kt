package com.faizan.undercover.data

import com.faizan.undercover.model.WordPair

/**
 * The bundled word bank. Pairs are grouped into categories so a group can tune
 * the difficulty / flavour of a night without writing their own list.
 */
object WordBank {

    const val FOOD = "Food & Drink"
    const val ANIMALS = "Animals"
    const val PLACES = "Places"
    const val OBJECTS = "Everyday Things"
    const val PEOPLE = "People & Jobs"
    const val SPORT = "Sport & Games"
    const val CULTURE = "Screen & Stage"
    const val NATURE = "Nature"
    const val DESI = "Desi"
    const val TRICKY = "Tricky"

    val categories: List<String> = listOf(
        FOOD, ANIMALS, PLACES, OBJECTS, PEOPLE, SPORT, CULTURE, NATURE, DESI, TRICKY
    )

    val pairs: List<WordPair> = listOf(
        // Food & drink
        WordPair("Pizza", "Pasta", FOOD),
        WordPair("Coffee", "Tea", FOOD),
        WordPair("Chocolate", "Vanilla", FOOD),
        WordPair("Butter", "Cheese", FOOD),
        WordPair("Ice cream", "Frozen yogurt", FOOD),
        WordPair("Burger", "Sandwich", FOOD),
        WordPair("Bakery", "Cafe", FOOD),
        WordPair("Whisky", "Wine", FOOD),
        WordPair("Rice", "Wheat", FOOD),
        WordPair("Soup", "Stew", FOOD),
        WordPair("Honey", "Jam", FOOD),
        WordPair("Noodles", "Spaghetti", FOOD),

        // Animals
        WordPair("Cat", "Dog", ANIMALS),
        WordPair("Lion", "Tiger", ANIMALS),
        WordPair("Butterfly", "Moth", ANIMALS),
        WordPair("Spider", "Scorpion", ANIMALS),
        WordPair("Crow", "Pigeon", ANIMALS),
        WordPair("Dolphin", "Shark", ANIMALS),
        WordPair("Horse", "Donkey", ANIMALS),
        WordPair("Frog", "Toad", ANIMALS),
        WordPair("Rabbit", "Hamster", ANIMALS),
        WordPair("Crocodile", "Alligator", ANIMALS),

        // Places
        WordPair("Beach", "Desert", PLACES),
        WordPair("Mountain", "Hill", PLACES),
        WordPair("Village", "City", PLACES),
        WordPair("School", "College", PLACES),
        WordPair("Temple", "Church", PLACES),
        WordPair("Hotel", "Hostel", PLACES),
        WordPair("Museum", "Gallery", PLACES),
        WordPair("Airport", "Railway station", PLACES),
        WordPair("Bridge", "Tunnel", PLACES),
        WordPair("Library", "Bookshop", PLACES),

        // Everyday things
        WordPair("Phone", "Tablet", OBJECTS),
        WordPair("Pen", "Pencil", OBJECTS),
        WordPair("Wallet", "Purse", OBJECTS),
        WordPair("Umbrella", "Raincoat", OBJECTS),
        WordPair("Kettle", "Teapot", OBJECTS),
        WordPair("Sofa", "Armchair", OBJECTS),
        WordPair("Candle", "Lantern", OBJECTS),
        WordPair("Camera", "Telescope", OBJECTS),
        WordPair("Shirt", "T-shirt", OBJECTS),
        WordPair("Sandal", "Sneaker", OBJECTS),
        WordPair("Mirror", "Window", OBJECTS),
        WordPair("Clock", "Watch", OBJECTS),
        WordPair("Fan", "Air conditioner", OBJECTS),
        WordPair("Backpack", "Suitcase", OBJECTS),

        // People & jobs
        WordPair("Doctor", "Nurse", PEOPLE),
        WordPair("Dentist", "Surgeon", PEOPLE),
        WordPair("Actor", "Singer", PEOPLE),
        WordPair("Astronaut", "Pilot", PEOPLE),
        WordPair("Teacher", "Professor", PEOPLE),
        WordPair("Chef", "Baker", PEOPLE),
        WordPair("King", "Queen", PEOPLE),
        WordPair("Lawyer", "Judge", PEOPLE),
        WordPair("Barber", "Tailor", PEOPLE),
        WordPair("Detective", "Spy", PEOPLE),

        // Sport & games
        WordPair("Football", "Basketball", SPORT),
        WordPair("Cricket", "Baseball", SPORT),
        WordPair("Chess", "Checkers", SPORT),
        WordPair("Swimming", "Diving", SPORT),
        WordPair("Marathon", "Sprint", SPORT),
        WordPair("Badminton", "Tennis", SPORT),
        WordPair("Skiing", "Skating", SPORT),
        WordPair("Referee", "Coach", SPORT),

        // Screen & stage
        WordPair("Movie", "TV show", CULTURE),
        WordPair("Guitar", "Piano", CULTURE),
        WordPair("Novel", "Poem", CULTURE),
        WordPair("Painting", "Sculpture", CULTURE),
        WordPair("Concert", "Festival", CULTURE),
        WordPair("Comedy", "Drama", CULTURE),
        WordPair("Podcast", "Radio show", CULTURE),
        WordPair("Cartoon", "Anime", CULTURE),

        // Nature
        WordPair("Sun", "Moon", NATURE),
        WordPair("Snow", "Rain", NATURE),
        WordPair("River", "Lake", NATURE),
        WordPair("Ocean", "Sea", NATURE),
        WordPair("Volcano", "Earthquake", NATURE),
        WordPair("Rose", "Lily", NATURE),
        WordPair("Summer", "Winter", NATURE),
        WordPair("Forest", "Jungle", NATURE),
        WordPair("Storm", "Hurricane", NATURE),

        // Desi
        WordPair("Samosa", "Kachori", DESI),
        WordPair("Idli", "Dosa", DESI),
        WordPair("Chai", "Filter coffee", DESI),
        WordPair("Roti", "Paratha", DESI),
        WordPair("Lassi", "Chaas", DESI),
        WordPair("Auto rickshaw", "Taxi", DESI),
        WordPair("Holi", "Diwali", DESI),
        WordPair("Biryani", "Pulao", DESI),
        WordPair("Kabaddi", "Kho kho", DESI),
        WordPair("Bollywood", "Tollywood", DESI),
        WordPair("Gulab jamun", "Rasgulla", DESI),
        WordPair("Local train", "Metro", DESI),

        // Tricky — deliberately close pairs for experienced groups
        WordPair("Jealousy", "Envy", TRICKY),
        WordPair("Promise", "Oath", TRICKY),
        WordPair("Lie", "Secret", TRICKY),
        WordPair("Habit", "Routine", TRICKY),
        WordPair("Fame", "Popularity", TRICKY),
        WordPair("Fear", "Anxiety", TRICKY),
        WordPair("Gift", "Reward", TRICKY),
        WordPair("Memory", "Dream", TRICKY),
        WordPair("Luck", "Fate", TRICKY),
        WordPair("Advice", "Warning", TRICKY)
    )

    fun byCategory(disabled: Set<String>): List<WordPair> =
        pairs.filter { it.category !in disabled }

    fun countIn(category: String): Int = pairs.count { it.category == category }
}
