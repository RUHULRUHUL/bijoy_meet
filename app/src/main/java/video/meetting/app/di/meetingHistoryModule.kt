package video.meetting.app.di

import video.meetting.app.repository.MeetingHistoryRepository
import video.meetting.app.viewmodel.MeetingHistoryViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val meetingHistoryModule = module {

    single { MeetingHistoryRepository(get()) }
    viewModel { MeetingHistoryViewModel(get()) }

}
