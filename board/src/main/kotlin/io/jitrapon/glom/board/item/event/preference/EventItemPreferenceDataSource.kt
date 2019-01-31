package io.jitrapon.glom.board.item.event.preference

import io.reactivex.Flowable
import java.util.*

/**
 * Main entry to the saved preference settings for event items
 *
 * Created by Jitrapon
 */
interface EventItemPreferenceDataSource {

    fun getPreference(refresh: Boolean): Flowable<EventItemPreference>

    fun getSyncTime(): Date
}