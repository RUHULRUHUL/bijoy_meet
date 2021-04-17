package video.meetting.app.viewmodel

import video.meetting.app.model.Meeting
import video.meetting.app.repository.MainRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MainRepository) : ViewModel() {

    fun addMeetingToDb(meeting: Meeting) {
        viewModelScope.launch {
            repository.addMeetingToDb(meeting)
        }
    }

}