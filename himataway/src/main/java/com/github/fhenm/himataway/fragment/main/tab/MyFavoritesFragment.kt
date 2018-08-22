package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.TweetListBasedFragment
import com.github.fhenm.himataway.viewmodel.MyFavoritesFragmentViewModel

class MyFavoritesFragment : TweetListBasedFragment<MyFavoritesFragmentViewModel>() {

    override fun createViewModel(dummy: Unit): MyFavoritesFragmentViewModel =
            ViewModelProviders
                    .of(this, MyFavoritesFragmentViewModel.Factory(
                            this.getTwitterRepo()
                    ))
                    .get(MyFavoritesFragmentViewModel::class.java)
}