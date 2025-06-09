package com.example.budgetbuddy.data.firebase.repository

import com.example.budgetbuddy.data.firebase.model.FirebaseBudget
import com.example.budgetbuddy.data.firebase.model.FirebaseCategoryBudget
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase budget repository that handles budget and category budget management using Firestore.
 * This replaces the Room-based BudgetRepository.
 */
@Singleton
class FirebaseBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val expenseRepository: FirebaseExpenseRepository
) {
    companion object {
        private const val BUDGETS_COLLECTION = "budgets"
        private const val CATEGORY_BUDGETS_COLLECTION = "categoryBudgets"
    }

    /**
     * Saves a budget with its category budgets.
     * Creates or updates the main budget and associated category budgets.
     */
    suspend fun saveBudget(
        userId: String,
        monthYear: String,
        totalAmount: BigDecimal,
        categoryBudgets: List<Pair<String, BigDecimal>>
    ): Result<String> {
        return try {
            android.util.Log.d("BudgetRepo", "Saving budget: userId=$userId, monthYear=$monthYear, totalAmount=$totalAmount")
            
            // Check if budget already exists for this month
            val existingBudget = getBudgetForMonthDirect(userId, monthYear)
            
            val budgetRef = if (existingBudget != null) {
                // Update existing budget
                val updates = mapOf(
                    "totalAmount" to totalAmount.toDouble(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection(BUDGETS_COLLECTION)
                    .document(existingBudget.id)
                    .update(updates)
                    .await()
                
                firestore.collection(BUDGETS_COLLECTION).document(existingBudget.id)
            } else {
                // Create new budget
                val budget = FirebaseBudget.fromBigDecimal(userId, monthYear, totalAmount)
                android.util.Log.d("BudgetRepo", "Creating new budget: $budget")
                val newBudgetRef = firestore.collection(BUDGETS_COLLECTION).document()
                newBudgetRef.set(budget).await()
                newBudgetRef
            }
            
            val budgetId = budgetRef.id
            
            // Delete existing category budgets for this budget
            deleteExistingCategoryBudgets(budgetId)
            
            // Create new category budgets - save ALL categories including those with zero amounts
            android.util.Log.d("BudgetRepo", "========== SAVING CATEGORY BUDGETS TO FIREBASE ==========")
            android.util.Log.d("BudgetRepo", "Budget ID: $budgetId")
            android.util.Log.d("BudgetRepo", "Total categories to save: ${categoryBudgets.size}")
            
            categoryBudgets.forEachIndexed { index, (categoryName, allocatedAmount) ->
                val categoryBudget = FirebaseCategoryBudget.fromBigDecimal(
                    budgetId = budgetId,
                    categoryName = categoryName,
                    allocatedAmount = allocatedAmount
                )
                android.util.Log.d("BudgetRepo", "[$index] SAVING: $categoryName = $allocatedAmount")
                android.util.Log.d("BudgetRepo", "[$index] Firebase object: $categoryBudget")
                
                val docRef = firestore.collection(CATEGORY_BUDGETS_COLLECTION).document()
                docRef.set(categoryBudget).await()
                
                android.util.Log.d("BudgetRepo", "[$index] SAVED to document: ${docRef.id}")
            }
            android.util.Log.d("BudgetRepo", "========== FINISHED SAVING CATEGORY BUDGETS ==========")
            
            
            android.util.Log.d("BudgetRepo", "Budget saved successfully with ID: $budgetId")
            Result.success(budgetId)
        } catch (e: Exception) {
            android.util.Log.e("BudgetRepo", "Failed to save budget", e)
            Result.failure(e)
        }
    }

    /**
     * Gets budget for a specific month as Flow for real-time updates.
     */
    fun getBudgetForMonth(userId: String, monthYear: String): Flow<FirebaseBudget?> = callbackFlow {
        val listener = firestore.collection(BUDGETS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("monthYear", monthYear)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val budget = snapshot?.documents?.firstOrNull()?.toObject(FirebaseBudget::class.java)
                trySend(budget)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets budget for a specific month directly (non-Flow).
     */
    suspend fun getBudgetForMonthDirect(userId: String, monthYear: String): FirebaseBudget? {
        return try {
            val snapshot = firestore.collection(BUDGETS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("monthYear", monthYear)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject(FirebaseBudget::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets category budgets for a specific budget.
     */
    fun getCategoryBudgetsForBudget(budgetId: String): Flow<List<FirebaseCategoryBudget>> = callbackFlow {
        android.util.Log.d("BudgetRepo", "===== LOADING CATEGORY BUDGETS FROM FIREBASE =====")
        android.util.Log.d("BudgetRepo", "Budget ID: $budgetId")
        
        val listener = firestore.collection(CATEGORY_BUDGETS_COLLECTION)
            .whereEqualTo("budgetId", budgetId)
            .orderBy("categoryName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BudgetRepo", "Error loading category budgets", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val categoryBudgets = snapshot?.toObjects(FirebaseCategoryBudget::class.java) ?: emptyList()
                android.util.Log.d("BudgetRepo", "LOADED ${categoryBudgets.size} category budgets from Firebase:")
                categoryBudgets.forEachIndexed { index, category ->
                    android.util.Log.d("BudgetRepo", "[$index] LOADED: ${category.categoryName} = ${category.allocatedAmount}")
                }
                android.util.Log.d("BudgetRepo", "===============================================")
                
                trySend(categoryBudgets)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets relevant category names for a period (categories that have budgets or expenses).
     */
    suspend fun getRelevantCategoryNamesForPeriod(userId: String, startDate: Date, endDate: Date): List<String> {
        // Get categories from expenses in the period
        val expenseCategories = expenseRepository.getUsedCategoriesInPeriod(userId, startDate, endDate)
        
        // Get categories from current month budget
        val monthYear = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(startDate)
        val budget = getBudgetForMonthDirect(userId, monthYear)
        val budgetCategories = if (budget != null) {
            getCategoryBudgetsForBudgetDirect(budget.id).map { it.categoryName }
        } else {
            emptyList()
        }
        
        return (expenseCategories + budgetCategories).distinct().sorted()
    }

    /**
     * Gets relevant category names as Flow for real-time updates.
     */
    fun getRelevantCategoryNamesForPeriodFlow(userId: String, startDate: Date, endDate: Date): Flow<List<String>> {
        val monthYear = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(startDate)
        
        return getBudgetForMonth(userId, monthYear).map { budget ->
            val budgetCategories = if (budget != null) {
                try {
                    getCategoryBudgetsForBudgetDirect(budget.id).map { it.categoryName }
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            budgetCategories.distinct().sorted()
        }
    }

    /**
     * Gets current budget categories for expense categorization.
     * Returns ALL categories (for Add Expense screen).
     */
    fun getCurrentBudgetCategories(userId: String): Flow<List<FirebaseCategoryBudget>> {
        val currentMonthYear = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(Date())
        
        return getBudgetForMonth(userId, currentMonthYear).flatMapLatest { budget ->
            if (budget != null) {
                // Get saved category budgets and merge with defaults for Add Expense
                getCategoryBudgetsForBudget(budget.id).map { savedCategories ->
                    mergeWithDefaultCategories(savedCategories)
                }
            } else {
                // Return ALL default categories if no budget exists
                flowOf(getDefaultCategories())
            }
        }
    }
    
    /**
     * Gets budget categories for home display.
     * Returns only categories that have allocated budgets > 0.
     */
    fun getBudgetCategoriesForHome(userId: String): Flow<List<FirebaseCategoryBudget>> {
        val currentMonthYear = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(Date())
        
        return getBudgetForMonth(userId, currentMonthYear).flatMapLatest { budget ->
            if (budget != null) {
                // Get all saved categories and filter to only those with budget > 0
                getCategoryBudgetsForBudget(budget.id).map { allCategories ->
                    val categoriesWithBudget = allCategories.filter { it.allocatedAmount > 0.0 }
                    android.util.Log.d("BudgetRepo", "Home display: Found ${allCategories.size} total categories, ${categoriesWithBudget.size} with budgets > 0")
                    categoriesWithBudget.forEach { category ->
                        android.util.Log.d("BudgetRepo", "Home category: ${category.categoryName} = ${category.allocatedAmount}")
                    }
                    categoriesWithBudget
                }
            } else {
                android.util.Log.d("BudgetRepo", "No budget found for home display")
                // Return empty list if no budget exists
                flowOf(emptyList())
            }
        }
    }
    
    /**
     * Merges saved category budgets with default categories to ensure all categories are shown.
     */
    private fun mergeWithDefaultCategories(savedCategories: List<FirebaseCategoryBudget>): List<FirebaseCategoryBudget> {
        val defaultCategories = getDefaultCategories()
        val savedCategoryMap = savedCategories.associateBy { it.categoryName }
        
        return defaultCategories.map { defaultCategory ->
            // Use saved category if it exists, otherwise use default with 0 allocation
            savedCategoryMap[defaultCategory.categoryName] ?: defaultCategory
        }
    }

    /**
     * Returns default categories when no budget exists.
     * Updated to include ALL standard budget categories.
     */
    private fun getDefaultCategories(): List<FirebaseCategoryBudget> {
        return listOf(
            FirebaseCategoryBudget(categoryName = "Food & Dining", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Transportation", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Shopping", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Entertainment", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Bills & Utilities", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Healthcare", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Education", allocatedAmount = 0.0),
            FirebaseCategoryBudget(categoryName = "Travel", allocatedAmount = 0.0)
        )
    }

    /**
     * Gets all budgets for a user.
     */
    fun getAllBudgets(userId: String): Flow<List<FirebaseBudget>> = callbackFlow {
        val listener = firestore.collection(BUDGETS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("monthYear", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val budgets = snapshot?.toObjects(FirebaseBudget::class.java) ?: emptyList()
                trySend(budgets)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Deletes a budget and all its category budgets.
     */
    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            // Delete category budgets first
            deleteExistingCategoryBudgets(budgetId)
            
            // Delete main budget
            firestore.collection(BUDGETS_COLLECTION)
                .document(budgetId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates budget total amount.
     */
    suspend fun updateBudgetTotal(budgetId: String, newTotalAmount: BigDecimal): Result<Unit> {
        return try {
            val updates = mapOf(
                "totalAmount" to newTotalAmount.toDouble(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(BUDGETS_COLLECTION)
                .document(budgetId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets category budget allocation for a specific category.
     */
    suspend fun getCategoryBudgetAllocation(budgetId: String, categoryName: String): BigDecimal {
        return try {
            val snapshot = firestore.collection(CATEGORY_BUDGETS_COLLECTION)
                .whereEqualTo("budgetId", budgetId)
                .whereEqualTo("categoryName", categoryName)
                .limit(1)
                .get()
                .await()
            
            val categoryBudget = snapshot.documents.firstOrNull()?.toObject(FirebaseCategoryBudget::class.java)
            categoryBudget?.getAllocatedAmountAsBigDecimal() ?: BigDecimal.ZERO
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    /**
     * Gets budget summary (total budget vs total spending) for a month.
     */
    suspend fun getBudgetSummary(userId: String, monthYear: String): BudgetSummary {
        val budget = getBudgetForMonthDirect(userId, monthYear)
        val totalBudget = budget?.getTotalAmountAsBigDecimal() ?: BigDecimal.ZERO
        
        // Calculate total spending for the month
        val startDate = getStartOfMonth(monthYear)
        val endDate = getEndOfMonth(monthYear)
        val totalSpent = expenseRepository.getTotalSpendingBetween(userId, startDate, endDate)
        
        return BudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = totalBudget - totalSpent,
            percentageUsed = if (totalBudget > BigDecimal.ZERO) {
                (totalSpent.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toInt()
            } else 0
        )
    }

    /**
     * Deletes existing category budgets for a budget.
     */
    private suspend fun deleteExistingCategoryBudgets(budgetId: String) {
        val existingCategoryBudgets = getCategoryBudgetsForBudgetDirect(budgetId)
        existingCategoryBudgets.forEach { categoryBudget ->
            firestore.collection(CATEGORY_BUDGETS_COLLECTION)
                .document(categoryBudget.id)
                .delete()
                .await()
        }
    }

    /**
     * Gets category budgets for a budget directly (non-Flow).
     */
    suspend fun getCategoryBudgetsForBudgetDirect(budgetId: String): List<FirebaseCategoryBudget> {
        return try {
            val snapshot = firestore.collection(CATEGORY_BUDGETS_COLLECTION)
                .whereEqualTo("budgetId", budgetId)
                .get()
                .await()
            
            snapshot.toObjects(FirebaseCategoryBudget::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Helper to get start of month date.
     */
    private fun getStartOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Helper to get end of month date.
     */
    private fun getEndOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        return calendar.time
    }
}

/**
 * Data class for budget summary information.
 */
data class BudgetSummary(
    val totalBudget: BigDecimal,
    val totalSpent: BigDecimal,
    val remaining: BigDecimal,
    val percentageUsed: Int
) 