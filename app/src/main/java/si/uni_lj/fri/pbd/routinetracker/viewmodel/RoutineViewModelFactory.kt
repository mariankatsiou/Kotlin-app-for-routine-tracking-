package si.uni_lj.fri.pbd.routinetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository

class RoutineViewModelFactory(private val repository: RoutineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(RoutineDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}