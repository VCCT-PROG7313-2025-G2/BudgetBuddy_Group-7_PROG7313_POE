# BudgetBuddy - Automated Testing & CI/CD Guide

## 🚀 Overview

This guide documents the comprehensive automated testing and Continuous Integration/Continuous Deployment (CI/CD) setup for the BudgetBuddy Android application using GitHub Actions.

## 📋 Testing Infrastructure

### Unit Tests
- **Location**: `app/src/test/java/com/example/budgetbuddy/`
- **Framework**: JUnit 4, Mockito, Kotlin Coroutines Test
- **Coverage**: Core business logic, data validation, calculations

#### Key Test Files:
- `ExampleUnitTest.kt` - Core functionality tests including:
  - Budget calculation logic
  - Expense validation
  - Category management
  - Achievement point calculations
  - Date range validation

### Instrumented Tests
- **Location**: `app/src/androidTest/java/com/example/budgetbuddy/`
- **Framework**: Espresso, UI Automator, AndroidX Test
- **Coverage**: UI flows, integration testing, performance testing

#### Existing Test Files:
- `AddExpenseFlowTest.kt` - Tests expense creation workflow
- `ProfileFlowTest.kt` - Tests profile management features
- `ReportsFlowTest.kt` - Tests reports and analytics features
- `BottomNavigationTest.kt` - Tests navigation functionality
- `SignUpFlowTest.kt` - Tests user registration
- `LoginFlowTest.kt` - Tests user authentication
- `PerformanceTest.kt` - Tests app performance metrics

### Performance Tests
- **Startup Time**: < 3 seconds
- **Navigation Performance**: < 2 seconds for 4 navigations
- **Memory Usage**: < 50MB increase during testing
- **UI Responsiveness**: < 3 seconds for 40 interactions

## 🔧 GitHub Actions Workflows

### 1. Main CI/CD Workflow (`.github/workflows/build.yml`)

#### Jobs:
- **Unit Tests**: Runs JUnit tests with coverage reporting
- **Lint Analysis**: Performs code quality checks with baseline
- **Build Debug APK**: Compiles and packages debug version
- **Instrumented Tests**: Runs UI tests on multiple Android API levels (26, 29, 33)
- **Code Quality**: Generates test coverage reports
- **Security Scan**: Performs dependency vulnerability checks

#### Triggers:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

### 2. Release Workflow (`.github/workflows/release.yml`)

#### Jobs:
- **Comprehensive Testing**: All unit and lint tests
- **Release APK Build**: Creates production-ready APK
- **Security Analysis**: CodeQL static analysis
- **Performance Testing**: Extended performance validation
- **GitHub Release**: Automatic release creation with notes

#### Triggers:
- Version tags (e.g., `v1.0.0`)
- Manual workflow dispatch

## 📊 Test Dependencies

### Unit Testing:
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.7.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("com.google.truth:truth:1.1.4")
```

### Instrumented Testing:
```kotlin
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
androidTestImplementation("androidx.test:core:1.5.0")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
```

## 🎯 Key Features Tested

### 1. Budget Management
- ✅ Monthly budget calculations
- ✅ Category budget allocations
- ✅ Over-budget detection
- ✅ Budget percentage calculations

### 2. Expense Tracking
- ✅ Expense amount validation
- ✅ Category assignment
- ✅ Date range filtering
- ✅ Expense aggregation

### 3. Reports & Analytics
- ✅ Category spending analysis
- ✅ Time period filtering
- ✅ Chart data generation
- ✅ Export functionality

### 4. Achievement System
- ✅ Points calculation logic
- ✅ Level progression
- ✅ Badge unlocking
- ✅ Leaderboard ranking

### 5. User Interface
- ✅ Navigation flows
- ✅ Form validation
- ✅ Error handling
- ✅ Performance metrics

## 🚦 Running Tests Locally

### Unit Tests:
```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests:
```bash
./gradlew connectedDebugAndroidTest
```

### Lint Checks:
```bash
./gradlew lintDebug
```

### Build APK:
```bash
./gradlew assembleDebug
```

### All Quality Checks:
```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## 📈 Test Reports

### Generated Reports:
- **Unit Test Results**: `app/build/reports/tests/testDebugUnitTest/`
- **Lint Report**: `app/build/reports/lint-results-debug.html`
- **Instrumented Test Results**: `app/build/reports/androidTests/connected/`
- **Coverage Reports**: `app/build/reports/coverage/`

### Artifacts in GitHub Actions:
- Unit test results
- Lint analysis reports
- Debug/Release APKs
- Performance test results
- Security scan results

## 🔒 Security & Quality

### Code Quality:
- **Lint Baseline**: Tracks existing issues, prevents new ones
- **Static Analysis**: CodeQL integration for security vulnerabilities
- **Dependency Scanning**: Automated vulnerability detection

### Performance Monitoring:
- App startup time validation
- Memory usage tracking
- UI responsiveness testing
- Navigation performance metrics

## 🎨 Best Practices Implemented

### 1. Test Organization:
- Clear separation of unit and integration tests
- Descriptive test naming with backticks
- Comprehensive helper functions
- Mock data classes for testing

### 2. CI/CD Pipeline:
- Parallel job execution for faster feedback
- Matrix testing across multiple Android versions
- Artifact preservation for debugging
- Automatic release management

### 3. Code Quality:
- Lint baseline to track improvements
- Security scanning for dependencies
- Performance benchmarking
- Comprehensive test coverage

## 🚀 Getting Started

1. **Clone the repository**
2. **Set up Firebase configuration** (see `FIREBASE_SETUP_CHECKLIST.md`)
3. **Run local tests**: `./gradlew testDebugUnitTest`
4. **Push to trigger CI**: GitHub Actions will automatically run all tests
5. **Create release**: Tag with `v*` format to trigger release workflow

## 📝 Test Coverage

### Current Coverage Areas:
- ✅ Budget calculations and validation
- ✅ Expense management workflows
- ✅ Category filtering and analysis
- ✅ User authentication flows
- ✅ Navigation and UI interactions
- ✅ Performance and memory usage
- ✅ Security and error handling

### Future Enhancements:
- 🔄 End-to-end Firebase integration tests
- 🔄 Accessibility testing automation
- 🔄 Screenshot testing for UI regression
- 🔄 Load testing for large datasets

## 🛠️ Troubleshooting

### Common Issues:

1. **Lint Failures**:
   - Update lint baseline: `./gradlew updateLintBaseline`
   - Check report: `app/build/reports/lint-results-debug.html`

2. **Test Failures**:
   - Run locally first: `./gradlew testDebugUnitTest`
   - Check test reports in `app/build/reports/tests/`

3. **Build Issues**:
   - Clean project: `./gradlew clean`
   - Sync dependencies: `./gradlew dependencies`

## 📞 Support

For questions about the testing setup or CI/CD pipeline:
1. Check existing test files for examples
2. Review GitHub Actions logs for detailed error information
3. Consult Android testing documentation for best practices

---

This testing infrastructure ensures robust, reliable development workflows while maintaining high code quality and performance standards for the BudgetBuddy application. 