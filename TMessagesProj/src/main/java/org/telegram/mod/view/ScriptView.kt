package org.telegram.mod.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.messenger.R
import org.telegram.mod.data.ScriptItem
import org.telegram.ui.ActionBar.Theme.ResourcesProvider
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Components.RecyclerListView

class ScriptView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    resourcesProvider: ResourcesProvider? = null,
) : LinearLayout(context, attrs, defStyleAttr){
    val header = HeaderCell(context,resourcesProvider)
    val content = RecyclerListView(context,resourcesProvider)

    init {
        orientation = VERTICAL
        addView(header)
        addView(content)
    }
}