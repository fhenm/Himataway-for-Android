package com.github.fhenm.himataway.viewmodel

enum class AddtionalType {
    AddToBottom,
    AddToTop,
    Clear
}

/**
 * プロフィール画面のリスト(RecycleView)に与えるデータ
 */
data class ProfileItemList<T>(
        val items:List<T>,
        val addType: AddtionalType
)
