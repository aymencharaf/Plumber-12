package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Appointment
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY id DESC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointmentsDirect(): List<Appointment>

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointmentsDirect()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
