package io.jitrapon.glom.board

import dagger.Component
import io.jitrapon.glom.base.di.BaseComponent
import io.jitrapon.glom.board.event.EventItemViewModel

@BoardScope
@Component(dependencies = [BaseComponent::class], modules = [BoardModule::class])
interface BoardComponent {

    fun inject(viewModel: BoardViewModel)
    fun inject(viewModel: EventItemViewModel)
}