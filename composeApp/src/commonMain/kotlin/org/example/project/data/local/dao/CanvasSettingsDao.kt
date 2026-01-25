package org.example.project.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.example.project.data.local.entity.CanvasSettingsEntity

/**
 * Data Access Object for canvas settings persistence.
 * Each folder has its own independent canvas settings.
 */
@Dao
interface CanvasSettingsDao {
    /**
     * Observes canvas settings for a specific folder.
     * Returns a Flow that emits whenever settings change.
     * @param folderId The folder ID ("ALL_DRAWINGS" for no folder)
     */
    @Query("SELECT * FROM canvas_settings WHERE folderId = :folderId")
    fun observeCanvasSettings(folderId: String): Flow<CanvasSettingsEntity?>

    /**
     * Gets canvas settings for a specific folder.
     * Returns null if no settings have been saved yet for this folder.
     * @param folderId The folder ID ("ALL_DRAWINGS" for no folder)
     */
    @Query("SELECT * FROM canvas_settings WHERE folderId = :folderId")
    suspend fun getCanvasSettings(folderId: String): CanvasSettingsEntity?

    /**
     * Inserts or updates canvas settings for a folder (upsert operation).
     * Uses REPLACE strategy to update existing settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: CanvasSettingsEntity)
}
