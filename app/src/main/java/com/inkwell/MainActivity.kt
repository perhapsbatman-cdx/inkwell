package com.inkwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkwell.data.BinderNodeType
import com.inkwell.data.DocumentEntity
import com.inkwell.data.InkwellDatabase
import com.inkwell.data.ProjectRepository
import com.inkwell.ui.EditorViewModel
import com.inkwell.ui.EditorViewModelFactory

enum class WritingMode { Standard, Sprint, Editing }

data class BinderTreeItem(
    val node: DocumentEntity,
    val depth: Int,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ProjectRepository(InkwellDatabase.getInstance(this).dao())

        setContent {
            val viewModel: EditorViewModel = viewModel(factory = EditorViewModelFactory(repository))
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InkwellApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun InkwellApp(viewModel: EditorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val standardDurations = (5..60 step 5).toList()
    val sprintDurations = listOf(5, 10, 15, 20, 25, 30)

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        TopControls(
            mode = uiState.mode,
            durationMinutes = uiState.durationMinutes,
            remainingSeconds = uiState.remainingSeconds,
            onModeChange = viewModel::onModeChanged,
            onDurationSelected = viewModel::onDurationSelected,
            onTimerToggle = viewModel::onTimerToggle,
            allowedDurations = if (uiState.mode == WritingMode.Sprint) sprintDurations else standardDurations,
            timerRunning = uiState.timerRunning,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.mode == WritingMode.Sprint) {
            SprintView(
                editorText = uiState.editorText,
                onEditorChange = viewModel::onEditorChanged,
                remainingSeconds = uiState.remainingSeconds,
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinePane(
                documents = uiState.documents,
                selectedNodeId = uiState.selectedNodeId,
                onSelected = { viewModel.onNodeSelected(it.id) },
                onAddDocument = viewModel::onAddDocument,
                onAddFolder = viewModel::onAddFolder,
                onRenameSelected = viewModel::onRenameSelectedNode,
                onDeleteSelected = viewModel::onDeleteSelectedNode,
                onMoveSelectedUp = viewModel::onMoveSelectedUp,
                onMoveSelectedDown = viewModel::onMoveSelectedDown,
                modifier = Modifier.weight(0.24f).fillMaxHeight(),
            )

            EditorPane(
                text = uiState.editorText,
                canEdit = uiState.selectedDocumentId != null,
                onValueChanged = viewModel::onEditorChanged,
                modifier = Modifier.weight(0.56f).fillMaxHeight(),
            )

            NotesPane(
                notes = uiState.notesText,
                canEdit = uiState.selectedDocumentId != null,
                onValueChanged = viewModel::onNotesChanged,
                modifier = Modifier.weight(0.20f).fillMaxHeight(),
            )
        }
    }
}

private fun flattenBinderTree(nodes: List<DocumentEntity>): List<BinderTreeItem> {
    val byParent = nodes.groupBy { it.parentId }
    val result = mutableListOf<BinderTreeItem>()

    fun walk(parentId: String?, depth: Int) {
        byParent[parentId]
            .orEmpty()
            .sortedBy { it.orderIndex }
            .forEach { node ->
                result += BinderTreeItem(node = node, depth = depth)
                walk(node.id, depth + 1)
            }
    }

    walk(parentId = null, depth = 0)
    return result
}

@Composable
private fun TopControls(
    mode: WritingMode,
    durationMinutes: Int,
    remainingSeconds: Int,
    onModeChange: (WritingMode) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onTimerToggle: () -> Unit,
    allowedDurations: List<Int>,
    timerRunning: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text("Inkwell MVP Workspace", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WritingMode.entries.forEach { entry ->
                    AssistChip(
                        onClick = { onModeChange(entry) },
                        label = { Text(if (entry == mode) "• ${entry.name}" else entry.name) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Timer:")
                allowedDurations.forEach { min ->
                    TextButton(onClick = { onDurationSelected(min) }) {
                        Text(if (min == durationMinutes) "[$min]" else "$min")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onTimerToggle, label = { Text(if (timerRunning) "Pause" else "Start") })
                Text("Remaining ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}")
            }
        }
    }
}

@Composable
private fun OutlinePane(
    documents: List<DocumentEntity>,
    selectedNodeId: String?,
    onSelected: (DocumentEntity) -> Unit,
    onAddDocument: () -> Unit,
    onAddFolder: () -> Unit,
    onRenameSelected: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelectedUp: () -> Unit,
    onMoveSelectedDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameDraft by remember { mutableStateOf("") }
    val collapsedFolders = remember { mutableStateMapOf<String, Boolean>() }
    val treeItems = remember(documents, collapsedFolders.toMap()) {
        val hiddenIds = mutableSetOf<String>()
        val byParent = documents.groupBy { it.parentId }

        fun markHidden(folderId: String) {
            byParent[folderId].orEmpty().forEach { child ->
                hiddenIds += child.id
                if (child.type == BinderNodeType.Folder) {
                    markHidden(child.id)
                }
            }
        }

        collapsedFolders.filterValues { it }.keys.forEach(::markHidden)
        flattenBinderTree(documents).filterNot { it.node.id in hiddenIds }
    }

    Column(
        modifier = modifier.border(1.dp, Color.Gray).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Outline", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onAddFolder) { Text("+ Folder") }
            TextButton(onClick = onAddDocument) { Text("+ Doc") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onRenameSelected(renameDraft) }) { Text("Rename") }
            TextButton(onClick = onDeleteSelected) { Text("Delete") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onMoveSelectedUp) { Text("↑ Up") }
            TextButton(onClick = onMoveSelectedDown) { Text("↓ Down") }
        }

        OutlinedTextField(
            value = renameDraft,
            onValueChange = { renameDraft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("New title for selected") },
        )

        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(treeItems) { item ->
                val document = item.node
                val selected = document.id == selectedNodeId
                val isFolder = document.type == BinderNodeType.Folder
                val collapsed = collapsedFolders[document.id] == true
                val icon = if (isFolder) {
                    if (collapsed) "📁▸" else "📂▾"
                } else {
                    "📝"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) Color(0xFFE3F2FD) else Color.Transparent)
                        .clickable {
                            onSelected(document)
                            renameDraft = document.title
                            if (isFolder) {
                                collapsedFolders[document.id] = !(collapsedFolders[document.id] == true)
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Spacer(modifier = Modifier.width((item.depth * 14).dp))
                    Text(icon)
                    Text(document.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun EditorPane(
    text: String,
    canEdit: Boolean,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.border(1.dp, Color.Gray).padding(8.dp)) {
        Text("Editor", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onValueChanged,
            enabled = canEdit,
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text(if (canEdit) "Start drafting..." else "Select a document (not a folder)") },
        )
    }
}

@Composable
private fun NotesPane(
    notes: String,
    canEdit: Boolean,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.border(1.dp, Color.Gray).padding(8.dp)) {
        Text("Notes", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onValueChanged,
            enabled = canEdit,
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text(if (canEdit) "Document-linked plain notes..." else "Select a document") },
        )
    }
}

@Composable
private fun SprintView(
    editorText: String,
    onEditorChange: (String) -> Unit,
    remainingSeconds: Int,
) {
    Column(modifier = Modifier.fillMaxSize().border(1.dp, Color.Gray).padding(12.dp)) {
        Text("Sprint Mode", fontWeight = FontWeight.Bold)
        Text("Timer ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Current Paragraph Focus",
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editorText,
            onValueChange = onEditorChange,
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Write without distractions...") },
        )
    }
}
