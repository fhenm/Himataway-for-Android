package com.github.fhenm.himataway.fragment.main.tab

import android.os.AsyncTask
import android.os.Bundle
import android.view.View

import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.model.TabManager
import com.github.fhenm.himataway.model.TwitterManager
import twitter4j.Query
import twitter4j.QueryResult

/**
 * 検索タブ
 */
class SearchFragment : BaseFragment() {

    private var mQuery: Query? = null
    private var mSearchWord: String? = null

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    override val tabId: Long
        get() = TabManager.SEARCH_TAB_ID - Math.abs(mSearchWord!!.hashCode())

//    fun getSearchWord(): String {
//        return mSearchWord ?: ""
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mSearchWord == null) {
            mSearchWord = arguments!!.getString("searchWord")
        }
        super.onActivityCreated(savedInstanceState)
    }

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイート＋ふぁぼ）
     * CreateFavoriteEventをキャッチしている為、ふぁぼイベントを受け取ることが出来る
     * @return trueは表示しない、falseは表示する
     */
    override fun isSkip(row: Row): Boolean {
        if (row.isStatus) {
            if (row.status.text.contains(mSearchWord!!)) {
                return false
            }
        }
        return true
    }

    override fun taskExecute() {
        SearchTask().execute()
    }

    private inner class SearchTask : AsyncTask<Void, Void, QueryResult>() {
        override fun doInBackground(vararg params: Void): QueryResult? {
            try {
                val query: Query
                if (mQuery != null && !mReloading) {
                    query = mQuery!!
                } else {
                    query = Query(mSearchWord + " exclude:retweets")
                }
                return TwitterManager.getTwitter().search(query)
            } catch (e: OutOfMemoryError) {
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

        override fun onPostExecute(queryResult: QueryResult?) {
            mFooter.visibility = View.GONE
            if (queryResult == null) {
                mReloading = false
                mPullToRefreshLayout.isRefreshing = false
                setListViewVisible(true)
                mQuery = null
                return
            }
            if (mReloading) {
                clear()
                for (status in queryResult.tweets) {
                    mAdapter!!.add(Row.newStatus(status))
                }
                mReloading = false
                if (queryResult.hasNext()) {
                    mQuery = queryResult.nextQuery()
                    mAutoLoader = true
                } else {
                    mQuery = null
                    mAutoLoader = false
                }
            } else {
                for (status in queryResult.tweets) {
                    mAdapter.extensionAdd(Row.newStatus(status))
                }
                mAutoLoader = true
                mQuery = queryResult.nextQuery()
                setListViewVisible(true)
            }
            mPullToRefreshLayout.isRefreshing = false
        }
    }
}
