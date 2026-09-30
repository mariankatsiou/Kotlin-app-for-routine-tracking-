package si.uni_lj.fri.pbd.routinetracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import si.uni_lj.fri.pbd.routinetracker.data.dao.RoutineWithStatus
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository

class RoutineListViewModel(private val repository: RoutineRepository) : ViewModel() {


    val allRoutines: LiveData<List<RoutineWithStatus>> = repository.routinesWithStatus.asLiveData()

}