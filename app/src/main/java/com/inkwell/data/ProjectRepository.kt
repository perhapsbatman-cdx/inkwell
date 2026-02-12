package com.inkwell.data

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(
    private val dao: InkwellDao,
) {
    fun observeDocuments(): Flow<List<DocumentEntity>> = dao.observeDocuments()

    fun observeBody(documentId: String): Flow<String> = dao.observeBody(documentId).map { it.orEmpty() }

    fun observeNotes(documentId: String): Flow<String> = dao.observeNotes(documentId).map { it.orEmpty() }

    suspend fun updateBody(documentId: String, body: String) {
        dao.upsertBody(DocumentContentEntity(documentId = documentId, body = body))
    }

    suspend fun updateNotes(documentId: String, notes: String) {
        dao.upsertNotes(DocumentNoteEntity(documentId = documentId, notes = notes))
    }

    suspend fun addDocument(parentId: String?, title: String = "Untitled"): String {
        val newId = UUID.randomUUID().toString()
        val nextOrder = dao.maxOrderIndexInParent(parentId) + 1

        dao.upsertDocument(
            DocumentEntity(
                id = newId,
                title = title,
                type = BinderNodeType.Document,
                parentId = parentId,
                orderIndex = nextOrder,
            )
        )
        dao.upsertBody(DocumentContentEntity(documentId = newId, body = ""))
        dao.upsertNotes(DocumentNoteEntity(documentId = newId, notes = ""))
        return newId
    }

    suspend fun addFolder(parentId: String?, title: String = "New Folder"): String {
        val newId = UUID.randomUUID().toString()
        val nextOrder = dao.maxOrderIndexInParent(parentId) + 1

        dao.upsertDocument(
            DocumentEntity(
                id = newId,
                title = title,
                type = BinderNodeType.Folder,
                parentId = parentId,
                orderIndex = nextOrder,
            )
        )
        return newId
    }

    suspend fun renameDocument(documentId: String, title: String) {
        dao.renameDocument(documentId = documentId, title = title)
    }



    suspend fun moveNodeUp(nodeId: String) {
        val nodes = dao.getAllDocumentsOnce()
        val node = nodes.firstOrNull { it.id == nodeId } ?: return
        val siblings = nodes.filter { it.parentId == node.parentId }.sortedBy { it.orderIndex }
        val index = siblings.indexOfFirst { it.id == nodeId }
        if (index <= 0) return

        val prev = siblings[index - 1]
        val current = siblings[index]
        val updated = nodes.map {
            when (it.id) {
                current.id -> current.copy(orderIndex = prev.orderIndex)
                prev.id -> prev.copy(orderIndex = current.orderIndex)
                else -> it
            }
        }
        dao.upsertDocuments(updated)
    }

    suspend fun moveNodeDown(nodeId: String) {
        val nodes = dao.getAllDocumentsOnce()
        val node = nodes.firstOrNull { it.id == nodeId } ?: return
        val siblings = nodes.filter { it.parentId == node.parentId }.sortedBy { it.orderIndex }
        val index = siblings.indexOfFirst { it.id == nodeId }
        if (index == -1 || index >= siblings.lastIndex) return

        val next = siblings[index + 1]
        val current = siblings[index]
        val updated = nodes.map {
            when (it.id) {
                current.id -> current.copy(orderIndex = next.orderIndex)
                next.id -> next.copy(orderIndex = current.orderIndex)
                else -> it
            }
        }
        dao.upsertDocuments(updated)
    }

    suspend fun deleteNodeRecursively(nodeId: String) {
        val nodes = dao.getAllDocumentsOnce()
        val childrenByParent = nodes.groupBy { it.parentId }

        fun collectSubtreeIds(startId: String, acc: MutableList<String>) {
            acc += startId
            childrenByParent[startId].orEmpty().forEach { child ->
                collectSubtreeIds(child.id, acc)
            }
        }

        val idsToDelete = mutableListOf<String>()
        collectSubtreeIds(nodeId, idsToDelete)

        val byId = nodes.associateBy { it.id }
        idsToDelete.forEach { id ->
            if (byId[id]?.type == BinderNodeType.Document) {
                dao.deleteBody(id)
                dao.deleteNotes(id)
            }
            dao.deleteDocument(id)
        }
    }

    suspend fun seedIfEmpty() {
        if (dao.documentCount() > 0) return

        val manuscriptFolder = "folder-manuscript"
        val researchFolder = "folder-research"

        val nodes = listOf(
            DocumentEntity(id = manuscriptFolder, title = "Manuscript", type = BinderNodeType.Folder, parentId = null, orderIndex = 0),
            DocumentEntity(id = researchFolder, title = "Research", type = BinderNodeType.Folder, parentId = null, orderIndex = 1),
            DocumentEntity(id = "1", title = "Chapter 1", type = BinderNodeType.Document, parentId = manuscriptFolder, orderIndex = 0),
            DocumentEntity(id = "2", title = "Chapter 2", type = BinderNodeType.Document, parentId = manuscriptFolder, orderIndex = 1),
            DocumentEntity(id = "3", title = "Scene: Alley", type = BinderNodeType.Document, parentId = researchFolder, orderIndex = 0),
        )
        dao.upsertDocuments(nodes)
        dao.upsertBody(DocumentContentEntity(documentId = "1", body = ""))
        dao.upsertBody(DocumentContentEntity(documentId = "2", body = ""))
        dao.upsertBody(DocumentContentEntity(documentId = "3", body = ""))
        dao.upsertNotes(DocumentNoteEntity(documentId = "1", notes = "Hero motivation: unresolved guilt"))
        dao.upsertNotes(DocumentNoteEntity(documentId = "2", notes = "Introduce conflict escalations"))
        dao.upsertNotes(DocumentNoteEntity(documentId = "3", notes = "Reference weather details"))
    }
}
