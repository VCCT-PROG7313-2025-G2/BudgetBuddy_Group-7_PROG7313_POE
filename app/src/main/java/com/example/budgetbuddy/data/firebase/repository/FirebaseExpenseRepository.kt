package com.example.budgetbuddy.data.firebase.repository

import com.example.budgetbuddy.data.firebase.model.FirebaseExpense
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import com.google.firebase.Timestamp
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Firebase expense repository that handles expense management using Firestore.
 * This replaces the Room-based ExpenseRepository.
 */
@Singleton
class FirebaseExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    companion object {
        private const val EXPENSES_COLLECTION = "expenses"
        private const val RECEIPTS_FOLDER = "receipts"
    }

    /**
     * Inserts a new expense into Firestore.
     * If receiptUri is provided, uploads to Firebase Storage first.
     */
    suspend fun insertExpense(
        userId: String,
        amount: BigDecimal,
        categoryName: String,
        date: Date,
        notes: String? = null,
        receiptUri: Uri? = null
    ): Result<String> {
        return try {
            // Upload receipt if provided
            val receiptUrl = receiptUri?.let { uploadReceipt(userId, it) }
            
            // Create expense document
            val expense = FirebaseExpense.fromBigDecimalAndDate(
                userId = userId,
                date = date,
                amount = amount,
                categoryName = categoryName,
                notes = notes,
                receiptUrl = receiptUrl
            )
            
            val documentRef = firestore.collection(EXPENSES_COLLECTION).document()
            documentRef.set(expense).await()
            
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all expenses for a user as a Flow for real-time updates.
     */
    fun getAllExpenses(userId: String): Flow<List<FirebaseExpense>> = callbackFlow {
        val listener = firestore.collection(EXPENSES_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.toObjects(FirebaseExpense::class.java) ?: emptyList()
                trySend(expenses)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets all expenses between two dates - this is what powers the spending chart.
     * Returns a Flow so the UI updates automatically when expenses change.
     */
    fun getExpensesBetween(userId: String, startDate: Date, endDate: Date): Flow<List<FirebaseExpense>> = callbackFlow {
        Log.d("ExpenseRepository", "Setting up expense stream for user $userId from $startDate to $endDate")
        
        val listener = firestore.collection(EXPENSES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
            .whereLessThanOrEqualTo("date", Timestamp(endDate))
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Error listening for expenses", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.toObjects(FirebaseExpense::class.java) ?: emptyList()
                Log.d("ExpenseRepository", "Found ${expenses.size} expenses in the date range")
                trySend(expenses)
            }
        
        awaitClose { 
            Log.d("ExpenseRepository", "Cleaning up expense listener")
            listener.remove() 
        }
    }

    /**
     * Gets total spending between two dates for a user.
     */
    suspend fun getTotalSpendingBetween(userId: String, startDate: Date, endDate: Date): BigDecimal {
        return try {
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
                .whereLessThanOrEqualTo("date", Timestamp(endDate))
                .get()
                .await()
            
            val expenses = snapshot.toObjects(FirebaseExpense::class.java)
            expenses.sumOf { it.getAmountAsBigDecimal() }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    /**
     * Gets spending by category between two dates.
     */
    suspend fun getSpendingByCategoryBetween(userId: String, startDate: Date, endDate: Date): Map<String, BigDecimal> {
        return try {
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
                .whereLessThanOrEqualTo("date", Timestamp(endDate))
                .get()
                .await()
            
            val expenses = snapshot.toObjects(FirebaseExpense::class.java)
            expenses.groupBy { it.categoryName }
                .mapValues { (_, expenseList) ->
                    expenseList.sumOf { it.getAmountAsBigDecimal() }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Gets spending by category between dates as Flow for real-time updates.
     */
    fun getSpendingByCategoryBetweenFlow(userId: String, startDate: Date, endDate: Date): Flow<Map<String, BigDecimal>> = callbackFlow {
        val listener = firestore.collection(EXPENSES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
            .whereLessThanOrEqualTo("date", Timestamp(endDate))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.toObjects(FirebaseExpense::class.java) ?: emptyList()
                val categorySpending = expenses.groupBy { it.categoryName }
                    .mapValues { (_, expenseList) ->
                        expenseList.sumOf { it.getAmountAsBigDecimal() }
                    }
                trySend(categorySpending)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Deletes an expense by ID.
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            // First get the expense to check for receipt URL
            val expense = firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .get()
                .await()
                .toObject(FirebaseExpense::class.java)
            
            // Delete receipt from storage if exists
            expense?.receiptUrl?.let { deleteReceipt(it) }
            
            // Delete expense document
            firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing expense.
     */
    suspend fun updateExpense(
        expenseId: String,
        amount: BigDecimal,
        categoryName: String,
        date: Date,
        notes: String? = null,
        newReceiptUri: Uri? = null
    ): Result<Unit> {
        return try {
            // Get existing expense
            val existingExpense = firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .get()
                .await()
                .toObject(FirebaseExpense::class.java)
            
            if (existingExpense == null) {
                return Result.failure(Exception("Expense not found"))
            }
            
            // Handle receipt update
            var receiptUrl = existingExpense.receiptUrl
            if (newReceiptUri != null) {
                // Delete old receipt if exists
                existingExpense.receiptUrl?.let { deleteReceipt(it) }
                // Upload new receipt
                receiptUrl = uploadReceipt(existingExpense.userId, newReceiptUri)
            }
            
            // Update expense
            val updates = mapOf(
                "amount" to amount.toDouble(),
                "categoryName" to categoryName,
                "date" to Timestamp(date),
                "notes" to notes,
                "receiptUrl" to receiptUrl
            )
            
            firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a receipt image to Firebase Storage.
     */
    private suspend fun uploadReceipt(userId: String, receiptUri: Uri): String {
        val timestamp = System.currentTimeMillis()
        val fileName = "${userId}_${timestamp}.jpg"
        val storageRef = storage.reference.child("$RECEIPTS_FOLDER/$fileName")
        
        val uploadTask = storageRef.putFile(receiptUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    /**
     * Deletes a receipt from Firebase Storage.
     */
    private suspend fun deleteReceipt(receiptUrl: String) {
        try {
            val storageRef = storage.getReferenceFromUrl(receiptUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            // Log but don't fail - file might already be deleted
        }
    }

    /**
     * Gets categories that have expenses in a given period.
     */
    suspend fun getUsedCategoriesInPeriod(userId: String, startDate: Date, endDate: Date): List<String> {
        return try {
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
                .whereLessThanOrEqualTo("date", Timestamp(endDate))
                .get()
                .await()
            
            val expenses = snapshot.toObjects(FirebaseExpense::class.java)
            expenses.map { it.categoryName }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets all expenses between dates directly (non-Flow)
     */
    suspend fun getExpensesBetweenDates(userId: String, startDate: Date, endDate: Date): List<FirebaseExpense> {
        return try {
            android.util.Log.d("FirebaseExpenseRepository", "=== Getting expenses between dates ===")
            android.util.Log.d("FirebaseExpenseRepository", "User ID: $userId")
            android.util.Log.d("FirebaseExpenseRepository", "Start date: $startDate")
            android.util.Log.d("FirebaseExpenseRepository", "End date: $endDate")
            
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(startDate))
                .whereLessThanOrEqualTo("date", com.google.firebase.Timestamp(endDate))
                .get()
                .await()
            
            val expenses = snapshot.toObjects(FirebaseExpense::class.java)
            android.util.Log.d("FirebaseExpenseRepository", "Found ${expenses.size} expenses")
            
            // Log all expenses found (for debugging)
            expenses.forEachIndexed { index, expense ->
                android.util.Log.d("FirebaseExpenseRepository", "Expense $index: userId=${expense.userId}, date=${expense.date.toDate()}, amount=${expense.amount}, category=${expense.categoryName}")
            }
            
            // Also check if there are ANY expenses for this user (regardless of date)
            val allExpensesSnapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val allExpenses = allExpensesSnapshot.toObjects(FirebaseExpense::class.java)
            android.util.Log.d("FirebaseExpenseRepository", "Total expenses for user $userId: ${allExpenses.size}")
            
            allExpenses.forEach { expense ->
                android.util.Log.d("FirebaseExpenseRepository", "All expense: date=${expense.date.toDate()}, amount=${expense.amount}, category=${expense.categoryName}")
            }
            
            // ALSO check if there are ANY expenses in the entire collection (debug only)
            val globalExpensesSnapshot = firestore.collection(EXPENSES_COLLECTION)
                .get()
                .await()
            val globalExpenses = globalExpensesSnapshot.toObjects(FirebaseExpense::class.java)
            android.util.Log.d("FirebaseExpenseRepository", "=== GLOBAL EXPENSES CHECK ===")
            android.util.Log.d("FirebaseExpenseRepository", "Total expenses in entire collection: ${globalExpenses.size}")
            
            globalExpenses.forEach { expense ->
                android.util.Log.d("FirebaseExpenseRepository", "Global expense: userId=${expense.userId}, date=${expense.date.toDate()}, amount=${expense.amount}, category=${expense.categoryName}")
            }
            
            expenses
        } catch (e: Exception) {
            android.util.Log.e("FirebaseExpenseRepository", "Error getting expenses between dates", e)
            emptyList()
        }
    }
} 