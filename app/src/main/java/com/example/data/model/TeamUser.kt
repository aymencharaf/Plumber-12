package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_users")
data class TeamUser(
    @PrimaryKey
    val uid: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "WORKER", // ADMIN, WORKER
    val active: Boolean = true,
    val workshopId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    fun isAdmin(): Boolean = role.uppercase() == "ADMIN"
    fun isWorker(): Boolean = role.uppercase() == "WORKER"
}
