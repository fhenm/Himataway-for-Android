package com.github.fhenm.himataway.fragment.main.tab

import android.os.AsyncTask
import android.view.View
import com.github.fhenm.himataway.event.model.StreamingCreateFavoriteEvent
import com.github.fhenm.himataway.event.model.StreamingUnFavoriteEvent
import com.github.fhenm.himataway.model.*
import com.github.fhenm.himataway.model.*
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status

/**
 * お気に入りタブ
 */
class FavoritesFragment : BaseFragment() {

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    override val tabId: Long
        get() = TabManager.FAVORITES_TAB_ID

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイート＋ふぁぼ）
     * CreateFavoriteEventをキャッチしている為、ふぁぼイベントを受け取ることが出来る
     * @return trueは表示しない、falseは表示する
     */
    override fun isSkip(row: Row): Boolean {
        return !row.isFavorite || row.source.id != AccessTokenManager.getUserId()
    }

    override fun taskExecute() {
        FavoritesTask().execute()
    }

    private inner class FavoritesTask : AsyncTask<Void, Void, ResponseList<Status>>() {
        override fun doInBackground(vararg params: Void): ResponseList<twitter4j.Status>? {
            try {
                val paging = Paging()
                if (mMaxId > 0 && !mReloading) {
                    paging.maxId = mMaxId - 1
                    paging.count = BasicSettings.getPageCount()
                }
                return TwitterManager.getTwitter().getFavorites(paging)
            } catch (e: OutOfMemoryError) {
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

        override fun onPostExecute(statuses: ResponseList<twitter4j.Status>?) {
            mFooter.visibility = View.GONE
            if (statuses == null || statuses.size == 0) {
                mReloading = false
                mPullToRefreshLayout.isRefreshing = false
                setListViewVisible(true)
                return
            }
            if (mReloading) {
                clear()
                for (status in statuses) {
                    FavRetweetManager.setFav(status.id)
                    if (mMaxId <= 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }
                    mAdapter!!.add(Row.newStatus(status))
                }
                mReloading = false
            } else {
                for (status in statuses) {
                    FavRetweetManager.setFav(status.id)
                    if (mMaxId <= 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }
                    mAdapter!!.extensionAdd(Row.newStatus(status))
                }
                mAutoLoader = true
                setListViewVisible(true)
            }
            mAdapter.notifyDataSetChanged()
            mPullToRefreshLayout.isRefreshing = false
        }
    }

    /**
     * ストリーミングAPIからふぁぼを受け取った時のイベント
     * @param event ふぁぼイベント
     */
    fun onEventMainThread(event: StreamingCreateFavoriteEvent) {
        addStack(event.row)
    }

    /**
     * ストリーミングAPIからあんふぁぼイベントを受信
     * @param event ツイート
     */
    fun onEventMainThread(event: StreamingUnFavoriteEvent) {
        val removePositions = mAdapter!!.removeStatus(event.status.id)
        updatePositionForRemove(removePositions)
    }
}
