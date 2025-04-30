# 📱 BudgetBuddy

> **Smart Budgeting & Expense Tracking Mobile App**  
> Empower your financial habits — track expenses, set goals, and achieve rewards!

---

## 📑 Table of Contents

- [🚀 About the Project](#-about-the-project)
- [🛠️ Features](#️-features)
- [📷 Screens Overview](#-screens-overview)
- [📂 Project Structure](#-project-structure)
- [📦 Tech Stack](#-tech-stack)
- [🧰 How to Run the Project](#-how-to-run-the-project)
- [🧑‍🤝‍🧑 Team Members](#-team-members)
- [📈 Future Improvements](#-future-improvements)
- [📚 References](#-references)
- [⚖️ License](#️-license)

---

## 🚀 About the Project

**BudgetBuddy** is a modern Android application built to help users manage their finances easily and effectively.  
It offers **expense tracking**, **budget planning**, **reports**, **rewards**, and **insights** — all wrapped in a beautiful, user-friendly experience.

---

## 🛠️ Features

- 📋 **User Authentication** (Login, Sign Up)
- 💸 **Track Expenses** — Add, edit, and delete expenses
- 🎯 **Set Budgets** — Monthly limits and category-specific budgets
- 📊 **Reports & Insights** — Visual graphs (Pie Chart, Bar Graphs)
- 🏆 **Rewards & Achievements** — Earn badges and track progress
- 📂 **Expense History** — Full searchable list
- 🔔 **Smart Notifications** — Budget alerts, daily reminders
- ☁️ **Cloud Sync** — Backup your financial data securely
- ⚙️ **Profile & Settings** — Manage personal details and app preferences

---

## 📷 Screens Overview

| Screen | Description |
|:------|:-------------|
| 1. Startup | App introduction with features summary |
| 2. Login/Sign Up | Authenticate users |
| 3. Account Creation | Register new users |
| 4. Homepage | Dashboard with overview |
| 5. Add New Expense | Add individual expenses |
| 6. Expenses List | View and filter expenses |
| 7. Budget Setup | Define monthly budgets |
| 8. Reports & Insights | Visualize spending habits |
| 9. Rewards & Achievements | Track and share badges earned |
| 10. Profile Page | View and edit user profile |
| 11. Settings Page | Manage notifications, cloud sync, and logout |

---

## 📂 Project Structure

```
BudgetBuddy/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/budgetbuddy/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── auth/         # Login, Signup, Account creation screens
│   │   │   │   │   ├── dashboard/    # Home, Expenses, Add Expense, Budget Setup
│   │   │   │   │   ├── insights/     # Reports & Insights
│   │   │   │   │   ├── profile/      # Profile page
│   │   │   │   │   ├── rewards/      # Rewards & Achievements page
│   │   │   │   │   ├── settings/     # Settings page
│   │   │   │   ├── data/             # Data models (User, Expense, Budget, Rewards)
│   │   │   │   ├── utils/            # Helpers, Constants
│   │   │   ├── res/
│   │   │   │   ├── layout/           # XML Layout files
│   │   │   │   ├── drawable/         # Images, icons
│   │   │   │   ├── values/           # Colors, Styles
├── build.gradle
├── README.md
```

---

## 📦 Tech Stack

- **Language:** Kotlin
- **Framework:** Android Jetpack
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room (for local data storage)
- **Networking:** Retrofit (for future cloud sync)
- **UI Components:** RecyclerView, ViewPager2, Navigation Component
- **Charts:** MPAndroidChart (for graphs and pie charts)

---

## 🧰 How to Run the Project

> 🧑‍💻 **No Android Studio experience? No problem!**

### 1. Install Android Studio
- Download and install it from [here](https://developer.android.com/studio).

### 2. Clone the repository
```bash
git clone https://github.com/ST10359034/BudgetBuddy.git
```

### 3. Open in Android Studio
- Open Android Studio ➔ *Open an existing project* ➔ Select the `BudgetBuddy` folder.

### 4. Build & Run
- Connect a device or start an Android Emulator
- Press **Run** ▶️ button.

---

## 🧑‍🤝‍🧑 Team Members

| Name | Student Number | Contribution |
|:----|:----------------|:-------------|
| UNATHI KOLBERG | ST10332707 | Authentication and User Management Lead |
| BULELA MHLANA | ST10198391 | Expense tracking and Entry Logic Lead |
| EMIL FABEL | ST10359034 | Budget Setup, Categories and Report Lead |
| LISHA NAIDOO | ST10404816 | Gamification, UI Polish and GitHub/Testing Lead |

---

## 📈 Future Improvements

- Integrate Firebase or AWS cloud sync
- Machine learning-based financial advice
- Dark mode theme
- App widgets (e.g., monthly budget widget)

---

## 📚 References

- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html)
- [MPAndroidChart Documentation](https://github.com/PhilJay/MPAndroidChart)
- [Room Persistence Library](https://developer.android.com/jetpack/androidx/releases/room)
- [OpenAI](https://chatgpt.com/)

---

## ⚖️ License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

# 🎉 Thank you for using BudgetBuddy!
> Helping you save smart, live smarter.
