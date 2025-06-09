package com.example.budgetbuddy.di

import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseBudgetRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Firebase repositories for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseRepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseExpenseRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): FirebaseExpenseRepository {
        return FirebaseExpenseRepository(firestore, storage)
    }

    @Provides
    @Singleton
    fun provideFirebaseRewardsRepository(
        firestore: FirebaseFirestore
    ): FirebaseRewardsRepository {
        return FirebaseRewardsRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideFirebaseBudgetRepository(
        firestore: FirebaseFirestore,
        expenseRepository: FirebaseExpenseRepository
    ): FirebaseBudgetRepository {
        return FirebaseBudgetRepository(firestore, expenseRepository)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        firebaseStorage: FirebaseStorage
    ): FirebaseAuthRepository {
        return FirebaseAuthRepository(firebaseAuth, firestore, firebaseStorage)
    }
} 