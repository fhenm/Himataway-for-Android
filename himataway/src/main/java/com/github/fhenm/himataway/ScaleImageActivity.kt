package com.github.fhenm.himataway

import android.app.Activity
import android.app.ActivityOptions
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import com.github.fhenm.himataway.adapter.SimplePagerAdapter
import com.github.fhenm.himataway.databinding.ActivityScaleImageBinding
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.ScaleImageFragment
import com.github.fhenm.himataway.util.ImageUtil
import com.github.fhenm.himataway.util.StatusUtil
import com.github.fhenm.himataway.viewmodel.ScaleImageActivityViewModel
import com.github.fhenm.himataway.R
import twitter4j.Status

/**
 * 画像の拡大表示用のActivity、かぶせて使う
 *
 * @author aska
 */
class ScaleImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DataBindingUtil.setContentView<ActivityScaleImageBinding>(this, R.layout.activity_scale_image)

        val viewModel = ViewModelProviders
                .of(this, ScaleImageActivityViewModel.Factory(
                        this.getTwitterRepo()
                ))
                .get(ScaleImageActivityViewModel::class.java)

        val adapter = SimplePagerAdapter(this, binding.pager)
        binding.symbol.setViewPager(binding.pager)
        binding.pager.offscreenPageLimit = 4
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                binding.pager.visibility = View.VISIBLE
                binding.transitionImage.visibility = View.GONE
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })

        viewModel.statusWithIndex.observe(this, Observer { pair ->
            if (pair != null) {
                showStatus(binding, adapter, pair.first, pair.second)
            }
        })

        if (intent != null) {
            viewModel.loadImage(intent)
        }
    }

    private fun showStatus(binding: ActivityScaleImageBinding, adapter: SimplePagerAdapter,
                           status: twitter4j.Status, index: Int) {
        val imageUrls = mutableListOf<String>()

        val urls = StatusUtil.getImageUrls(status)
        if (urls.size == 1) {
            binding.symbol.visibility = View.GONE
        }
        for (imageURL in urls) {
            imageUrls.add(imageURL)
            val args = Bundle()
            args.putString("url", imageURL)
            adapter.addTab(ScaleImageFragment::class.java, args)
        }
        // Activity Transition 用の TransitionName を設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //ImageUtil.displayImage(imageUrls[index], binding.transitionImage) //ここいらないよ？
            binding.transitionImage.transitionName = getString(R.string.transition_tweet_image)
        }

        adapter.notifyDataSetChanged()
        binding.pager.currentItem = index
    }

    companion object {
        /** 画像表示用の StartActivity  */
        fun startActivityWithImage(
                activity: Activity,
                status: Status,
                openIndex: Int,
                sharedView: View?,
                transitionName: String?) {
            val intent = Intent(activity, ScaleImageActivity::class.java)
            intent.putExtra("status", status)
            intent.putExtra("index", openIndex)

            val options: ActivityOptions? = if (sharedView != null && transitionName != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions.makeSceneTransitionAnimation(activity,
                            sharedView, transitionName)
                } else {
                    null
                }
            } else {
                null
            }
            if (options != null) {
                activity.startActivity(intent, options.toBundle())
            } else {
                activity.startActivity(intent)
            }
        }
    }
}

