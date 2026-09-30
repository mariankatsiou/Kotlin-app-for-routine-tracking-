package si.uni_lj.fri.pbd.routinetracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import si.uni_lj.fri.pbd.routinetracker.databinding.ItemRoutineBinding
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
/**
 * Adapter for the RecyclerView in RoutineListFragment.
 * Manages the display of routine items and their current status indicators
 */
class RoutineAdapter(
    private var routines: List<Routine>,
    private val onItemClick: (Routine) -> Unit,
    private val onItemLongClick: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    class RoutineViewHolder(val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val routine = routines[position]
        holder.binding.tvRoutineName.text = routine.name
        holder.binding.tvRoutineType.text = routine.type
        holder.binding.tvRoutineTime.text = "${routine.startTime} - ${routine.endTime}"

        holder.itemView.setOnClickListener { onItemClick(routine) }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(routine)
            true
        }

        val calendar = java.util.Calendar.getInstance()

// Logic to determine the routine's real-time status color
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)
        val todayName = dayFormat.format(calendar.time) // π.χ. "Mon"


        val nowInMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        val startParts = routine.startTime.split(":")
        val startInMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endParts = routine.endTime.split(":")
        val endInMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

        /**
         * 3. Status Indicator Logic:
         * - Grey (#BDBDBD): Routine is not scheduled for today or hasn't started yet.
         * - Green (#4CAF50): Routine is currently active.
         * - Red (#F44336): Routine interval has passed for today.
         */
        val statusColor = when {

            !routine.days.contains(todayName) -> "#BDBDBD"


            nowInMinutes < startInMinutes -> "#BDBDBD"


            nowInMinutes in startInMinutes..endInMinutes -> "#4CAF50"


            else -> "#F44336"
        }

        holder.binding.statusIndicator.setBackgroundColor(android.graphics.Color.parseColor(statusColor))


        holder.binding.statusIndicator.setBackgroundColor(android.graphics.Color.parseColor(statusColor))
    }

    override fun getItemCount() = routines.size
    /**
     * Updates the adapter's data set and refreshes the UI.
     */
    fun updateData(newRoutines: List<Routine>) {
        routines = newRoutines
        notifyDataSetChanged()
    }
}