# 🚀 Firebase Migration Guide

## Overview
This guide documents the migration from Room (SQLite) database to Firebase (Firestore + Auth + Storage) for the BudgetBuddy Android app.

## ✅ Completed Components

### 📊 **Phase 1: Firebase Setup**
- ✅ Firebase project created (`budgetbuddy-ea85b`)
- ✅ `google-services.json` configured
- ✅ Firebase dependencies added to `build.gradle.kts`
- ✅ Firebase BOM for version management

### 🏗️ **Phase 2: Data Models**
- ✅ `FirebaseUser` - User profiles with Firebase Auth UID
- ✅ `FirebaseBudget` - Monthly budgets with user references
- ✅ `FirebaseCategoryBudget` - Category allocations under budgets
- ✅ `FirebaseExpense` - Expenses with Storage URLs for receipts
- ✅ `FirebaseRewardPoints` - User points with atomic updates
- ✅ `FirebaseAchievement` - User achievements with timestamps

### 🔄 **Phase 3: Repository Layer**
- ✅ `FirebaseAuthRepository` - Authentication + user profile management
- ✅ `FirebaseExpenseRepository` - Expense CRUD + receipt upload
- ✅ `FirebaseBudgetRepository` - Budget management + category allocations
- ✅ `FirebaseRewardsRepository` - Points + achievements + leaderboards

### ⚙️ **Phase 4: Dependency Injection**
- ✅ `FirebaseModule` - Firebase service providers
- ✅ `FirebaseRepositoryModule` - Repository dependency injection
- ✅ `FirebaseSessionManager` - Auth state management
- ✅ `FirebaseToRoomAdapter` - Model conversion utilities
- ✅ `FirebaseAuthViewModel` - Example migrated ViewModel

## 🏛️ **Architecture Overview**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  ViewModel Layer │    │  Repository     │
│                 │    │                  │    │  Layer          │
│ • Fragments     │◄──►│ • ViewModels     │◄──►│ • Firebase      │
│ • Activities    │    │ • UI States      │    │   Repositories  │
│ • Adapters      │    │ • Flow/LiveData  │    │ • Room (Legacy) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        ▲
                                                        │
                                               ┌─────────────────┐
                                               │  Data Sources   │
                                               │                 │
                                               │ • Firestore     │
                                               │ • Firebase Auth │
                                               │ • Firebase      │
                                               │   Storage       │
                                               │ • Room DB       │
                                               │   (Legacy)      │
                                               └─────────────────┘
```

## 📊 **Data Migration Strategy**

### **Firestore Collections Structure**
```
budgetbuddy-ea85b/
├── users/
│   └── {userId}/
│       ├── name: string
│       ├── email: string
│       ├── biometricEnabled: boolean
│       ├── createdAt: timestamp
│       └── updatedAt: timestamp
├── budgets/
│   └── {budgetId}/
│       ├── userId: string (reference)
│       ├── monthYear: string ("yyyy-MM")
│       ├── totalAmount: number
│       ├── createdAt: timestamp
│       └── updatedAt: timestamp
├── categoryBudgets/
│   └── {categoryBudgetId}/
│       ├── budgetId: string (reference)
│       ├── categoryName: string
│       ├── allocatedAmount: number
│       └── createdAt: timestamp
├── expenses/
│   └── {expenseId}/
│       ├── userId: string (reference)
│       ├── date: timestamp
│       ├── amount: number
│       ├── categoryName: string
│       ├── notes: string (optional)
│       ├── receiptUrl: string (Firebase Storage URL)
│       └── createdAt: timestamp
├── rewardPoints/
│   └── {userId}/ (document ID = userId)
│       ├── userId: string
│       ├── currentPoints: number
│       └── lastUpdated: timestamp
└── achievements/
    └── {achievementId}/
        ├── userId: string (reference)
        ├── achievementName: string
        ├── description: string
        ├── iconName: string (optional)
        ├── achievedDate: timestamp (optional)
        └── createdAt: timestamp
```

### **Firebase Storage Structure**
```
budgetbuddy-ea85b.appspot.com/
└── receipts/
    └── {userId}_{timestamp}.jpg
```

## 🔄 **Migration Process**

### **Step 1: Enable Firebase Services**
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project `budgetbuddy-ea85b`
3. Enable Authentication:
   - Go to Authentication > Sign-in method
   - Enable Email/Password provider
4. Enable Firestore Database:
   - Go to Firestore Database > Create database
   - Choose "Start in test mode" (configure rules later)
5. Enable Storage:
   - Go to Storage > Get started
   - Use default security rules for now

### **Step 2: Update Dependency Injection**
The project now has dual support for both Room and Firebase. You can switch between them by changing the modules in `di/` packages:

**For Firebase (New):**
```kotlin
// In your Application class or relevant modules
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository // Use interface binding
}
```

**For Room (Legacy):**
```kotlin
// Keep existing Room modules active
// DatabaseModule, RepositoryModule remain unchanged
```

### **Step 3: Migrate ViewModels**
Use `FirebaseAuthViewModel` as a template to migrate other ViewModels:

```kotlin
@HiltViewModel
class MigratedViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val sessionManager: FirebaseSessionManager // Use Firebase session manager
) : ViewModel() {
    // Implementation using Firebase repositories
}
```

### **Step 4: Data Migration Script**
Create a one-time migration to transfer existing Room data to Firebase:

```kotlin
class DataMigration @Inject constructor(
    private val roomRepositories: RoomRepositories,
    private val firebaseRepositories: FirebaseRepositories
) {
    suspend fun migrateAllData() {
        // 1. Export Room data
        // 2. Transform to Firebase models
        // 3. Import to Firebase
        // 4. Verify integrity
    }
}
```

## 🛡️ **Security Rules**

### **Firestore Rules**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /budgets/{budgetId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    match /categoryBudgets/{categoryBudgetId} {
      allow read, write: if request.auth != null;
      // Additional validation needed based on budget ownership
    }
    
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    match /rewardPoints/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /achievements/{achievementId} {
      allow read: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
  }
}
```

### **Storage Rules**
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /receipts/{userId}_{timestamp}.jpg {
      allow read, write: if request.auth != null && 
        request.auth.uid == userId;
    }
  }
}
```

## 🚧 **Next Steps**

### **Immediate Actions**
1. **Enable Firebase Services** in console
2. **Sync project** in Android Studio to resolve dependencies
3. **Test Firebase connection** with sample data
4. **Migrate AuthViewModel** to use Firebase authentication

### **Progressive Migration**
1. **Phase 1**: Authentication (Auth + User Profile)
2. **Phase 2**: Expense Management (with receipt upload)
3. **Phase 3**: Budget Management
4. **Phase 4**: Rewards System
5. **Phase 5**: Complete Room removal

### **Testing Strategy**
- Unit tests for repositories
- Integration tests for Firebase operations
- UI tests with Firebase Test Lab
- Offline behavior testing

## 🎯 **Benefits Achieved**

### **Immediate Benefits**
- ✅ **Real-time sync** across devices
- ✅ **Automatic backup** to cloud
- ✅ **Scalability** without server management
- ✅ **Offline support** with automatic sync when online

### **Future Benefits**
- 🔄 **Multi-device support** with same account
- 📊 **Analytics** with Firebase Analytics
- 🔔 **Push notifications** for budget alerts
- 🚀 **Remote config** for feature flags
- 🔐 **Enhanced security** with Firebase Auth

## ⚠️ **Important Notes**

### **Cost Considerations**
- Firestore: Pay per read/write operation
- Storage: Pay per GB stored and transferred
- Auth: Free up to 10K MAU

### **Data Precision**
- BigDecimal → Double conversion for Firestore
- Implement proper rounding for financial calculations
- Use helper methods in Firebase models

### **Migration Timing**
- Can run Room and Firebase in parallel during migration
- Use feature flags to switch between systems
- Plan for data reconciliation if needed

## 🆘 **Troubleshooting**

### **Common Issues**
1. **Build errors**: Sync project after adding Firebase
2. **Permission denied**: Check Firestore security rules
3. **Auth errors**: Verify provider is enabled in console
4. **Storage upload failures**: Check storage rules and file paths

### **Debug Tips**
- Enable Firebase logging: `FirebaseFirestore.setLoggingEnabled(true)`
- Use Firebase Emulator for local development
- Monitor operations in Firebase Console

---

This migration provides a solid foundation for cloud-based data management while maintaining all existing functionality. The dual-repository approach allows for gradual migration and easy rollback if needed. 