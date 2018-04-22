package io.jitrapon.glom.base.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.google.android.instantapps.InstantApps
import io.jitrapon.glom.base.component.GooglePlaceProvider
import io.jitrapon.glom.base.component.PlaceProvider
import io.jitrapon.glom.base.model.*
import io.jitrapon.glom.base.util.AppLogger
import io.jitrapon.glom.base.util.showAlertDialog
import io.jitrapon.glom.base.util.showSnackbar
import io.jitrapon.glom.base.util.showToast

/**
 * Wrapper around Android's AppCompatActivity. Contains convenience functions
 * relating to fragment transactions, activity transitions, Android's new runtime permission handling,
 * analytics, and more. All activities should extend from this class.
 *
 * @author Jitrapon Tiachunpun
 */
abstract class BaseActivity : AppCompatActivity() {

    /* this activity 's swipe refresh layout, if provided */
    private var refreshLayout: SwipeRefreshLayout? = null

    /* subclass should overwrite this variable for naming the activity */
    var tag: String = "base"

    /* indicates whether or not this Activity instance is Instant App */
    val isInstantApp: Boolean by lazy {
        InstantApps.isInstantApp(this)
    }

    /* shared handler object */
    val handler: Handler by lazy {
        Handler()
    }

    /* shared google place provider */
    val placeProvider: PlaceProvider by lazy {
        GooglePlaceProvider(lifecycle, activity = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onCreateViewModel()
        onSubscribeToObservables()

        // if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments
        savedInstanceState ?: onCreateFragment(savedInstanceState)
    }

    /**
     * Common UI Action handlers for all child activities
     */
    private val viewActionHandler: Observer<UiActionModel> = Observer {
        it?.let {
            when (it) {
                is Toast -> showToast(it.message)
                is Snackbar -> showSnackbar(it.level, it.message, it.actionMessage, it.actionCallback)
                is Alert -> showAlertDialog(it.title, it.message, it.positiveOptionText, it.onPositiveOptionClicked,
                        it.negativeOptionText, it.onNegativeOptionClicked, it.isCancelable, it.onCancel)
                is EmptyLoading -> showEmptyLoading(it.show)
                is Navigation -> navigate(it.action, it.payload)
                is ReloadData -> onRefresh(it.delay)
                else -> {
                    AppLogger.w("This ViewAction is is not yet supported by this handler")
                }
            }
        }
    }

    /**
     * Called when one or more instances of fragments have to be created.
     * Only called if savedInstanceState is NULL
     */
    open fun onCreateFragment(savedInstanceState: Bundle?) {}

    /**
     * Called when one or more ViewModel instances should be created
     */
    open fun onCreateViewModel() {}

    /**
     * Subscribe to LiveData and LiveEvent from the ViewModel
     */
    open fun onSubscribeToObservables() {}

    /**
     * Should be called by child class to handle all view action observables automatically
     */
    protected fun subscribeToViewActionObservables(observableViewAction: LiveData<UiActionModel>) {
        observableViewAction.observe(this, viewActionHandler)
    }

    /**
     * Called when a RefreshLayout has been triggered manually by the user. This is a good time
     * to call any necessary ViewModel's function to (re)-load the data
     */
    open fun onRefresh(delayBeforeRefresh: Long) {}

    /**
     * Indicates that the view has no data and should be showing the main loading progress bar.
     * Override this function to make it behave differently
     */
    open fun showEmptyLoading(show: Boolean) {
        getEmptyLoadingView()?.let {
            it.visibility = if (show) View.VISIBLE else View.GONE

            // if triggered manually by swipe refresh layout, hide it. We don't need to show
            // two loading icons
            if (show) showLoading(false)
        }
        // if somehow the refreshlayout is still loading, set it to hide
        if (!show) showLoading(false)
    }

    /**
     * Returns a loading progress bar shown when the view is empty and is about to load a data
     */
    open fun getEmptyLoadingView(): ProgressBar? = null

    /**
     * Indicates that the view is loading some data. Override this function to make it behave differently
     */
    open fun showLoading(show: Boolean) {
        refreshLayout?.let {
            if (show) {
                if (!it.isRefreshing) it.isRefreshing = true
            }
            else {
                if (it.isRefreshing) it.isRefreshing = false
            }
        }
    }

    /**
     * Overrides this function to allow handling of navigation events
     */
    open fun navigate(action: String, payload: Any?) {}

    /**
     * Wrapper around Android's handler to delay run a Runnable on the main thread
     */
    fun delayRun(delay: Long, block: (Handler) -> Unit) {
        handler.postDelayed({
            block(handler)
        }, delay)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}