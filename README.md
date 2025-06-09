# 📱 BudgetBuddy

> **Smart Budgeting & Expense Tracking Mobile App**  
> Empower your financial habits — track expenses, set goals, and achieve rewards!

---

## 📑 Table of Contents

- [🚀 About the Project](#-about-the-project)
- [🎯 Purpose & Design Philosophy](#-purpose--design-philosophy)
- [🛠️ Features](#️-features)
- [🆕 New & Lecturer-Requested Features](#-new--lecturer-requested-features)
- [📷 Screens Overview](#-screens-overview)
- [📂 Project Structure](#-project-structure)
- [📦 Tech Stack](#-tech-stack)
- [⚙️ GitHub & GitHub Actions](#️-github--github-actions)
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

## 🎯 Purpose & Design Philosophy

### 💡 **The Problem We Solve**

In today's fast-paced world, **personal financial management** remains one of the biggest challenges for individuals:

- **58% of people** live paycheck to paycheck without proper budgeting
- **Expense tracking** is often tedious and abandoned after a few weeks
- **Financial literacy** gaps lead to poor spending decisions
- **Traditional budgeting apps** are either too complex or lack gamification elements
- **Young adults** struggle with developing healthy financial habits early

**BudgetBuddy** addresses these pain points by making financial management **simple**, **engaging**, and **rewarding**.

### 🎨 **Design Philosophy**

#### **1. User-Centric Design**
Our design prioritizes **user experience** above all else:

- **Intuitive Navigation**: Clean, consistent interface following Material Design 3 principles
- **Minimal Learning Curve**: New users can start tracking expenses within 30 seconds
- **Visual Hierarchy**: Important information (balances, alerts) prominently displayed
- **Accessibility First**: Support for screen readers, high contrast mode, and large text options

#### **2. Gamification & Behavioral Psychology**
We leverage **positive reinforcement** to build lasting financial habits:

- **Achievement System**: Unlock badges for consistent budgeting and savings milestones
- **Progress Visualization**: Visual grades (A-F) make budget performance tangible
- **Micro-Interactions**: Smooth animations and feedback celebrate user actions
- **Social Elements**: Shareable achievements encourage positive peer influence

#### **3. Data-Driven Insights**
Transform raw financial data into **actionable intelligence**:

- **Smart Categorization**: Auto-categorize expenses with machine learning
- **Trend Analysis**: Identify spending patterns over time with beautiful charts
- **Predictive Budgeting**: Auto Budget feature suggests realistic budget allocations
- **Personalized Recommendations**: Tailored advice based on spending behavior

### 🏗️ **Architecture & Technical Design**

#### **MVVM Architecture Pattern**
```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│   View      │───▶│   ViewModel  │───▶│    Model    │
│ (Fragment)  │    │ (LiveData)   │    │ (Repository)│
└─────────────┘    └──────────────┘    └─────────────┘
```

**Benefits:**
- **Separation of Concerns**: UI logic separate from business logic
- **Testability**: ViewModels can be unit tested without Android dependencies
- **Data Persistence**: LiveData ensures UI stays in sync with data changes
- **Configuration Changes**: Survives screen rotations and app state changes

#### **Dependency Injection with Hilt**
- **Singleton Pattern**: Database, network clients, and repositories are application-scoped
- **Lifecycle Awareness**: ViewModels automatically injected and scoped correctly
- **Testing Support**: Easy to mock dependencies for unit and integration tests

#### **Database Design (Room)**
```sql
-- Optimized schema for fast queries
Expenses: [id, amount, category, date, description, receipt_path]
Budgets:  [id, category, limit, period, user_id]
Users:    [id, name, email, currency, minimum_budget]
```

**Performance Optimizations:**
- **Indexed Queries**: Date and category fields indexed for fast filtering
- **Pagination**: Large expense lists loaded incrementally
- **Background Threading**: All database operations run off main thread

### 🎭 **User Experience (UX) Design**

#### **Information Architecture**
```
Home Dashboard
├── Quick Actions (Add Expense, View Budget)
├── Weekly Spending Chart
├── Budget Status Cards
└── Recent Transactions

Navigation Structure
├── Home (Dashboard)
├── Expenses (Add/View/Filter)
├── Reports (Analytics/Insights)
├── Rewards (Achievements/Progress)
└── Profile (Settings/Preferences)
```

#### **Design System**
- **Color Palette**: Calming blues and greens for trust, vibrant accents for actions
- **Typography**: Roboto font family for excellent readability across devices
- **Iconography**: Consistent Material Design icons with custom financial symbols
- **Spacing**: 8dp grid system ensures visual consistency

#### **Responsive Design**
- **Adaptive Layouts**: Optimized for phones (5" to 7"), tablets (7" to 12")
- **Orientation Support**: Seamless landscape/portrait transitions
- **Dynamic Type**: Scales with system font size preferences
- **Touch Targets**: Minimum 48dp for accessibility compliance

### 🔒 **Security & Privacy Design**

#### **Data Protection**
- **Local-First Architecture**: Sensitive data stored locally using Room encryption
- **Firebase Authentication**: Secure OAuth2 implementation
- **No Sensitive Storage**: Financial account numbers never stored in app
- **Secure Networking**: HTTPS only, certificate pinning for API calls

#### **Privacy by Design**
- **Minimal Data Collection**: Only collect what's necessary for core functionality
- **User Consent**: Clear opt-in for analytics and cloud sync features
- **Data Portability**: Export feature allows users to download their data
- **Right to Deletion**: Complete account deletion removes all user data

### 📱 **Mobile-First Considerations**

#### **Performance Optimization**
- **App Size**: Optimized APK under 15MB with ProGuard/R8 obfuscation
- **Battery Efficiency**: Background tasks minimized, intelligent sync scheduling
- **Memory Management**: ViewPager2 with fragment recycling for smooth navigation
- **Network Awareness**: Offline-first approach with intelligent sync when connected

#### **Platform Integration**
- **System Themes**: Dark/light mode follows system preferences
- **Notification Management**: Smart alerts for budget thresholds and reminders
- **Deep Linking**: Direct navigation to specific expenses or reports via URLs
- **Sharing Integration**: Native Android sharing for reports and achievements

This comprehensive design approach ensures BudgetBuddy not only solves real financial problems but does so in a way that users actually **want** to engage with regularly, building lasting positive financial habits.

![🎯 Purpose & Design Philosophy](image1.jpg)

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

## 🆕 New & Lecturer-Requested Features

![🆕 New Features](image2.jpg)

### 📉 Personal Minimum Budget (Lecturer-Driven Enhancement)
A custom budgeting floor that ensures users don't budget below their essential needs.

- Users input a personal minimum monthly budget (e.g., $1500).
- Integrated with **Auto Budget**, influencing algorithmic recommendations.
- Ensures realistic, goal-driven planning and serves as a safeguard against under-budgeting.

![📉 Personal Minimum Budget](image3.jpg)

### 🤖 Auto Budget
A smart budgeting assistant that automatically distributes user budgets across categories based on selected strategies:

- **Balanced**, **Essentials First**, **Savings Focus**, **Lifestyle-Heavy** options
- Respects the user's Personal Minimum Budget

![🤖 Auto Budget](image4.jpg)

### 🅰️ Visual Grading Scale
Helps users track how well they stick to their budget with easy-to-understand grades (A–F):

- Displays in the *Rewards & Achievements* section
- Includes progress bars and summary descriptions like "Excellent budget management this month"

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
- Line graph + trend indicators (e.g., "Rising")
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

## ⚙️ GitHub & GitHub Actions

### 🔄 Version Control & Collaboration
Our team leverages **GitHub** as the central hub for project collaboration and version control:

- **Repository Management**: Centralized codebase with organized branch structure
- **Pull Request Workflow**: All team members contribute through feature branches and pull requests
- **Code Reviews**: Mandatory peer reviews before merging to ensure code quality
- **Issue Tracking**: GitHub Issues for bug reports, feature requests, and task management
- **Project Boards**: Organized sprint planning and progress tracking

### 🤖 Continuous Integration with GitHub Actions

We've implemented a robust **CI/CD pipeline** using GitHub Actions to ensure code quality and automated testing:

#### **Automated Workflows:**

1. **🧪 Unit Testing Pipeline**
   ```yaml
   name: Unit Tests
   triggers: [push, pull_request]
   ```
   - Runs comprehensive unit tests for ViewModels, utilities, and business logic
   - Includes Firebase helper functions and validation tests
   - Generates test coverage reports

2. **📱 Instrumented Testing Pipeline**
   ```yaml
   name: Android Instrumented Tests
   triggers: [push, pull_request]
   ```
   - UI automation tests for critical user flows
   - Navigation testing across all fragments
   - Form validation and user interaction tests

3. **🔍 Code Quality & Linting**
   ```yaml
   name: Android Lint & Code Analysis
   triggers: [push, pull_request]
   ```
   - Static code analysis using Android Lint
   - Kotlin code style enforcement
   - Security vulnerability scanning

4. **🏗️ Build Verification**
   ```yaml
   name: Build APK
   triggers: [push, pull_request]
   ```
   - Automated debug and release APK building
   - Gradle dependency resolution verification
   - Multi-variant build testing

#### **Quality Gates:**
- ✅ **All tests must pass** before merge approval
- ✅ **Zero critical lint issues** allowed in main branch
- ✅ **Successful APK build** required for all PRs
- ✅ **Code coverage** maintained above 80% for core modules

#### **Automated Reporting:**
- 📊 **Test Results**: Automatic test result posting in PR comments
- 📈 **Performance Metrics**: APK size tracking and memory usage analysis
- 🚨 **Failure Notifications**: Instant Slack/email alerts for build failures
- 📋 **Status Badges**: Real-time build status in README

### 🔒 Security & Best Practices
- **Secret Management**: Sensitive keys stored in GitHub Secrets
- **Branch Protection**: Main branch protected with required status checks
- **Automated Dependency Updates**: Dependabot for security patches
- **Code Scanning**: GitHub Advanced Security for vulnerability detection

This comprehensive GitHub Actions setup ensures that our BudgetBuddy app maintains high code quality, reliability, and security throughout the development lifecycle.

![⚙️ GitHub Actions](image15.jpeg)

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

This project is licensed under the **MIT License**.

---

# 🎉 Thank you for using BudgetBuddy!
> Helping you save smart, live smarter.




---

# 🎉 Thank you for using BudgetBuddy!
> Helping you save smart, live smarter.

