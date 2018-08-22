/**
 * RecyclerView 関連の拡張メソッド群
 */
package com.github.fhenm.himataway.extensions

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.view.View

fun RecyclerView.addOnPagingListener(listener:()->Unit) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(view: RecyclerView?, scrollState: Int) {}

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            // see - http://recyclerview.hatenablog.com/entry/2016/11/05/182404
            val totalCount = recyclerView!!.adapter.itemCount //合計のアイテム数
            val childCount = recyclerView.childCount // RecyclerViewに表示されてるアイテム数
            val layoutManager = recyclerView.layoutManager

            if (layoutManager is GridLayoutManager) { // GridLayoutManager
                val firstPosition = layoutManager.findFirstVisibleItemPosition() // RecyclerViewに表示されている一番上のアイテムポジション
                if (totalCount == childCount + firstPosition) {
                    // ページング処理
                    listener()
                }
            } else if (layoutManager is LinearLayoutManager) { // LinearLayoutManager
                val firstPosition = layoutManager.findFirstVisibleItemPosition() // RecyclerViewの一番上に表示されているアイテムのポジション
                if (totalCount == childCount + firstPosition) {
                    // ページング処理
                    listener()
                }
            }
        }
    })
}

/**
 * Returns the adapter position of the first visible view. This position does not include
 * adapter changes that were dispatched after the last layout pass.
 *
 * @return The adapter position of the first visible item or [RecyclerView.NO_POSITION] if
 * there aren't any visible items.
 */
fun RecyclerView.firstVisiblePosition(): Int {
    val child = findOneVisibleChild(0, layoutManager.childCount, false, true)
    return if (child == null) RecyclerView.NO_POSITION else this.getChildAdapterPosition(child)
}

/**
 * Returns the adapter position of the first fully visible view. This position does not include
 * adapter changes that were dispatched after the last layout pass.
 *
 * @return The adapter position of the first fully visible item or
 * [RecyclerView.NO_POSITION] if there aren't any visible items.
 */
fun RecyclerView.findFirstCompletelyVisibleItemPosition(): Int {
    val child = findOneVisibleChild(0, layoutManager.childCount, true, false)
    return if (child == null) RecyclerView.NO_POSITION else this.getChildAdapterPosition(child)
}

/**
 * Returns the adapter position of the last visible view. This position does not include
 * adapter changes that were dispatched after the last layout pass.
 *
 * @return The adapter position of the last visible view or [RecyclerView.NO_POSITION] if
 * there aren't any visible items
 */
fun RecyclerView.findLastVisibleItemPosition(): Int {
    val child = findOneVisibleChild(layoutManager.childCount - 1, -1, false, true)
    return if (child == null) RecyclerView.NO_POSITION else this.getChildAdapterPosition(child)
}

/**
 * Returns the adapter position of the last fully visible view. This position does not include
 * adapter changes that were dispatched after the last layout pass.
 *
 * @return The adapter position of the last fully visible view or
 * [RecyclerView.NO_POSITION] if there aren't any visible items.
 */
fun RecyclerView.findLastCompletelyVisibleItemPosition(): Int {
    val child = findOneVisibleChild(layoutManager.childCount - 1, -1, true, false)
    return if (child == null) RecyclerView.NO_POSITION else this.getChildAdapterPosition(child)
}

fun RecyclerView.setSelection(position:Int) {
    this.layoutManager.scrollToPosition(position)
}

fun RecyclerView.setSelectionFromTop(pos:Int, offset:Int) {
    var layoutManager = this.layoutManager
    if (layoutManager is LinearLayoutManager) {
        layoutManager.scrollToPositionWithOffset(pos, offset)
    }
}

private fun RecyclerView.findOneVisibleChild(fromIndex: Int, toIndex: Int, completelyVisible: Boolean,
                                             acceptPartiallyVisible: Boolean): View? {
    val helper: OrientationHelper
    if (layoutManager.canScrollVertically()) {
        helper = OrientationHelper.createVerticalHelper(layoutManager)
    } else {
        helper = OrientationHelper.createHorizontalHelper(layoutManager)
    }

    val start = helper.startAfterPadding
    val end = helper.endAfterPadding
    val next = if (toIndex > fromIndex) 1 else -1
    var partiallyVisible: View? = null
    var i = fromIndex
    while (i != toIndex) {
        val child = layoutManager.getChildAt(i)
        val childStart = helper.getDecoratedStart(child)
        val childEnd = helper.getDecoratedEnd(child)
        if (childStart < end && childEnd > start) {
            if (completelyVisible) {
                if (childStart >= start && childEnd <= end) {
                    return child
                } else if (acceptPartiallyVisible && partiallyVisible == null) {
                    partiallyVisible = child
                }
            } else {
                return child
            }
        }
        i += next
    }
    return partiallyVisible
}

