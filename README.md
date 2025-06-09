# 📱 BudgetBuddy

> **Smart Budgeting & Expense Tracking Mobile App**  
> Empower your financial habits — track expenses, set goals, and achieve rewards!

## 🎯 Purpose of the App

BudgetBuddy was created to address the growing need for accessible, intuitive financial management tools. In today's economic climate, effective expense tracking and budget planning are essential life skills. This app serves as a comprehensive solution for:

- **Personal Financial Literacy**: Helping users understand their spending patterns and develop healthier financial habits
- **Goal-Oriented Budgeting**: Enabling users to set realistic financial targets and track progress toward achieving them
- **Expense Awareness**: Providing clear insights into where money goes, empowering informed financial decisions
- **Behavioral Change**: Using gamification elements (rewards, achievements) to encourage consistent financial tracking
- **Accessibility**: Making sophisticated budgeting tools available to users regardless of their financial background or expertise

The app bridges the gap between complex financial software and simple expense trackers, offering professional-grade features in an approachable, user-friendly interface.

## 🔧 GitHub & GitHub Actions Integration

BudgetBuddy leverages GitHub's powerful development ecosystem and automated workflows to maintain code quality and streamline development:

### **Version Control & Collaboration**
- **Repository Management**: All source code is hosted on GitHub, enabling collaborative development across team members
- **Branch Strategy**: Feature branches for individual development, with pull requests for code review and integration
- **Issue Tracking**: GitHub Issues used for bug reports, feature requests, and project planning
- **Code Reviews**: Pull request system ensures all code changes are reviewed before merging

### **Automated CI/CD with GitHub Actions**
Our project implements comprehensive GitHub Actions workflows for:

- **🏗️ Continuous Integration**:
  - Automated builds on every push and pull request
  - Unit test execution to catch regressions early
  - Code quality checks and linting
  - Dependency vulnerability scanning

- **📱 APK Generation**:
  - Automated debug APK builds for testing
  - Release APK builds for deployment
  - Artifact storage for easy download and distribution

- **🧪 Testing Automation**:
  - Unit tests for business logic validation
  - Instrumented tests for UI functionality
  - Performance tests to ensure app responsiveness
  - Test reports generation and sharing

- **📋 Code Quality Assurance**:
  - Lint checks for code style and potential issues
  - Security scanning for vulnerable dependencies
  - Build validation across different Android API levels

### **Workflow Files**
Located in `.github/workflows/`, our automated processes include:
- `android-ci.yml`: Main CI pipeline for builds and tests
- `release.yml`: Release automation and APK generation
- `code-quality.yml`: Linting and security checks

This integration ensures consistent code quality, reduces manual errors, and accelerates the development cycle while maintaining high standards for the BudgetBuddy application.

---

## 📑 Table of Contents

- [🚀 About the Project](#-about-the-project)
- [🛠️ Features](#️-features)
- [🆕 New & Lecturer-Requested Features](#-new--lecturer-requested-features)
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

![🚀 About the Project](image1.jpg)

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

![🛠️ Features](image2.jpg)

---

## 🆕 New & Lecturer-Requested Features

### 📉 Personal Minimum Budget (Lecturer-Driven Enhancement)
A custom budgeting floor that ensures users don’t budget below their essential needs.

- Users input a personal minimum monthly budget (e.g., $1500).
- Integrated with **Auto Budget**, influencing algorithmic recommendations.
- Ensures realistic, goal-driven planning and serves as a safeguard against under-budgeting.

![📉 Personal Minimum Budget](image3.jpg)

### 🤖 Auto Budget
A smart budgeting assistant that automatically distributes user budgets across categories based on selected strategies:

- **Balanced**, **Essentials First**, **Savings Focus**, **Lifestyle-Heavy** options
- Respects the user’s Personal Minimum Budget

![🤖 Auto Budget](image4.jpg)

### 🅰️ Visual Grading Scale
Helps users track how well they stick to their budget with easy-to-understand grades (A–F):

- Displays in the *Rewards & Achievements* section
- Includes progress bars and summary descriptions like “Excellent budget management this month”

![🅰️ Visual Grading Scale](image5.jpg)

### 🌐 Currency Selector
Makes BudgetBuddy more inclusive for international users:

- Choose from multiple global currencies
- Affects all screens, including summaries, history, goals, and rewards

![🌐 Currency Selector](image6.jpg)

### 🧁 Visual Spending by Category
A combination of charts and detailed breakdowns:

- Interactive **donut chart** and **category table**
- **Custom time filtering** (e.g., June 2025)
- **Export report** to PDF/shareable formats

![🧁 Visual Spending by Category](image7.jpg)

### 📈 Spending Analysis Over Time
Powerful analytics tool to study financial trends:

- Choose timeframes: 7 days, 30 days, 3 months, 12 months
- Category filtering and summary (e.g., R350 total, R11.29 daily avg)
- Line graph + trend indicators (e.g., “Rising”)
- Export functionality included

![📈 Spending Analysis Over Time](image8.jpg)

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

![📷 Screens Overview](image9.jpeg)
![📷 Screens Overview](image10.jpeg)
![📷 Screens Overview](image11.jpeg)
![📷 Screens Overview](image12.jpeg)

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

![📂 Project Structure](image13.jpeg)

## 📦 Tech Stack

- **Language:** Kotlin
- **Framework:** Android Jetpack
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room (for local data storage)
- **Networking:** Retrofit (for future cloud sync)
- **UI Components:** RecyclerView, ViewPager2, Navigation Component
- **Charts:** MPAndroidChart (for graphs and pie charts)
- **Backend:** Firebase (used for authentication and future cloud database)

![📦 Tech Stack](image14.jpeg)

---

## 🧰 How to Run the Project

> 🧑‍💻 **No Android Studio experience? No problem!**

### 1. Install Android Studio
- Download and install it from [here](https://developer.android.com/studio)

### 2. Clone the repository
```bash
git clone https://github.com/ST10359034/BudgetBuddy.git
```

### 3. Open in Android Studio
- Open Android Studio ➔ *Open an existing project* ➔ Select the `BudgetBuddy` folder

### 4. Build & Run
- Connect a device or start an Android Emulator
- Press **Run** ▶️ button

---
## Video Demonstration Link

https://youtu.be/QN1Gl3wHmoY

---

## 🧑‍🤝‍🧑 Team Members

| Name | Student Number | Contribution |
|:----|:----------------|:-------------|
| UNATHI KOLBERG | ST10332707 | Authentication and User Management Lead |
| BULELA MHLANA | ST10198391 | Expense tracking and Entry Logic Lead |
| EMIL FABEL | ST10359034 | Budget Setup, Categories and Report Lead |
| LISHA NAIDOO | ST10404816 | Gamification, UI Polish and GitHub/Testing Lead |

![🧑‍🤝‍🧑 Team Members](image15.jpeg)

---

## 📈 Future Improvements

- Integrate Firebase or AWS cloud sync
- Machine learning-based financial advice
- Dark mode theme
- App widgets (e.g., monthly budget widget)
- Voice-input for adding expenses

![📈 Future Improvements](image16.jpeg)

---

## 📚 References

- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html)
- [MPAndroidChart Documentation](https://github.com/PhilJay/MPAndroidChart)
- [Room Persistence Library](https://developer.android.com/jetpack/androidx/releases/room)
- [OpenAI](https://chatgpt.com)
- [Firebase](https://firebase.google.com)

---

## ⚖️ License

This project is licensed under the **MIT License**

---

# 🎉 Thank you for using BudgetBuddy!
> Helping you save smart, live smarter.

