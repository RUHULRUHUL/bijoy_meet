package video.meetting.app.repository

import video.meetting.app.db.MeetingDao
import video.meetting.app.model.Meeting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository(private val meetingDao: MeetingDao) {

    suspend fun addMeetingToDb(meeting: Meeting) = withContext(Dispatchers.IO) {
        meetingDao.insertMeeting(meeting)
    }

}