package io.jitrapon.glom.board

import com.google.android.gms.maps.model.LatLng

/**
 * All constants for this module that should not belong in Strings.xml
 * i.e. used for storing constants such as Intent ACTIONS, EXTRAS, REQUEST CODES, etc.
 *
 * @author Jitrapon Tiachunpun
 */
object Const {

    const val EXTRA_BOARD_ITEM = "android.intent.EXTRA_BOARD_ITEM"
    const val EXTRA_IS_BOARD_ITEM_MODIFIED = "android.intent.EXTRA_IS_BOARD_ITEM_MODIFIED"
    const val EXTRA_IS_BOARD_ITEM_NEW = "android.intent.EXTRA_IS_BOARD_ITEM_NEW"
    const val EDIT_ITEM_REQUEST_CODE = 1001
    const val NAVIGATE_TO_MAP_SEARCH = "action.navigate.map.search"
    const val NAVIGATE_TO_EVENT_PLAN = "action.navigate.event.plan"
    const val PLAN_EVENT_REQUEST_CODE = 1002
    const val NAVIGATE_BACK = "action.navigate.back"
    const val NAVIGATE_TO_PLACE_PICKER = "action.navigate.placepicker"
    const val PLACE_PICKER_RESULT_CODE = 1003
    const val NAVIGATE_TO_BOARD_PREFERENCE = "action.navigate.board.preference"
    const val EXTRA_BOARD_ITEM_TYPE = "android.intent.EXTRA_BOARD_ITEM_TYPE"
    const val DISMISS_BOARD_ITEM = "action.dismiss.board.item"
    const val BOARD_ITEM_PREFERENCE_REQUEST_CODE = 1004
}

data class NavigationArguments(val latLng: LatLng?,
                               val query: String?,
                               val placeId: String?,
                               val withDirection: Boolean)
