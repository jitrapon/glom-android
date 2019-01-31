package io.jitrapon.glom.board.item.event.preference

import androidx.lifecycle.MutableLiveData
import io.jitrapon.glom.base.model.AsyncErrorResult
import io.jitrapon.glom.base.model.AsyncSuccessResult
import io.jitrapon.glom.base.model.UiModel
import io.jitrapon.glom.base.viewmodel.BaseViewModel
import io.jitrapon.glom.board.BoardInjector
import io.jitrapon.glom.board.BoardViewModel
import javax.inject.Inject

/**
 * ViewModel class responsible for showing and interacting with the event item preference
 *
 * Created by Jitrapon
 */
class EventItemPreferenceViewModel : BaseViewModel() {

    @Inject
    lateinit var interactor: EventItemPreferenceInteractor

    private val observablePreferenceUiModel = MutableLiveData<EventItemPreferenceUiModel>()
    private val preference = EventItemPreferenceUiModel()

    init {
        BoardInjector.getComponent().inject(this)

        loadPreference(false)
    }

    fun loadPreference(refresh: Boolean) {
        observablePreferenceUiModel.value = preference.apply {
            status = UiModel.Status.LOADING
        }

        loadData(refresh, interactor::loadPreference, if (!firstLoadCalled) BoardViewModel.FIRST_LOAD_ANIM_DELAY
        else BoardViewModel.SUBSEQUENT_LOAD_ANIM_DELAY) {
            when (it) {
                is AsyncSuccessResult -> updatePreference(it.result.second)
                is AsyncErrorResult -> updatePreference(it.error)
            }
        }
        firstLoadCalled = true
    }

    private fun updatePreference(preference: EventItemPreference) {

    }

    private fun updatePreference(error: Throwable) {
        handleError(error)

        observablePreferenceUiModel.value = preference.apply {
            status = UiModel.Status.ERROR
        }
    }
}