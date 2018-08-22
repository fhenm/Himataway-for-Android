package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.TweetListBasedFragment
import com.github.fhenm.himataway.viewmodel.MyInteractionsFragmentViewModel

class MyInteractionsFragment : TweetListBasedFragment<MyInteractionsFragmentViewModel>() {

    override fun createViewModel(dummy: Unit): MyInteractionsFragmentViewModel =
            ViewModelProviders
                    .of(this, MyInteractionsFragmentViewModel.Factory(
                            this.getTwitterRepo()
                    ))
                    .get(MyInteractionsFragmentViewModel::class.java)
}