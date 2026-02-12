package com.inkwell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkwell.WritingMode
import com.inkwell.data.BinderNodeType
import com.inkwell.data.DocumentEntity
import com.inkwell.data.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val selectedNodeId: String? = null,
    val selectedDocumentId: String? = null,
    val editorText: String = "",
    val notesText: String = "",
    val mode: WritingMode = WritingMode.Standard,
    val durationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val timerRunning: Boolean = false,
)

class EditorViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var bodyObserverJob: Job? = null
    private var notesObserverJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            observeDocuments()
        }
    }

    private suspend fun observeDocuments() {
        repository.observeDocuments().collectLatest { nodes ->
            _uiState.update { state ->
                val selectedNodeId = state.selectedNodeId
                    ?.takeIf { id -> nodes.any { it.id == id } }
                    ?: nodes.firstOrNull()?.id

                val selectedNode = nodes.firstOrNull { it.id == selectedNodeId }
                val selectedDocumentId = when (selectedNode?.type) {
                    BinderNodeType.Document -> selectedNode.id
                    else -> null
                }

                state.copy(
                    documents = nodes,
                    selectedNodeId = selectedNodeId,
                    selectedDocumentId = selectedDocumentId,
                )
            }

            _uiState.value.selectedDocumentId?.let(::observeSelectedDocument)
            if (_uiState.value.selectedDocumentId == null) {
                bodyObserverJob?.cancel()
                notesObserverJob?.cancel()
                _uiState.update { it.copy(editorText = "", notesText = "") }
            }
        }
    }

    fun onModeChanged(mode: WritingMode) {
        val correctedDuration = if (mode == WritingMode.Sprint && _uiState.value.durationMinutes !in sprintDurations) {
            sprintDurations.first()
        } else {
            _uiState.value.durationMinutes
        }

        _uiState.update {
            it.copy(
                mode = mode,
                durationMinutes = correctedDuration,
                remainingSeconds = correctedDuration * 60,
                timerRunning = false,
            )
        }
        stopTimer()
    }

    fun onDurationSelected(minutes: Int) {
        _uiState.update {
            it.copy(durationMinutes = minutes, remainingSeconds = minutes * 60, timerRunning = false)
        }
        stopTimer()
    }

    fun onTimerToggle() {
        val shouldRun = !_uiState.value.timerRunning
        _uiState.update { it.copy(timerRunning = shouldRun) }
        if (shouldRun) startTimer() else stopTimer()
    }

    fun onNodeSelected(nodeId: String) {
        val node = _uiState.value.documents.firstOrNull { it.id == nodeId } ?: return
        _uiState.update {
            it.copy(
                selectedNodeId = nodeId,
                selectedDocumentId = if (node.type == BinderNodeType.Document) nodeId else null,
                timerRunning = false,
            )
        }
        stopTimer()
        if (node.type == BinderNodeType.Document) {
            observeSelectedDocument(nodeId)
        } else {
            bodyObserverJob?.cancel()
            notesObserverJob?.cancel()
            _uiState.update { it.copy(editorText = "", notesText = "") }
        }
    }

    fun onAddDocument() {
        val selectedNode = _uiState.value.documents.firstOrNull { it.id == _uiState.value.selectedNodeId }
        val parentId = when (selectedNode?.type) {
            BinderNodeType.Folder -> selectedNode.id
            BinderNodeType.Document -> selectedNode.parentId
            else -> null
        }

        viewModelScope.launch {
            val newId = repository.addDocument(parentId = parentId)
            _uiState.update { it.copy(selectedNodeId = newId, selectedDocumentId = newId, timerRunning = false) }
            stopTimer()
            observeSelectedDocument(newId)
        }
    }

    fun onAddFolder() {
        val selectedNode = _uiState.value.documents.firstOrNull { it.id == _uiState.value.selectedNodeId }
        val parentId = when (selectedNode?.type) {
            BinderNodeType.Folder -> selectedNode.id
            BinderNodeType.Document -> selectedNode.parentId
            else -> null
        }

        viewModelScope.launch {
            val newId = repository.addFolder(parentId = parentId)
            _uiState.update { it.copy(selectedNodeId = newId, selectedDocumentId = null, timerRunning = false) }
            stopTimer()
            bodyObserverJob?.cancel()
            notesObserverJob?.cancel()
            _uiState.update { it.copy(editorText = "", notesText = "") }
        }
    }



    fun onMoveSelectedUp() {
        val selectedId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch { repository.moveNodeUp(selectedId) }
    }

    fun onMoveSelectedDown() {
        val selectedId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch { repository.moveNodeDown(selectedId) }
    }

    fun onRenameSelectedNode(newTitle: String) {
        val selectedId = _uiState.value.selectedNodeId ?: return
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameDocument(selectedId, trimmed) }
    }

    fun onDeleteSelectedNode() {
        val selectedId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch {
            repository.deleteNodeRecursively(selectedId)
            _uiState.update { it.copy(timerRunning = false) }
            stopTimer()
        }
    }

    fun onEditorChanged(text: String) {
        _uiState.update { it.copy(editorText = text) }
        val docId = _uiState.value.selectedDocumentId ?: return
        viewModelScope.launch { repository.updateBody(docId, text) }

        val state = _uiState.value
        if ((state.mode == WritingMode.Standard || state.mode == WritingMode.Sprint) &&
            !state.timerRunning &&
            state.remainingSeconds == state.durationMinutes * 60
        ) {
            _uiState.update { it.copy(timerRunning = true) }
            startTimer()
        }
    }

    fun onNotesChanged(text: String) {
        _uiState.update { it.copy(notesText = text) }
        val docId = _uiState.value.selectedDocumentId ?: return
        viewModelScope.launch { repository.updateNotes(docId, text) }
    }

    private fun observeSelectedDocument(documentId: String) {
        bodyObserverJob?.cancel()
        notesObserverJob?.cancel()

        bodyObserverJob = viewModelScope.launch {
            repository.observeBody(documentId).collectLatest { body ->
                _uiState.update { it.copy(editorText = body) }
            }
        }

        notesObserverJob = viewModelScope.launch {
            repository.observeNotes(documentId).collectLatest { notes ->
                _uiState.update { it.copy(notesText = notes) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerRunning && _uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.update { state -> state.copy(remainingSeconds = state.remainingSeconds - 1) }
            }
            if (_uiState.value.remainingSeconds <= 0) {
                _uiState.update { it.copy(timerRunning = false) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    companion object {
        private val sprintDurations = listOf(5, 10, 15, 20, 25, 30)
    }
}

class EditorViewModelFactory(
    private val repository: ProjectRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
