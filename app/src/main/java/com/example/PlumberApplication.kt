package com.example

import android.app.Application
import com.example.data.db.MaterialDatabase
import com.example.data.db.PlumberDatabase
import com.example.data.repository.MaterialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlumberApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var materialDatabase: MaterialDatabase
        private set

    lateinit var materialRepository: MaterialRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize MaterialDatabase & MaterialRepository using Room
        materialDatabase = MaterialDatabase.getDatabase(this)
        materialRepository = MaterialRepository(materialDatabase.materialDao())

        // Initialize PlumberDatabase as well
        val plumberDb = PlumberDatabase.getDatabase(this)

        // Asynchronously import materials.json into Room database on Application startup
        applicationScope.launch {
            materialRepository.initializeLibraryIfEmpty(this@PlumberApplication)

            // Also seed into PlumberDatabase if empty to keep compatibility
            val plumberMaterialDao = plumberDb.materialDao()
            if (plumberMaterialDao.getMaterialCount() == 0) {
                val plumberMatRepo = MaterialRepository(plumberMaterialDao)
                plumberMatRepo.initializeLibraryIfEmpty(this@PlumberApplication)
            }
        }
    }
}
