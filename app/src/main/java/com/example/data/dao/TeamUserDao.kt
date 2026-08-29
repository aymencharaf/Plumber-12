package com.example.data.dao

import androidx.room.*
import com.example.data.model.TeamUser
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamUserDao {
    @Query("SELECT * FROM team_users ORDER BY name ASC")
    fun getAllTeamUsers(): Flow<List<TeamUser>>

    @Query("SELECT * FROM team_users")
    suspend fun getAllTeamUsersDirect(): List<TeamUser>

    @Query("DELETE FROM team_users")
    suspend fun deleteAllTeamUsersDirect()

    @Query("SELECT * FROM team_users WHERE role = 'WORKER' ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<TeamUser>>

    @Query("SELECT * FROM team_users WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): TeamUser?

    @Query("SELECT * FROM team_users WHERE phone = :phone OR email = :email LIMIT 1")
    suspend fun getUserByPhoneOrEmail(phone: String, email: String): TeamUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: TeamUser)

    @Query("UPDATE team_users SET active = :active WHERE uid = :uid")
    suspend fun setUserActive(uid: String, active: Boolean)

    @Query("UPDATE team_users SET password = :newPassword WHERE uid = :uid")
    suspend fun updatePassword(uid: String, newPassword: String)

    @Delete
    suspend fun deleteUser(user: TeamUser)

    @Query("DELETE FROM team_users WHERE uid = :uid")
    suspend fun deleteUserByUid(uid: String)
}
