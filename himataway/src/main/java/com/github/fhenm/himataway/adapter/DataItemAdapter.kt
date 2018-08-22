package com.github.fhenm.himataway.adapter

import android.support.v7.widget.RecyclerView

/**
 * プロフィール画面の RecyclerView の共通Adapter
 */
abstract class DataItemAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** アイテムを追加する */
    abstract fun add(item: T)

    /** アイテムを挿入する */
    abstract fun insert(index:Int, item: T)

    /** アイテムを ID で削除する */
    abstract fun remove(id: Long)

    /** アイテムを全て削除する */
    abstract fun clear()

    /** アイテムがタップされた時のハンドラ */
    abstract var onItemClickListener: ((T)->Unit)

    /** アイテムがロングタップされた時のハンドラ */
    abstract var onItemLongClickListener: ((T)->Boolean)
}