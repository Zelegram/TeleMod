package org.telegram.mod.script

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.mod.data.ScriptFile
import org.telegram.ui.Cells.NotificationsCheckCell
import org.telegram.ui.Components.RecyclerListView
import kotlin.math.abs

class LocalScriptAdapter(
    private val items: MutableList<ScriptFile>
) : RecyclerListView.SelectionAdapter() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RecyclerListView.Holder(NotificationsCheckCell(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val value = listOfNotNull(
            item.header?.version,
            item.header?.author,
            item.header?.runAt?.value
        ).joinToString(" | ")
        val checkCell = (holder.itemView as NotificationsCheckCell)
        checkCell.tag = item.content
        checkCell.setTextAndValueAndCheck(
            item.header?.name, value, !ScriptFileManager.isDisabled(item.file), 0,false, false
        )
    }

    fun isToggleSwitchArea(view: View, x: Float): Boolean {
        val toggleArea = AndroidUtilities.dp(76f)
        return (LocaleController.isRTL && x <= toggleArea) || (!LocaleController.isRTL && x >= view.measuredWidth - toggleArea)
    }

    fun removeItemAt(position: Int) {
        items[position].file.delete()
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int = items.size
    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = true
}

class LocalScriptItemTouchCallback(
    context: Context,
    private val callback: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.menu_delete_old)!!
    private val background = ColorDrawable(Color.parseColor("#f44336"))
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false // Move not handled

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        viewHolder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(callback)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
        val isSwipeRight = dX > 0
        val drawThreshold = deleteIcon.intrinsicWidth * 2

        if (dX == 0f && !isCurrentlyActive) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw background for delete action
        val backgroundLeft = if (isSwipeRight) itemView.left else itemView.right + dX.toInt()
        val backgroundRight = if (isSwipeRight) itemView.left + dX.toInt() else itemView.right
        background.setBounds(backgroundLeft, itemView.top, backgroundRight, itemView.bottom)
        background.draw(c)

        // Only draw the delete icon when the swipe distance surpasses the threshold
        if (abs(dX) > drawThreshold) {
            val iconTop = itemView.top + iconMargin
            val iconLeft = if (isSwipeRight) itemView.left + iconMargin else itemView.right - iconMargin - deleteIcon.intrinsicWidth
            val iconRight = iconLeft + deleteIcon.intrinsicWidth
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconTop + deleteIcon.intrinsicHeight)
            deleteIcon.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}