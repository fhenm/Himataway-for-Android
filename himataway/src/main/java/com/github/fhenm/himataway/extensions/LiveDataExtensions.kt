/**
 * LiveData に関する拡張関数群
 */
package com.github.fhenm.himataway.extensions

import android.arch.lifecycle.LiveData

/** null でない値を取得する。null の場合例外。 */
fun <T> LiveData<T>.get() : T = this.value!!