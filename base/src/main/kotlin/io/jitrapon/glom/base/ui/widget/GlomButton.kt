package io.jitrapon.glom.base.ui.widget

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import io.jitrapon.glom.base.model.ButtonUiModel
import io.jitrapon.glom.base.model.UiModel
import io.jitrapon.glom.base.util.getString

/**
 * Base button for using throughout the app. Allow additional operations such as
 * disabling upon clicked.
 *
 * Created by Jitrapon
 */
class GlomButton : AppCompatButton {

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    fun applyState(uiModel: ButtonUiModel) {
        isEnabled = uiModel.status == UiModel.Status.SUCCESS || uiModel.status == UiModel.Status.ERROR
        uiModel.text?.let {
            text = context.getString(it)
        }
    }
}