package org.example.project.data.repository

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.data.local.dao.FolderDao
import org.example.project.data.local.entity.FolderEntity
import org.example.project.domain.model.Folder

/**
 * Repository for managing folders
 */
class FolderRepository(private val folderDao: FolderDao) {

    fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getFolderById(id: String): Folder? {
        return folderDao.getFolderById(id)?.toDomain()
    }

    suspend fun insertFolder(folder: Folder) {
        folderDao.insertFolder(folder.toEntity())
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.toEntity())
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolderById(folder.id)
    }

    suspend fun deleteFolderById(folderId: String) {
        folderDao.deleteFolderById(folderId)
    }

    // Conversion extensions
    private fun FolderEntity.toDomain(): Folder {
        return Folder(
            id = id,
            name = name,
            color = Color(colorValue.toULong()),
            createdAt = createdAt
        )
    }

    private fun Folder.toEntity(): FolderEntity {
        return FolderEntity(
            id = id,
            name = name,
            colorValue = color.value.toLong(),
            createdAt = createdAt
        )
    }
}
