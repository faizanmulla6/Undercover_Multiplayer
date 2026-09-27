# 🕵️‍♂️ Undercover

A social deduction party game for 3 to 16 players. Find the impostors before they outnumber the civilians!

This project is a modern, feature-rich implementation of the classic "Undercover" game, featuring both **Single-Device** (pass-and-play) and **Local WiFi Multiplayer** modes.

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/jetpack%20compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

## ✨ Features

- **🎮 Dual Game Modes**:
  - **Single Phone**: Pass one device around the circle.
  - **Multiplayer**: Everyone joins on their own phone via Local WiFi (mDNS discovery).
- **🎨 Dynamic Themes**:
  - **Warm Sunset (Day)**: A cozy, high-contrast cream and parchment theme.
  - **Briefing Room (Night)**: A sleek dark mode for late-night sessions.
  - Quick-switch toggle in the top bar.
- **🔡 Expansive Word Bank**:
  - 13+ Curated categories (Food, Animals, Tech, Desi, etc.).
  - **Custom Pairs**: Add your own inside jokes or theme-specific words.
  - **Cloud Sync**: Import word pairs directly from a Google Sheet via Apps Script.
- **🛠 Advanced Hosting**:
  - Adjustable Undercover & Blank agent counts.
  - Discussion timers and flexible voting styles (Quick Tap vs. Tally).
  - Manual Host resolution to control the game flow.
- **📊 Leaderboard**: Track scores across rounds.

## 🚀 Installation

### Download APK
You can download the latest production-ready APK from the [Releases](https://github.com/faiza/UndercoverMultiplayer/releases) section of this repository.

### Build from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/faiza/UndercoverMultiplayer.git
   ```
2. Open the project in **Android Studio (Hedgehog or newer)**.
3. Sync Gradle and run the `app` module on your device or emulator.

## 📖 How to Play

1.  **Setup**: Add player names and choose the number of Undercover agents and Blank agents.
2.  **The Secret**: Each player views their secret word. 
    - **Civilians** get the same word.
    - **Undercover** agents get a slightly different (but related) word.
    - **Blank** agents get no word at all!
3.  **The Clues**: In the generated speaking order, each player gives a **one-word clue** about their secret.
4.  **The Discussion**: After everyone has spoken, the group debates who seems suspicious.
5.  **The Vote**: Players vote to eliminate someone. 
    - If the **Blank** agent is voted out, they get one chance to guess the Civilians' word to steal the win!
6.  **Victory**: Civilians win if all impostors are caught. Impostors win if they outnumber the civilians.

## 🛠 Tech Stack

- **UI**: Jetpack Compose with Material 3.
- **State**: ViewModel with `mutableStateOf` and Kotlin Coroutines.
- **Networking**: Custom TCP Socket Server/Client with mDNS (NsdManager) for zero-config discovery.
- **Storage**: SharedPreferences with JSON serialization for persistence.
- **Networking Library**: OkHttp (for Cloud Sync).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Created with ❤️ by Faizan*
