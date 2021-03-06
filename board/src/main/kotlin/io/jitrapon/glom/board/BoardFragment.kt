package io.jitrapon.glom.board

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.jitrapon.glom.base.model.AndroidString
import io.jitrapon.glom.base.model.UiModel
import io.jitrapon.glom.base.ui.BaseFragment
import io.jitrapon.glom.base.ui.widget.recyclerview.ItemTouchHelperCallback
import io.jitrapon.glom.base.ui.widget.stickyheader.StickyHeadersLinearLayoutManager
import io.jitrapon.glom.base.util.*
import io.jitrapon.glom.base.viewmodel.BaseViewModel
import io.jitrapon.glom.board.item.BoardItem
import io.jitrapon.glom.board.item.BoardItemAdapter
import io.jitrapon.glom.board.item.BoardItemPreferenceActivity
import io.jitrapon.glom.board.item.BoardItemUiModel
import io.jitrapon.glom.board.item.event.EventItem
import io.jitrapon.glom.board.item.event.EventItemActivity
import io.jitrapon.glom.board.item.event.plan.PlanEventActivity
import kotlinx.android.synthetic.main.board_fab_action_menu.*
import kotlinx.android.synthetic.main.board_fragment.*

/**
 * Fragment showing the board items in a group
 *
 * @author Jitrapon Tiachunpun
 */
class BoardFragment : BaseFragment() {

    /* this fragment's main ViewModel instance */
    private lateinit var viewModel: BoardViewModel

    private lateinit var newItemFab: FloatingActionButton
    private lateinit var customizeFab: FloatingActionButton
    private lateinit var newItemFabTextView: TextView
    private lateinit var customizeFabTextView: TextView

    companion object {

        fun newInstance(): BoardFragment = BoardFragment()

        /* first-time list reveal animation delay */
        private const val REVEAL_ANIM_DELAY = 300L
    }

    /**
     * Returns this fragment's XML layout
     */
    override fun getLayoutId() = R.layout.board_fragment

    /**
     * Returns the SwipeRefreshLayout in this fragment's XML layout
     */
    override fun getSwipeRefreshLayout(): androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = board_refresh_layout

    /**
     * Returns the progress bar for when this view is empty
     */
    override fun getEmptyLoadingView() = board_progressbar as ProgressBar

    /**
     * Returns the toolbar that this fragment will manage
     */
    override fun getToolbar(): Toolbar? = toolbar

    /**
     * Returns the toolbar's profile menu
     */
    override fun getProfileMenuIcon(): ImageView? = appbar_profile_menu

    /**
     * Create this fragment's ViewModel instance
     */
    override fun onCreateViewModel(activity: androidx.fragment.app.FragmentActivity) {
        viewModel = obtainViewModel(BoardViewModel::class.java)
    }

    /**
     * Called when any view must be initialized
     */
    override fun onSetupView(view: View) {
        board_recycler_view.apply {
            val recyclerView = this
            adapter = BoardItemAdapter(viewModel, this@BoardFragment, activity!!.resources.configuration.orientation).apply {

                // add swipe functionality
                val touchHelper = ItemTouchHelper(ItemTouchHelperCallback(this, false))
                touchHelper.attachToRecyclerView(recyclerView)
            }
            layoutManager = StickyHeadersLinearLayoutManager<BoardItemAdapter>(view.context)
            itemAnimator = DefaultItemAnimator()
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
//            createRecycledPool(recyclerView, 10)

            hide()
        }
        board_animation_view.hide()

        // main fab click listener
        board_fab.setOnClickListener {
            showFabActionMenu()
        }
    }

    /**
     * Called when this fragment is ready to subscribe to ViewModel's events
     */
    override fun onSubscribeToObservables() {
        subscribeToViewActionObservables(viewModel.getObservableViewAction())
        subscribeToAppBarObservable(viewModel.getObservableProfileMenuIcon())

        // observes board and its items
        viewModel.getObservableBoard().observe(viewLifecycleOwner, Observer {
            it?.let {
                when (it.status) {
                    UiModel.Status.EMPTY -> {
                        board_recycler_view.hide()
                        board_status_viewswitcher.apply {
                            displayedChild = 0
                        }
                    }
                    UiModel.Status.ERROR -> {
                        board_recycler_view.hide()
                        board_status_viewswitcher.apply {
                            displayedChild = 1
                        }
                    }
                    UiModel.Status.SUCCESS -> {
                        board_status_viewswitcher.reset()
                        board_recycler_view.show(REVEAL_ANIM_DELAY)

                        // loads additional place information for items that have Place IDs
                        it.requestPlaceInfoItemIds.let {
                            when {
                                it == null -> { /* do nothing */ }
                                it.isEmpty() -> viewModel.loadPlaceInfo(placeProvider, null)
                                else -> viewModel.loadPlaceInfo(placeProvider, it)
                            }
                        }

                        // loads additional place information for items that have addresses but no
                        // LatLng
                        it.requestAddressItemIds.let {
                            when {
                                it == null -> { /* do nothing */ }
                                it.isEmpty() -> viewModel.loadAddressInfo(placeProvider, null)
                                else -> viewModel.loadAddressInfo(placeProvider, it)
                            }
                        }

                        // if this list is not null, force update specific items
                        if (!it.itemsChangedIndices.isNullOrEmpty()) {
                            it.itemsChangedIndices?.forEach {
                                board_recycler_view.adapter!!.notifyItemChanged(it.first, it.second)
                            }
                        }
                        else {
                            // perform full recyclerview updates only when diff results is not available
                            it.diffResult.let {
                                if (it == null) {
                                    board_recycler_view.adapter!!.notifyDataSetChanged()
                                }
                                else {
                                    it.dispatchUpdatesTo(board_recycler_view.adapter!!)
                                }
                            }
                        }

                        // perform sync to a specific item
                        it.saveItem?.let {
                            viewModel.syncItem(it, true)
                        }
                    }
                    UiModel.Status.LOADING -> {
                        board_status_viewswitcher.reset()

                        // dispatch any pending updates to items if available
                        if (!it.itemsChangedIndices.isNullOrEmpty()) {
                            it.itemsChangedIndices?.forEach {
                                board_recycler_view.adapter!!.notifyItemChanged(it.first, it.second)
                            }
                        }
                        else {
                            //do nothing
                        }
                    }
                    else -> { /* not applicable */ }
                }
            }
        })

        // observes animation
        viewModel.getObservableAnimation().observe(viewLifecycleOwner, Observer {
            it?.let {
                board_animation_view.animate(it)
            }
        })

        // observes new selected board item
        viewModel.getObservableBoardItem().observe(viewLifecycleOwner, Observer {
            it?.let { arg ->
                val boardItem = arg.first
                val sharedElements = arg.second
                val isNewItem = arg.third
                val launchOption = when (boardItem) {
                    is EventItem -> EventItemActivity::class.java to arg.first
                    else -> null
                }
                launchOption?.let { (activity, boardItem) ->
                    startActivity(activity, Const.EDIT_ITEM_REQUEST_CODE, {
                        putExtra(Const.EXTRA_BOARD_ITEM, boardItem)
                        putExtra(Const.EXTRA_IS_BOARD_ITEM_NEW, isNewItem)
                    }, sharedElements)
                }
            }
        })

        // observers on navigation event
        viewModel.getObservableNavigation().observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.action == Const.NAVIGATE_TO_EVENT_PLAN) {
                    val (boardItem, isNewItem) = it.payload as Pair<*, *>

                    startActivity(PlanEventActivity::class.java, Const.PLAN_EVENT_REQUEST_CODE, {
                        putExtra(Const.EXTRA_BOARD_ITEM, boardItem as EventItem)
                        putExtra(Const.EXTRA_IS_BOARD_ITEM_NEW, isNewItem as Boolean)
                    }, animTransition = io.jitrapon.glom.R.anim.slide_up to 0)
                }
                else if (it.action == Const.NAVIGATE_TO_BOARD_PREFERENCE) {
                    val boardItemType = it.payload as Int

                    //TODO stop loading

                    startActivity(BoardItemPreferenceActivity::class.java, Const.BOARD_ITEM_PREFERENCE_REQUEST_CODE, {
                        putExtra(Const.EXTRA_BOARD_ITEM_TYPE, boardItemType)
                    })
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Const.EDIT_ITEM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    data?.getParcelableExtra<BoardItem>(Const.EXTRA_BOARD_ITEM)?.let {
                        if (data.getBooleanExtra(Const.EXTRA_IS_BOARD_ITEM_NEW, false)) {
                            viewModel.addNewItem(it)
                        }
                        else if (data.getBooleanExtra(Const.EXTRA_IS_BOARD_ITEM_MODIFIED, false)) {
                            viewModel.syncItem(it, false)
                        }
                    }
                }
                catch (ex: Exception) {
                    AppLogger.e(ex)
                }
            }
        }
        else if (requestCode == Const.PLAN_EVENT_REQUEST_CODE) {
            data?.getParcelableExtra<BoardItem?>(Const.EXTRA_BOARD_ITEM)?.let {
                viewModel.syncItem(it, false)
            }
        }
        else if (requestCode == Const.BOARD_ITEM_PREFERENCE_REQUEST_CODE) {
            showToast(AndroidString(text = "TODO"))
        }
    }

    /**
     * Callback for when the board has been manually refreshed
     */
    override fun onRefresh(delayBeforeRefresh: Long, refresh: Boolean) {
        val loadType = if (refresh) BaseViewModel.LoadType.REMOTE else BaseViewModel.LoadType.LOCAL_AND_INVALIDATE
        if (delayBeforeRefresh > 0L) {
            delayRun(delayBeforeRefresh) {
                viewModel.loadBoard(loadType)
            }
        }
        else {
            viewModel.loadBoard(loadType)
        }
    }

    /**
     * Experiment: optimize recyclerview by creating viewholders beforehand for better
     * scrolling performance
     */
    private fun createRecycledPool(recyclerView: RecyclerView, poolCount: Int) {
        recyclerView.apply {
            setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
                setMaxRecycledViews(BoardItemUiModel.TYPE_EVENT, poolCount)
                for (i in 0..poolCount) {
                    putRecycledView(adapter!!.createViewHolder(board_recycler_view, BoardItemUiModel.TYPE_EVENT))
                }
            })
        }
    }

    private fun showFabActionMenu() {
        board_fab_menu_stub.let { stub ->
            // already inflated
            if (stub == null) {
                if (board_fab_menu.isVisible) {
                    collapseFabActionMenu(board_fab_menu)
                }
                else {
                    expandFabActionMenu(board_fab_menu)
                }
            }
            else {
                stub.setOnInflateListener { _, inflated ->
                    inflated as ViewGroup
                    customizeFab = inflated.findViewById(R.id.board_fab_customize)
                    customizeFabTextView = inflated.findViewById(R.id.board_fab_customize_desc)
                    customizeFab.apply {
                        setOnClickListener {
                            viewModel.showBoardPreference()
                            collapseFabActionMenu(inflated)
                        }
                        hide()
                    }
                    newItemFab = inflated.findViewById(R.id.board_fab_new_item)
                    newItemFabTextView = inflated.findViewById(R.id.board_fab_new_item_desc)
                    newItemFab.apply {
                        setOnClickListener {
                            viewModel.showEmptyNewItem()
                            collapseFabActionMenu(inflated)
                        }
                        hide()
                    }
                    expandFabActionMenu(inflated)
                }
                stub.inflate()
            }
        }
    }

    private fun expandFabActionMenu(viewGroup: ViewGroup) {
        viewGroup.show()

        delayRun(100L) {
            customizeFab.show()
            customizeFabTextView.show()
        }
        delayRun(200L) {
            newItemFab.show()
            newItemFabTextView.show()
        }
    }

    private fun collapseFabActionMenu(viewGroup: ViewGroup) {
        delayRun(100L) {
            newItemFab.hide()
            newItemFabTextView.hide(50L, true)
        }
        delayRun(200L) {
            customizeFab.hide()
            customizeFabTextView.hide(50L, true)
        }
        delayRun(300L) { viewGroup.hide() }
    }

    /**
     * Sign in state has changed from other view
     */
    override fun onSignOut() {
        onRefresh(100L, true)
    }
}
