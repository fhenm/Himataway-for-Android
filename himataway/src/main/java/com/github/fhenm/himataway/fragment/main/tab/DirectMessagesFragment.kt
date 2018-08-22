package com.github.fhenm.himataway.fragment.main.tab

import android.os.AsyncTask
import android.view.View

import java.util.Collections

import com.github.fhenm.himataway.event.model.StreamingDestroyMessageEvent
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.model.TabManager
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.DirectMessage
import twitter4j.Paging
import twitter4j.ResponseList

class DirectMessagesFragment : BaseFragment() {

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    override val tabId: Long
        get() = TabManager.DIRECT_MESSAGES_TAB_ID

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイートやDM）
     * @return trueは表示しない、falseは表示する
     */
    override fun isSkip(row: Row): Boolean {
        return !row.isDirectMessage
    }

    override fun taskExecute() {
        DirectMessagesTask().execute()
    }

    private inner class DirectMessagesTask : AsyncTask<Void, Void, ResponseList<DirectMessage>>() {
        override fun doInBackground(vararg params: Void): ResponseList<DirectMessage>? {
            try {
                val twitter = TwitterManager.getTwitter()

                // 受信したDM
                val directMessagesPaging = Paging()
                if (mDirectMessagesMaxId > 0 && !mReloading) {
                    directMessagesPaging.maxId = mDirectMessagesMaxId - 1
                    directMessagesPaging.count = BasicSettings.getPageCount() / 2
                } else {
                    directMessagesPaging.count = 10
                }
                val directMessages = twitter.getDirectMessages(directMessagesPaging)
                for (directMessage in directMessages) {
                    if (mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > directMessage.id) {
                        mDirectMessagesMaxId = directMessage.id
                    }
                }

                // 送信したDM
                val sentDirectMessagesPaging = Paging()
                if (mSentDirectMessagesMaxId > 0 && !mReloading) {
                    sentDirectMessagesPaging.maxId = mSentDirectMessagesMaxId - 1
                    sentDirectMessagesPaging.count = BasicSettings.getPageCount() / 2
                } else {
                    sentDirectMessagesPaging.count = 10
                }
                val sentDirectMessages = twitter.getSentDirectMessages(sentDirectMessagesPaging)
                for (directMessage in sentDirectMessages) {
                    if (mSentDirectMessagesMaxId <= 0L || mSentDirectMessagesMaxId > directMessage.id) {
                        mSentDirectMessagesMaxId = directMessage.id
                    }
                }

                directMessages.addAll(sentDirectMessages)

                // 日付でソート
                Collections.sort(directMessages) { arg0, arg1 ->
                    arg1.createdAt.compareTo(
                            arg0.createdAt)
                }
                return directMessages
            } catch (e: OutOfMemoryError) {
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

        override fun onPostExecute(statuses: ResponseList<DirectMessage>?) {
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
                    mAdapter!!.add(Row.newDirectMessage(status))
                }
                mReloading = false
            } else {
                for (status in statuses) {
                    mAdapter!!.extensionAdd(Row.newDirectMessage(status))
                }
                mAutoLoader = true
                setListViewVisible(true)
            }
            mAdapter.notifyDataSetChanged()
            mPullToRefreshLayout.isRefreshing = false
        }
    }

    /**
     * DM削除通知
     * @param event DMのID
     */
    fun onEventMainThread(event: StreamingDestroyMessageEvent) {
        mAdapter!!.removeDirectMessage(event.statusId!!)
    }
}
