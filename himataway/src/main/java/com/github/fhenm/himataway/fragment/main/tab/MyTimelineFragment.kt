package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.TweetListBasedFragment
import com.github.fhenm.himataway.viewmodel.MyTimelineFragmentViewModel

class MyTimelineFragment : TweetListBasedFragment<MyTimelineFragmentViewModel>() {

    override fun createViewModel(dummy: Unit): MyTimelineFragmentViewModel =
            ViewModelProviders
                    .of(this, MyTimelineFragmentViewModel.Factory(
                            this.getTwitterRepo()
                    ))
                    .get(MyTimelineFragmentViewModel::class.java)

}