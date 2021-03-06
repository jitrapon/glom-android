package io.jitrapon.glom.board

import dagger.Component
import io.jitrapon.glom.base.di.BaseComponent
import io.jitrapon.glom.board.item.event.EventItemViewModel
import io.jitrapon.glom.board.item.event.widget.placepicker.PlacePickerViewModel
import io.jitrapon.glom.board.item.event.plan.PlanEventViewModel
import io.jitrapon.glom.board.item.event.preference.EventItemPreferenceViewModel

@BoardScope
@Component(dependencies = [BaseComponent::class], modules = [BoardModule::class])
interface BoardComponent {

    fun inject(viewModel: BoardViewModel)
    fun inject(viewModel: EventItemViewModel)
    fun inject(viewModel: PlanEventViewModel)
    fun inject(viewModel: EventItemPreferenceViewModel)
    fun inject(viewModel: PlacePickerViewModel)
}
