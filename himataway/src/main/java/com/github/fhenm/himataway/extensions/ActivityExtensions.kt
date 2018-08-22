package com.github.fhenm.himataway.extensions

import android.app.Activity
import com.github.fhenm.himataway.himatawayApplication
import com.github.fhenm.himataway.repository.TwitterRepository

fun Activity.getTwitterRepo() : TwitterRepository {
    val app = (this.application as himatawayApplication)!!
    /*return app.twitterRepo!!*/
    return app.twitterRepo()
}