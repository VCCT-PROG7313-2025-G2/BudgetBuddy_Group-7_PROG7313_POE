# 🔥 Firebase Setup Verification Checklist

## ✅ **Quick Setup Check**

### **1. Firebase Console Access**
- [ ] Can access https://console.firebase.google.com
- [ ] Project `budgetbuddy-ea85b` is visible
- [ ] Project shows "Active" status

### **2. Authentication Setup**
- [ ] **Authentication** section is accessible
- [ ] **Email/Password** sign-in method is **ENABLED**
- [ ] Test user can be created (optional)

### **3. Firestore Database**
- [ ] **Firestore Database** is created (not Realtime Database)
- [ ] Database shows "Cloud Firestore" tab
- [ ] Database is in **Production mode** or **Test mode**

### **4. Firebase Storage**
- [ ] **Storage** section shows a bucket
- [ ] Bucket name: `budgetbuddy-ea85b.firebasestorage.app`
- [ ] Storage rules are configured

### **5. Security Rules**

#### **Firestore Rules** (Firestore → Rules tab):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /budgets/{budgetId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    match /rewardPoints/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /achievements/{achievementId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    // Read-only leaderboard access
    match /rewardPoints/{document=**} {
      allow read: if request.auth != null;
    }
  }
}
```

#### **Storage Rules** (Storage → Rules tab):
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /receipts/{userId}/{receiptId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### **6. App Configuration**
- [ ] `google-services.json` is in `app/` directory ✅ **VERIFIED**
- [ ] Project ID matches: `budgetbuddy-ea85b` ✅ **VERIFIED**
- [ ] Package name matches: `com.example.budgetbuddy` ✅ **VERIFIED**

## 🚨 **Common Issues & Solutions**

### **Issue: Authentication Not Working**
**Solution:**
1. Ensure Email/Password is enabled in Authentication → Sign-in method
2. Check that `google-services.json` is in the correct location
3. Rebuild the project after adding `google-services.json`

### **Issue: Firestore Permission Denied**
**Solution:**
1. Check that user is authenticated before accessing Firestore
2. Verify security rules allow the specific operation
3. Ensure user ID matches in the rules

### **Issue: Storage Upload Fails**
**Solution:**
1. Verify Firebase Storage is enabled
2. Check storage security rules
3. Ensure user is authenticated

### **Issue: App Can't Connect to Firebase**
**Solution:**
1. Verify internet connection
2. Check `google-services.json` configuration
3. Rebuild project and clear cache
4. Check Firebase project status

## 🧪 **Test Your Setup**

### **Manual Testing Steps:**

1. **Run the app** in Android Studio
2. **Create a new account** with email/password
3. **Login** with the created account
4. **Add an expense** with a receipt photo
5. **Create a budget** with categories
6. **Check Firestore** in Firebase Console for data
7. **Check Storage** for uploaded receipt
8. **Verify Authentication** shows the user

### **Expected Firebase Console Data:**

After testing, you should see:

**Authentication → Users:**
```
User ID: (Firebase-generated)
Email: your-test-email@example.com
Created: (timestamp)
```

**Firestore → Data:**
```
users/{userId}
├── name: "Test User"
├── email: "test@example.com"
├── biometricLoginEnabled: false
└── createdAt: (timestamp)

budgets/{budgetId}
├── userId: "{userId}"
├── monthYear: "2024-06"
├── totalAmount: 2000
└── categoryBudgets: [...]

expenses/{expenseId}
├── userId: "{userId}"
├── amount: 25.50
├── categoryName: "Food"
├── date: (timestamp)
└── receiptUrl: "https://..."
```

**Storage → Files:**
```
receipts/{userId}/{expenseId}.jpg
```

## ✅ **Success Indicators**

Your Firebase setup is correct when:

- [ ] Users can sign up and login
- [ ] Data appears in Firestore after app usage
- [ ] Receipt photos upload to Storage
- [ ] Real-time updates work across devices
- [ ] No permission errors in console logs

## 📞 **Need Help?**

If you encounter issues:

1. **Check Firebase Console logs**: Firebase Console → Analytics → DebugView
2. **Review security rules**: Ensure they match the provided rules
3. **Verify service enablement**: All three services (Auth, Firestore, Storage) are enabled
4. **Test with simple operations**: Start with just authentication, then add data operations

---

## 🎉 **Ready to Go!**

Once all items are checked, your Firebase setup is complete and ready for the migrated BudgetBuddy app! 🚀 