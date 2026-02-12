package com.inkwell.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object BinderNodeType {
    const val Folder = "FOLDER"
    const val Document = "DOCUMENT"
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val parentId: String?,
    val orderIndex: Int,
)

@Entity(tableName = "document_contents")
data class DocumentContentEntity(
    @PrimaryKey val documentId: String,
    val body: String,
)

@Entity(tableName = "document_notes")
data class DocumentNoteEntity(
    @PrimaryKey val documentId: String,
    val notes: String,
)
