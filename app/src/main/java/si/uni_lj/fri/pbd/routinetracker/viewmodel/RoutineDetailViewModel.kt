package si.uni_lj.fri.pbd.routinetracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import si.uni_lj.fri.pbd.routinetracker.data.RoutineContext
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.data.entity.RoutineExecution
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository

class RoutineDetailViewModel(private val repository: RoutineRepository) : ViewModel() {


    private val _routineContext = MutableLiveData<RoutineContext?>()
    val routineContext: LiveData<RoutineContext?> get() = _routineContext

    fun getRoutineById(id: Long): LiveData<Routine> {
        return repository.getRoutineById(id).asLiveData()
    }

    fun getExecutionsForRoutine(routineId: Long): LiveData<List<RoutineExecution>> {
        return repository.getExecutionsForRoutine(routineId).asLiveData()
    }

    fun insertRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.insertRoutine(routine)
        }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }



    fun fetchRoutineContext(routineId: Long) {
        viewModelScope.launch {
            try {

                val contextResult = repository.buildRoutineContext(routineId)

                _routineContext.postValue(contextResult)
            } catch (e: Exception) {
                e.printStackTrace()
                _routineContext.postValue(null)
            }
        }
    }
}