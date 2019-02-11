package com.jafca.hsp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(private val parkedLocationAdapter: ParkedLocationAdapter, private val icon: Drawable) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private val background = ColorDrawable(Color.RED)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        parkedLocationAdapter.deleteParkedLocation(position)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive) {
            val itemView = viewHolder.itemView
            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight

            when {
                dX > 0 -> { // Swiping right
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(
                        itemView.left, itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                }
                dX < 0 -> { // Swiping left
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top, itemView.right, itemView.bottom
                    )
                }
                else -> // unSwiped
                    background.setBounds(0, 0, 0, 0)
            }

            background.draw(canvas)
            icon.draw(canvas)
        }

        super.onChildDraw(
            canvas, recyclerView, viewHolder, dX,
            dY, actionState, isCurrentlyActive
        )
    }
}