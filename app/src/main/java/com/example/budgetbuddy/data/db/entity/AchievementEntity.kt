package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.budgetbuddy.data.db.converter.DateConverter // Assuming converter exists
import java.util.Date

@Entity(tableName = "achievements",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["userId"])]
)
@TypeConverters(DateConverter::class)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val achievementId: Long = 0,
    val userId: Long,
    val achievementName: String, // e.g., "Saved $100", "Budgeted 3 months"
    val description: String,
    val iconName: String?, // Name of a drawable resource
    val achievedDate: Date? // Null if not yet achieved
) 