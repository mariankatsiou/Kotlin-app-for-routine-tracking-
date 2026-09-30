package si.uni_lj.fri.pbd.routinetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineDetailViewModel
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiExecution(val id: String, val date: String, val isCompleted: Boolean)

class RoutineDetailsFragment : Fragment() {

    private lateinit var viewModel: RoutineDetailViewModel
    private var routineId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        routineId = arguments?.getLong("routineId") ?: -1L

        val dao = RoutinesDatabase.getDatabase(requireContext().applicationContext).routineDao()
        val repository = RoutineRepository(requireContext().applicationContext, dao)
        val factory = RoutineViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RoutineDetailViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RoutineExpressiveTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                        var routine by remember { mutableStateOf<Routine?>(null) }
                        var executions by remember { mutableStateOf<List<UiExecution>>(emptyList()) }
                        var suggestion by remember { mutableStateOf<String?>(null) }
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        DisposableEffect(routineId) {
                            // Observe basic routine details from the local database
                            val observer = Observer<Routine> { routine = it }
                            viewModel.getRoutineById(routineId).observe(viewLifecycleOwner, observer)
                            onDispose { viewModel.getRoutineById(routineId).removeObserver(observer) }
                        }
// Observe the execution history list and map the entities to UI-friendly objects (UiExecution)
                        DisposableEffect(routineId) {
                            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val observer = Observer<List<Any>> { list ->
                                executions = list.mapIndexed { index, exec ->
                                    val dateTimestamp = exec.javaClass.getMethod("getDateTimestamp").invoke(exec) as Long
                                    val completed = exec.javaClass.getMethod("getCompleted").invoke(exec) as Boolean
                                    UiExecution(index.toString(), format.format(Date(dateTimestamp)), completed)
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            val liveData = viewModel.getExecutionsForRoutine(routineId) as androidx.lifecycle.LiveData<List<Any>>
                            liveData.observe(viewLifecycleOwner, observer)
                            onDispose { liveData.removeObserver(observer) }
                        }

                        DisposableEffect(routineId) {
                            val observer = Observer<Any?> { context ->
                                context?.let {
                                    suggestion = it.javaClass.getMethod("getSuggestion").invoke(it) as String
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            val liveData = viewModel.routineContext as androidx.lifecycle.LiveData<Any?>
                            liveData.observe(viewLifecycleOwner, observer)
                            onDispose { liveData.removeObserver(observer) }
                        }
// Trigger a fresh API call and sensor read when the screen is loaded, leveraging Coroutines
                        LaunchedEffect(routineId) {
                            viewModel.fetchRoutineContext(routineId)
                        }

                        if (showDeleteDialog) {
                            ExpressiveDeleteDialog(
                                onConfirm = {
                                    // Ensure associated alarms/notifications are canceled before deleting the routine
                                    val alarmScheduler = AlarmScheduler(requireContext())
                                    alarmScheduler.cancelAlarm(routineId)
                                    routine?.let { viewModel.deleteRoutine(it) }
                                    showDeleteDialog = false
                                    findNavController().navigateUp()
                                },
                                onDismiss = { showDeleteDialog = false }
                            )
                        }

                        routine?.let { currentRoutine ->
                            RoutineDetailsScreen(
                                routine = currentRoutine,
                                suggestion = suggestion,
                                history = executions,
                                onBackClick = { findNavController().navigateUp() },
                                onEditClick = {
                                    val bundle = Bundle().apply { putLong("routineId", routineId) }
                                    findNavController().navigate(R.id.addEditRoutineFragment, bundle)
                                },
                                onDeleteClick = { showDeleteDialog = true }
                            )
                        } ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RoutineDetailsScreen(
    routine: Routine,
    suggestion: String?,
    history: List<UiExecution>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Scaffold(

        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = onDeleteClick,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
                FloatingActionButton(
                    onClick = onEditClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = "FLOW DETAILS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ExpressiveChipStatic(text = routine.type)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard(
                        title = "Schedule",
                        value = "${routine.startTime} - ${routine.endTime}",
                        icon = Icons.Default.Info,
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        title = "Alerts",
                        value = if (routine.notificationsEnabled) "Active" else "Off",
                        icon = Icons.Default.Notifications,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoCard(
                    title = "Active Days",
                    value = routine.days,
                    icon = Icons.Default.Menu,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!suggestion.isNullOrEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp)) // Αντί για Lightbulb
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Contextual Suggestion", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(suggestion, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (history.isEmpty()) {
                    Text("No execution history yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(history, key = { it.id }) { exec ->
                HistoryRow(exec)
            }
        }
    }
}



@Composable
fun InfoCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun HistoryRow(execution: UiExecution) {
    val (bgColor, icon, tint) = if (execution.isCompleted) {
        Triple(Color(0xFFE8F5E9).copy(alpha = 0.6f), Icons.Default.CheckCircle, Color(0xFF1B5E20))
    } else {
        Triple(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), Icons.Default.Clear, MaterialTheme.colorScheme.error)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(execution.date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tint)
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun ExpressiveChipStatic(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ExpressiveDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Routine", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Are you sure you want to permanently delete this routine? This action cannot be undone.", style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}


@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, showSystemUi = true)
@Composable
fun PreviewRoutineDetails() {
    val mockRoutine = Routine(1L, "Morning Gym", "Exercise", "07:00", "08:30", "Mon, Wed, Fri", true)
    val mockHistory = listOf(
        UiExecution("1", "12/05/2026 08:30", true),
        UiExecution("2", "10/05/2026 08:30", false)
    )

    RoutineExpressiveTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            RoutineDetailsScreen(
                routine = mockRoutine,
                suggestion = "It's sunny outside! Consider moving your exercise to the park.",
                history = mockHistory,
                onBackClick = {}, onEditClick = {}, onDeleteClick = {}
            )
        }
    }
}