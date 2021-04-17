package video.meetting.app.di

import video.meetting.app.repository.MainRepository
import video.meetting.app.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {

    single { MainRepository(get()) }
    viewModel { MainViewModel(get()) }

}
