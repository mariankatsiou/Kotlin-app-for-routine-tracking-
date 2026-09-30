package si.uni_lj.fri.pbd.routinetracker

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineDetailViewModel
import si.uni_lj.fri.pbd.routinetracker.viewmodel.RoutineViewModelFactory
import java.util.*

class AddEditRoutineFragment : Fragment() {

    private lateinit var viewModel: RoutineDetailViewModel
    private lateinit var repository: RoutineRepository
    private var routineId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        routineId = arguments?.getLong("routineId") ?: -1L

        val dao = RoutinesDatabase.getDatabase(requireContext().applicationContext).routineDao()
        repository = RoutineRepository(requireContext().applicationContext, dao)
        val factory = RoutineViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RoutineDetailViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RoutineExpressiveTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // State variables for form
                        var name by remember { mutableStateOf("") }
                        var type by remember { mutableStateOf("Study") }
                        var startTime by remember { mutableStateOf("09:00") }
                        var endTime by remember { mutableStateOf("10:00") }
                        var selectedDays by remember { mutableStateOf(setOf<String>()) }
                        var notificationsEnabled by remember { mutableStateOf(false) }
                        var isEditMode by remember { mutableStateOf(false) }

                        // Load data if in Edit Mode
                        DisposableEffect(routineId) {
                            if (routineId != -1L) {
                                isEditMode = true
                                val observer = Observer<Routine> { routine ->
                                    routine?.let {
                                        name = it.name
                                        type = it.type
                                        startTime = it.startTime
                                        endTime = it.endTime
                                        selectedDays = it.days.split(", ").map { d -> d.trim() }.filter { d -> d.isNotEmpty() }.toSet()
                                        notificationsEnabled = it.notificationsEnabled
                                    }
                                }
                                val liveData = viewModel.getRoutineById(routineId)
                                liveData.observe(viewLifecycleOwner, observer)
                                onDispose { liveData.removeObserver(observer) }
                            } else {
                                onDispose { }
                            }
                        }

                        AddEditRoutineScreen(
                            isEditMode = isEditMode,
                            name = name, onNameChange = { name = it },
                            type = type, onTypeChange = { type = it },
                            startTime = startTime, onStartTimeChange = { startTime = it },
                            endTime = endTime, onEndTimeChange = { endTime = it },
                            selectedDays = selectedDays, onDaysChange = { selectedDays = it },
                            notificationsEnabled = notificationsEnabled, onNotificationsChange = { notificationsEnabled = it },
                            onSaveClick = {
                                saveRoutine(name, type, startTime, endTime, selectedDays, notificationsEnabled)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun saveRoutine(
        name: String, type: String, startTime: String, endTime: String,
        selectedDays: Set<String>, notificationsEnabled: Boolean
    ) {
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDays.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endMins = endParts[0].toInt() * 60 + endParts[1].toInt()

        if (endMins <= startMins) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        val daysString = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            .filter { it in selectedDays }
            .joinToString(", ")

        viewLifecycleOwner.lifecycleScope.launch {
            var finalId = routineId
            val routineObj = Routine(
                id = if (routineId == -1L) 0 else routineId,
                name = name.trim(),
                type = type,
                startTime = startTime,
                endTime = endTime,
                days = daysString,
                notificationsEnabled = notificationsEnabled
            )

            if (routineId == -1L) {
                finalId = repository.insertRoutine(routineObj)
            } else {
                viewModel.updateRoutine(routineObj)
            }

            val alarmScheduler = AlarmScheduler(requireContext())
            alarmScheduler.cancelAlarm(finalId)

            if (notificationsEnabled) {
                val scheduledRoutine = routineObj.copy(id = finalId)
                alarmScheduler.scheduleAlarm(scheduledRoutine)
            }

            Toast.makeText(requireContext(), "Routine saved successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
}

// ==========================================
// COMPOSE UI FOR ADD/EDIT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoutineScreen(
    isEditMode: Boolean,
    name: String, onNameChange: (String) -> Unit,
    type: String, onTypeChange: (String) -> Unit,
    startTime: String, onStartTimeChange: (String) -> Unit,
    endTime: String, onEndTimeChange: (String) -> Unit,
    selectedDays: Set<String>, onDaysChange: (Set<String>) -> Unit,
    notificationsEnabled: Boolean, onNotificationsChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            ExpressiveSaveButton(
                text = if (isEditMode) "Update Routine" else "Save Routine",
                onClick = onSaveClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isEditMode) "EDIT FLOW" else "NEW FLOW",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 1. Name Input
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Routine Name", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.titleMedium,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Category Selection (Study, Exercise, Socialise)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("Study", "Exercise", "Socialise")
                    categories.forEach { cat ->
                        ExpressiveChip(
                            text = cat,
                            isSelected = type == cat,
                            onClick = { onTypeChange(cat) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Time Selection
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimePickerCard(
                    label = "Start Time",
                    time = startTime,
                    onClick = {
                        showTimePickerDialog(context, startTime) { onStartTimeChange(it) }
                    },
                    modifier = Modifier.weight(1f)
                )
                TimePickerCard(
                    label = "End Time",
                    time = endTime,
                    onClick = {
                        showTimePickerDialog(context, endTime) { onEndTimeChange(it) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Days Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Days of Week", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    allDays.forEach { day ->
                        DayCircle(
                            day = day.take(1),
                            isSelected = selectedDays.contains(day),
                            onClick = {
                                val newDays = selectedDays.toMutableSet()
                                if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
                                onDaysChange(newDays)
                            }
                        )
                    }
                }
            }

            // 5. Notifications
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Enable Notifications", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// HELPER COMPONENTS
// ==========================================

fun showTimePickerDialog(context: android.content.Context, currentTime: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    TimePickerDialog(context, { _, h, m ->
        onTimeSelected(String.format("%02d:%02d", h, m))
    }, hour, minute, true).show()
}

@Composable
fun ExpressiveChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DayCircle(day: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(day, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimePickerCard(label: String, time: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(time, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ExpressiveSaveButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))

    Box(modifier = Modifier.fillMaxWidth().padding(24.dp).scale(scale)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.titleLarge)
        }
    }
}



@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, showSystemUi = true)
@Composable
fun PreviewAddEditRoutineScreen() {
    var mockName by remember { mutableStateOf("Morning Workout") }
    var mockType by remember { mutableStateOf("Exercise") }
    var mockStart by remember { mutableStateOf("07:30") }
    var mockEnd by remember { mutableStateOf("08:30") }
    var mockDays by remember { mutableStateOf(setOf("Mon", "Wed", "Fri")) }
    var mockNotifs by remember { mutableStateOf(true) }

    RoutineExpressiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AddEditRoutineScreen(
                isEditMode = false,
                name = mockName,
                onNameChange = { mockName = it },
                type = mockType,
                onTypeChange = { mockType = it },
                startTime = mockStart,
                onStartTimeChange = { mockStart = it },
                endTime = mockEnd,
                onEndTimeChange = { mockEnd = it },
                selectedDays = mockDays,
                onDaysChange = { mockDays = it },
                notificationsEnabled = mockNotifs,
                onNotificationsChange = { mockNotifs = it },
                onSaveClick = { }
            )
        }
    }
}