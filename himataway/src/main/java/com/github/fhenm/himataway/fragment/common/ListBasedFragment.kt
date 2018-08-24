package com.github.fhenm.himataway.fragment.common

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.fhenm.himataway.adapter.DividerItemDecoration
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.databinding.PullToRefreshList2Binding
import com.github.fhenm.himataway.extensions.addOnPagingListener
import com.github.fhenm.himataway.extensions.firstVisiblePosition
import com.github.fhenm.himataway.extensions.setSelection
import com.github.fhenm.himataway.extensions.setSelectionFromTop
import com.github.fhenm.himataway.viewmodel.AddtionalType
import com.github.fhenm.himataway.viewmodel.ListBasedFragmentViewModel
import twitter4j.TwitterResponse

/**
 * List(RecyclerView) を持つ画面のベースFragment
 *
 * TViewItem - RecyleView に表示する行の型
 * TId - TDataItem を識別するIDの型
 * TDataItem - API等から読み込んだデータ1行の型
 * TCursor - API読み込み結果の「次」の情報を示す型
 * TViewModel - Fragment に対応させる ViewModel の型
 */
abstract class ListBasedFragment<
        TViewItem,
        TId,
        TDataItem : TwitterResponse?,
        TCursor,
        TViewModel : ListBasedFragmentViewModel<TId, TDataItem, TCursor>>
    : Fragment(), SupportListInterface {

    /*** 実装クラスで、 Fragment 用の ViewModel を生成する */
    abstract fun createViewModel(id: TId): TViewModel

    /*** 実装クラスで、RecyclerView に設定する Adapter を生成する */
    abstract fun createAdapter() : DataItemAdapter<TViewItem>

    /*** 実装クラスで、モデル側の型からView用の型へ変換する */
    abstract fun convertDataToViewItem(dataItem:TDataItem): TViewItem

    /*** 実装クラスで、モデル側の型のIDを得る */
    abstract val id : TId

    protected lateinit var adapter : DataItemAdapter<TViewItem>

    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: TViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bin = inflater?.let { inf -> PullToRefreshList2Binding.inflate(inf, container, false) }
        if (bin == null) {
            return null
        }
        val binding = bin

        recyclerView = binding.recyclerView

        viewModel = createViewModel(id)

        viewModel.startReceiveStreaming()

        // RecyclerView の設定
        binding.recyclerView.visibility = View.GONE
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context)) // 罫線付ける

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(binding.recyclerView)

        // Status(ツイート)をViewに描写するアダプター
        adapter = createAdapter()
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnPagingListener {
            // ページング処理(追加読み込み)
            viewModel.loadListItems(true)
        }

        // Pull to Refresh の開始
        binding.ptrLayout.setOnRefreshListener {
            // 洗い替え
            viewModel.loadListItems(false)
        }

        // ViewModel の監視

        // Toast の表示
        viewModel.toast.observe(this, Observer { toast ->
            if (toast != null) {
                Toast.makeText(activity, toast, Toast.LENGTH_LONG).show()
            }
        })

        // 追加読み込みの Progress
        viewModel.isVisibleBottomProgress.observe(this, Observer { isVisible ->
            binding.guruguru.visibility = if (isVisible ?: false) View.VISIBLE else View.GONE
        })

        // Pull to Refresh の Progress
        viewModel.isVisiblePullProgress.observe(this, Observer { isVisible ->
            binding.ptrLayout.isRefreshing = isVisible ?: false
        })

        // リストビューの Visible
        viewModel.isVisibleListView.observe(this, Observer { isVisible ->
            binding.recyclerView.visibility = if (isVisible ?: false) View.VISIBLE else View.GONE
        })

        // 読み込んだデータを RecyclerView のアダプタに適用
        viewModel.listItems.observe(this, Observer { data ->
            if (data == null) {
                return@Observer
            }

            // 表示している要素の位置
            val position = binding.recyclerView.firstVisiblePosition()

            // 縦スクロール位置
            val view = binding.recyclerView.getChildAt(0)

            // 追加でなかったら全消し
            if (data.addType == AddtionalType.Clear) {
                adapter.clear()
            }

            var count = 0
            if (data.addType == AddtionalType.AddToBottom || data.addType == AddtionalType.Clear) {
                for (dataItem in data.items) {
                    adapter.add(convertDataToViewItem(dataItem))
                    count++
                }
            } else {
                for (dataItem in data.items) {
                    adapter.insert(0, convertDataToViewItem(dataItem))
                    count++
                }
            }

            adapter.notifyDataSetChanged()

            if (data.addType == AddtionalType.Clear) {
                binding.recyclerView.setSelection(0)
            } else if (data.addType == AddtionalType.AddToTop) {
                val y = view?.top ?: 0

                val isKeepOnTop = position == 0 && y == 0 // && count < 3

                if (isKeepOnTop) {
                    binding.recyclerView.setSelection(0)
                } else {
                    // 少しでもスクロールさせている時は画面を動かさない様にスクロー位置を復元する
                    binding.recyclerView.setSelectionFromTop(position + count, y)

//            // 未読の新規ツイートをチラ見せ
//            if (position == 0 && y == 0) {
//                mListView!!.smoothScrollToPositionFromTop(position + count, 120)
//            }
                }
            }
        })

        if (arguments?.getBoolean("load") ?: false) {
            // 初回のデータ読み込み(ViewModel の init でやるべき？)
            firstLoad()
        }

        return binding.root
    }

    override val isTop: Boolean
        get() = recyclerView.firstVisiblePosition() == 0

    override fun goToTop(): Boolean {
        recyclerView.setSelection(0)
        return true
    }

    private var isLoaded : Boolean = false
    override fun firstLoad() {
        if (!isLoaded) {
            viewModel.loadListItems(false)
            isLoaded = true
        }
    }

    override fun reload() {
        viewModel.loadListItems(false)
    }

}

