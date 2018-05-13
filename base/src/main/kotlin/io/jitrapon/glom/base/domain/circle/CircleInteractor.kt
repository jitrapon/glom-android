package io.jitrapon.glom.base.domain.circle

import io.jitrapon.glom.base.model.AsyncErrorResult
import io.jitrapon.glom.base.model.AsyncResult
import io.jitrapon.glom.base.model.AsyncSuccessResult
import io.jitrapon.glom.base.util.AppLogger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Interactor dealing with Circle business logic. This class also manages
 * currently active circle state
 *
 * @author Jitrapon Tiachunpun
 */
class CircleInteractor(private val circleDataSource: CircleDataSource) {

    private var activeCircleId: String = "abcd1234"

    /**
     * Loads a circle from a data source, with an optional specified list of fields
     */
    fun loadCircle(refresh: Boolean, vararg fields: String, onComplete: (AsyncResult<Circle>) -> Unit) {
        getCurrentCircle().let {
            if (it == null) {
                onComplete(AsyncErrorResult(Exception("Active circle ID is NULL")))
            }
            else {
                circleDataSource.getCircle(refresh, it.circleId, *fields)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            onComplete(AsyncSuccessResult(it))
                        }, {
                            onComplete(AsyncErrorResult(it))
                        })
            }
        }
    }

    /**
     * Returns a nullable currently active Circle
     */
    fun getCurrentCircle(): Circle? {
        try {
            return circleDataSource.getCircle(false, activeCircleId).blockingFirst()
        }
        catch (ex: Exception) {
            AppLogger.e(ex)
        }
        return null
    }
}
