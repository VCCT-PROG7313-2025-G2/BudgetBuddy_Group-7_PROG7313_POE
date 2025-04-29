package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "reward_points",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["userId"], unique = true)]
)
data class RewardPointsEntity(
    @PrimaryKey(autoGenerate = true) val pointsId: Long = 0,
    val userId: Long,
    val currentPoints: Int = 0
) 