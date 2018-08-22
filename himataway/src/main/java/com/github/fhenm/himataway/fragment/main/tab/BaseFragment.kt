package com.github.fhenm.himataway.fragment.main.tab

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING
import android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.ProgressBar

import com.github.fhenm.himataway.event.NewRecordEvent
import com.github.fhenm.himataway.event.action.GoToTopEvent
import com.github.fhenm.himataway.event.action.PostAccountChangeEvent
import com.github.fhenm.himataway.event.action.StatusActionEvent
import com.github.fhenm.himataway.event.model.StreamingCreateStatusEvent
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent
import com.github.fhenm.himataway.event.settings.BasicSettingsChangeEvent
import com.github.fhenm.himataway.listener.StatusLongClickListener
import com.github.fhenm.himataway.model.AccessTokenManager
import com.github.fhenm.himataway.model.Row

import java.util.ArrayList

import de.greenrobot.event.EventBus
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.databinding.PullToRefreshList2Binding
import com.github.fhenm.himataway.extensions.addOnPagingListener
import com.github.fhenm.himataway.extensions.firstVisiblePosition
import com.github.fhenm.himataway.extensions.setSelection
import com.github.fhenm.himataway.extensions.setSelectionFromTop
import com.github.fhenm.himataway.fragment.common.SupportListInterface
import com.github.fhenm.himataway.fragment.dialog.StatusMenuFragment

abstract class BaseFragment : Fragment(), SupportListInterface {

    protected  lateinit var mAdapter: RecyclerTweetAdapter
    protected var mAutoLoader = false
    protected var mReloading = false
    private var mScrolling = false
    protected var mMaxId = 0L // 読み込んだ最新のツイートID
    protected var mDirectMessagesMaxId = 0L // 読み込んだ最新の受信メッセージID
    protected var mSentDirectMessagesMaxId = 0L // 読み込んだ最新の送信メッセージID
    private val mStackRows = ArrayList<Row>()

    private lateinit var mListView: RecyclerView
    protected lateinit var mFooter: ProgressBar
    protected lateinit var mPullToRefreshLayout: SwipeRefreshLayout

    fun setListViewVisible(isVisible:Boolean) {
        mListView.visibility = if (isVisible) View.VISIBLE else View.GONE;
    }

    override val isTop: Boolean
        get() = mListView.firstVisiblePosition() == 0

    /**
     * ツイートの表示処理、画面のスクロール位置によって適切な処理を行う、まだバグがある
     */
    private val mRender = Runnable {
        if (mScrolling) {
            return@Runnable
        }
        if (mAdapter == null) {
            return@Runnable
        }

        // 表示している要素の位置
        val position = mListView.firstVisiblePosition()

        // 縦スクロール位置
        val view = mListView.getChildAt(0)
        val y = view?.top ?: 0

        // 要素を上に追加（ addだと下に追加されてしまう ）
        var count = 0
        var highlight = false
        for (row in mStackRows) {
            mAdapter!!.insert(0, row)
            count++
            if (row.isFavorite) {
                // お気に入りしたのが自分じゃない時
                if (row.source.id != AccessTokenManager.getUserId()) {
                    highlight = true
                }
            } else if (row.isStatus) {
                // 投稿主が自分じゃない時
                if (row.status.user.id != AccessTokenManager.getUserId()) {
                    highlight = true
                }
            } else if (row.isDirectMessage) {
                // 投稿主が自分じゃない時
                if (row.message.senderId != AccessTokenManager.getUserId()) {
                    highlight = true
                }
            }
        }
        mStackRows.clear()

        val autoScroll = position == 0 && y == 0 && count < 3

        if (highlight) {
            EventBus.getDefault().post(NewRecordEvent(tabId, searchWord, autoScroll))
        }

        if (autoScroll) {
            mListView.setSelection(0)
        } else {
            // TODO あとで
//            // 少しでもスクロールさせている時は画面を動かさない様にスクロー位置を復元する
//            mListView!!.setSelectionFromTop(position + count, y)
//            // 未読の新規ツイートをチラ見せ
//            if (position == 0 && y == 0) {
//                mListView!!.smoothScrollToPositionFromTop(position + count, 120)
//            }
        }
    }

    /**
     * 1. スクロールが終わった瞬間にストリーミングAPIから受信し溜めておいたツイートがあればそれを表示する
     * 2. スクロールが終わった瞬間に表示位置がトップだったらボタンのハイライトを消すためにイベント発行
     * 3. スクロール中はスクロール中のフラグを立てる
     */
    private val mOnScrollListener2 = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, scrollState: Int) {
            when (scrollState) {
                SCROLL_STATE_IDLE -> {
                    mScrolling = false
                    if (mStackRows.size > 0) {
                        showStack()
                    } else if (isTop) {
                        EventBus.getDefault().post(GoToTopEvent())
                    }
                }
                SCROLL_STATE_TOUCH_SCROLL, SCROLL_STATE_DRAGGING -> mScrolling = true
            }
        }
    }

    /**
     * タブ固有のID、ユーザーリストではリストのIDを、その他はマイナスの固定値を返す
     */
    abstract val tabId: Long

    val searchWord: String
        get() = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bin = PullToRefreshList2Binding.inflate(inflater!!, container, false) ?: return null

        this.mListView = bin.recyclerView
        this.mFooter = bin.guruguru
        this.mPullToRefreshLayout = bin.ptrLayout

        mAdapter = createAdapter()
        mListView.adapter = mAdapter

        mAdapter.onItemClickListener = { row ->
            StatusMenuFragment.newInstance(row)
                    .show(this.activity.supportFragmentManager, "dialog")
        }

        mAdapter.onItemLongClickListener = { row ->
            StatusLongClickListener.handleRow(this.activity, row)
        }

        mListView.addOnScrollListener(mOnScrollListener2)
        mListView.addOnPagingListener {
            additionalReading()
        }

        mListView.visibility = View.VISIBLE
        mFooter.visibility = View.GONE
//        mListView!!.isFastScrollEnabled = BasicSettings.getFastScrollOn()

        this.mPullToRefreshLayout.setOnRefreshListener {
            reload()
        }

        return bin.root
    }

    private fun createAdapter(): RecyclerTweetAdapter {
        return RecyclerTweetAdapter(activity, arrayListOf())
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//
//        /**
//         * mMainPagerAdapter.notifyDataSetChanged() された時に
//         * onCreateView と onActivityCreated がインスタンスが生きたまま呼ばれる
//         * 多重に初期化処理を実行しないように変数チェックを行う
//         */
//        if (mAdapter == null) {
//            // Status(ツイート)をViewに描写するアダプター
//            mAdapter = RecyclerTweetAdapter(activity, arrayListOf())
//            mListView.visibility = View.GONE
//            taskExecute();
//        }
//
//        mListView.adapter = mAdapter
//    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun firstLoad() {
        // 初回読み込み済だったら何もしない
        if (mAutoLoader) {
            return
        }

        // 起動直後に MainActivity から呼ばれる TimelineFragment の firstLoad は、
        // タイミング的にまだ onCreateView 前で mPullToRefreshLayout が null なので
        // とりあえず無視
        // TODO あとでなんとかする
        if (mPullToRefreshLayout != null) {
            mPullToRefreshLayout.isRefreshing = true
        }

        taskExecute()
    }

    override fun reload() {
        mReloading = true
        mPullToRefreshLayout.isRefreshing = true
        taskExecute()
    }

    fun clear() {
        mMaxId = 0L
        mDirectMessagesMaxId = 0L
        mSentDirectMessagesMaxId = 0L
        mAdapter!!.clear()
    }

    protected fun additionalReading() {
        if (!mAutoLoader || mReloading) {
            return
        }
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        taskExecute()
    }

    override fun goToTop(): Boolean {
        if (mListView == null) {
            activity.finish()
            return false
        }
        mListView.setSelection(0)
        if (mStackRows.size > 0) {
            showStack()
            return false
        } else {
            return true
        }
    }

    /**
     * 新しいツイートを表示して欲しいというリクエストを一旦待たせ、
     * 250ms以内に同じリクエストが来なかったら表示する。
     * 250ms以内に同じリクエストが来た場合は、更に250ms待つ。
     * 表示を連続で行うと処理が重くなる為この制御を入れている。
     */
    private fun showStack() {
        if (mListView == null) {
            return
        }
        mListView.removeCallbacks(mRender)
        mListView.postDelayed(mRender, 250)
    }

    /**
     * ストリーミングAPIからツイートやメッセージを受信した時の処理
     * 1. 表示スべき内容かチェックし、不適切な場合はスルーする
     * 2. すぐ表示すると流速が早い時にガクガクするので溜めておく
     *
     * @param row ツイート情報
     */
    fun addStack(row: Row) {
        if (isSkip(row)) {
            return
        }
        mStackRows.add(row)
        if (!mScrolling && isTop) {
            showStack()
        } else {
            EventBus.getDefault().post(NewRecordEvent(tabId, searchWord, false))
        }
    }

    /**
     * そのツイート（またはメッセージ）を表示するかどうかのチェック
     */
    protected abstract fun isSkip(row: Row): Boolean

    /**
     * 読み込み用のAsyncTaskを実行する
     */
    protected abstract fun taskExecute()

    /**
     *
     * !!! EventBus !!!
     *
     */


    /**
     * 高速スクロールの設定が変わったら切り替える
     */
    fun onEventMainThread(event: BasicSettingsChangeEvent) {
//        mListView!!.isFastScrollEnabled = BasicSettings.getFastScrollOn() // TODO RecyclerView にはFastなさそう
    }

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * ストリーミングAPIからツイ消しイベントを受信
     *
     * @param event ツイート
     */
    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        val removePositions = mAdapter!!.removeStatus(event.statusId!!)
        for (removePosition in removePositions) {
            if (removePosition >= 0) {
                val visiblePosition = mListView.firstVisiblePosition()
                if (visiblePosition > removePosition) {
                    val view = mListView.getChildAt(0)
                    val y = view?.top ?: 0
                    mListView.setSelectionFromTop(visiblePosition - 1, y)
                    break
                }
            }
        }
    }

    /**
     * ストリーミングAPIからツイートイベントを受信
     *
     * @param event ツイート
     */
    fun onEventMainThread(event: StreamingCreateStatusEvent) {
        addStack(event.row)
    }

    /**
     * アカウント変更通知を受け、表示中のタブはリロード、表示されていたいタブはクリアを行う
     *
     * @param event アプリが表示しているタブのID
     */
    fun onEventMainThread(event: PostAccountChangeEvent) {
        if (event.tabId == tabId) {
            reload()
        } else {
            clear()
        }
    }

    protected fun updatePositionForRemove(removePositions:List<Int>) {
        for (removePosition in removePositions) {
            if (removePosition >= 0) {
                val visiblePosition = mListView.firstVisiblePosition()
                if (visiblePosition > removePosition) {
                    val view = mListView.getChildAt(0)
                    val y = view?.top ?: 0
                    mListView.setSelectionFromTop(visiblePosition - 1, y)
                    break
                }
            }
        }
    }
}
