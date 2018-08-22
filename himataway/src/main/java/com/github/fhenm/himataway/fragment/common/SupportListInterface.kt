package com.github.fhenm.himataway.fragment.common

interface SupportListInterface {
    val isTop: Boolean get

    fun goToTop(): Boolean

    fun firstLoad()

    fun reload()
}