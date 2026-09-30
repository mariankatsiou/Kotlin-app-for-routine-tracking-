package si.uni_lj.fri.pbd.routinetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.data.dao.RoutineWithStatus
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.util.RoutineEvaluationWorker
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineDetailViewModel
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineListViewModel
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineViewModelFactory

class RoutineListFragment : Fragment() {

    private lateinit var listViewModel: RoutineListViewModel
    private lateinit var detailViewModel: RoutineDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize Room database DAO and Repository to inject dependencies into the ViewModels
        val dao = RoutinesDatabase.getDatabase(requireContext().applicationContext).routineDao()
        val repository = RoutineRepository(requireContext().applicationContext, dao)
        val factory = RoutineViewModelFactory(repository)
// Use a ViewModelFactory to instantiate ViewModels that require constructor parameters
        listViewModel = ViewModelProvider(this, factory)[RoutineListViewModel::class.java]
        detailViewModel = ViewModelProvider(this, factory)[RoutineDetailViewModel::class.java]
// Bridge traditional Android Fragments with Jetpack Compose using ComposeView
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RoutineExpressiveTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        var routines by remember { mutableStateOf<List<RoutineWithStatus>>(emptyList()) }
// Safely observe LiveData within Compose. DisposableEffect ensures the observer is properly cleaned up when the Composable leaves the screen.
                        DisposableEffect(listViewModel) {
                            val observer = Observer<List<RoutineWithStatus>> { routines = it }
                            listViewModel.allRoutines.observe(viewLifecycleOwner, observer)
                            onDispose { listViewModel.allRoutines.removeObserver(observer) }
                        }

                        RoutineListScreen(
                            routines = routines,
                            onAddClick = { findNavController().navigate(R.id.addEditRoutineFragment) },
                            onCheckExecutionClick = { runExecutionCheck() },
                            onRoutineClick = { routine ->
                                val bundle = Bundle().apply { putLong("routineId", routine.id) }
                                findNavController().navigate(R.id.routineDetailsFragment, bundle)
                            },
                            onRoutineLongClick = { routine -> showDeleteDialog(routine) }
                        )
                    }
                }
            }
        }
    }

    private fun runExecutionCheck() {
        // Enqueue a OneTimeWorkRequest via WorkManager to trigger an immediate background sync
        val immediateRequest = OneTimeWorkRequestBuilder<RoutineEvaluationWorker>().build()
        WorkManager.getInstance(requireContext()).enqueue(immediateRequest)
        Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteDialog(routine: Routine) {
        // Cancel any pending alarms associated with this routine before removing it from the database
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Routine")
            .setMessage("Are you sure you want to permanently delete this routine?")
            .setPositiveButton("Yes") { _, _ ->
                val alarmScheduler = AlarmScheduler(requireContext())
                alarmScheduler.cancelAlarm(routine.id)
                detailViewModel.deleteRoutine(routine)
            }
            .setNegativeButton("No", null)
            .show()
    }
}

// ==========================================
// MAIN SCREEN (Grid Only - Toolbar is Native now)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineListScreen(
    routines: List<RoutineWithStatus>,
    onAddClick: () -> Unit,
    onCheckExecutionClick: () -> Unit,
    onRoutineClick: (Routine) -> Unit,
    onRoutineLongClick: (Routine) -> Unit
) {
    var isScreenLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isScreenLoaded = true
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = isScreenLoaded,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = onCheckExecutionClick,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Routines")
                    }

                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Routine")
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            AnimatedVisibility(
                visible = isScreenLoaded,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(tween(500))
            ) {
                Text(
                    text = "Your daily flow.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
                )
            }

            if (routines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Ready to start your flow?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                        bottom = 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = routines, key = { it.routine.id }) { item ->
                        ExpressiveGridCard(
                            item = item,
                            onClick = { onRoutineClick(item.routine) },
                            onLongClick = { onRoutineLongClick(item.routine) },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// EXPRESSIVE CARD
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveGridCard(
    item: RoutineWithStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "SpatialSpring"
    )

    val (targetBg, targetContent, icon) = when (item.isCompleted) {
        true -> Triple(Color(0xFFE8F5E9).copy(alpha = 0.7f), Color(0xFF1B5E20), Icons.Default.CheckCircle)
        false -> Triple(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), MaterialTheme.colorScheme.error, Icons.Default.Clear)
        null -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.primary, Icons.Default.Info)
    }

    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = spring(stiffness = Spring.StiffnessVeryLow), label = "EffectsSpring")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .scale(scale)
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onClick, onLongClick = onLongClick)
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = targetContent.copy(alpha = 0.8f))
            Column {
                Text(text = item.routine.name, style = MaterialTheme.typography.titleMedium, color = targetContent, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = item.routine.startTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = targetContent.copy(alpha = 0.7f))
            }
        }
    }
}

// ==========================================
// PREVIEW
// ==========================================
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, showSystemUi = true)
@Composable
fun PreviewRoutineGrid() {
    val dummyData = listOf(
        RoutineWithStatus(Routine(1L, "Morning Gym", "Exercise", "07:00", "08:30", "Mon", true), true),
        RoutineWithStatus(Routine(2L, "Study Kotlin", "Study", "18:00", "20:00", "Daily", true), false),
        RoutineWithStatus(Routine(3L, "Meditation", "Relax", "21:00", "21:30", "Daily", false), null)
    )

    RoutineExpressiveTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            RoutineListScreen(routines = dummyData, {}, {}, {}, {})
        }
    }
}