package com.github.fhenm.himataway.extensions

import android.support.v4.app.Fragment
import com.github.fhenm.himataway.repository.TwitterRepository

fun Fragment.getTwitterRepo() : TwitterRepository {
    return this.activity!!.getTwitterRepo()
}