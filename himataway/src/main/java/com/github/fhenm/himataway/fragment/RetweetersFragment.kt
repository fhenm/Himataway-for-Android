package com.github.fhenm.himataway.fragment

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.github.fhenm.himataway.R
import com.github.fhenm.himataway.adapter.RecyclerUserAdapter
import com.github.fhenm.himataway.databinding.FragmentRecyclerRetweetersBinding
import com.github.fhenm.himataway.extensions.firstVisiblePosition
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.extensions.setSelection
import com.github.fhenm.himataway.extensions.setSelectionFromTop
import com.github.fhenm.himataway.viewmodel.AddtionalType
import com.github.fhenm.himataway.viewmodel.RetweetersFragmentViewModel


/**
 * リツイートを表示
 *
 * @author fhenm, ms~~~~
 */
class RetweetersFragment : DialogFragment() {

    private lateinit var adapter: RecyclerUserAdapter

    private lateinit var binding: FragmentRecyclerRetweetersBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        binding = DataBindingUtil.inflate<FragmentRecyclerRetweetersBinding>(LayoutInflater.from(context),
                R.layout.fragment_recycler_retweeters, null, false)
        dialog.setContentView(binding.getRoot())

        adapter = RecyclerUserAdapter(activity!!, ArrayList())
        binding.recyclerView.adapter = adapter

        val statusId = arguments!!.getLong("statusId")

        val viewModel = ViewModelProviders
                .of(this, RetweetersFragmentViewModel.Factory(
                        this.getTwitterRepo(),
                        statusId
                ))
                .get(RetweetersFragmentViewModel::class.java)

        // ViewModel の監視

        // 追加読み込みの Progress
        viewModel.isVisibleBottomProgress.observe(this, Observer { isVisible ->
            binding.guruguru.visibility = if (isVisible ?: false) View.VISIBLE else View.GONE
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
                    adapter.add(dataItem)
                    count++
                }
            } else {
                for (dataItem in data.items) {
                    adapter.insert(0, dataItem)
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

        viewModel.loadListItems(false)

        return dialog
    }
}
