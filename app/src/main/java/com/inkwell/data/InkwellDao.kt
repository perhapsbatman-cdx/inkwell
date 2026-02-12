package com.inkwell.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InkwellDao {
    @Query("SELECT * FROM documents ORDER BY orderIndex ASC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY orderIndex ASC")
    suspend fun getAllDocumentsOnce(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Query("UPDATE documents SET title = :title WHERE id = :documentId")
    suspend fun renameDocument(documentId: String, title: String)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM documents WHERE parentId IS :parentId")
    suspend fun maxOrderIndexInParent(parentId: String?): Int

    @Query("SELECT body FROM document_contents WHERE documentId = :documentId")
    fun observeBody(documentId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBody(content: DocumentContentEntity)

    @Query("DELETE FROM document_contents WHERE documentId = :documentId")
    suspend fun deleteBody(documentId: String)

    @Query("SELECT notes FROM document_notes WHERE documentId = :documentId")
    fun observeNotes(documentId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(note: DocumentNoteEntity)

    @Query("DELETE FROM document_notes WHERE documentId = :documentId")
    suspend fun deleteNotes(documentId: String)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun documentCount(): Int
}
