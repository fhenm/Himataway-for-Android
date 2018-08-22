package com.github.fhenm.himataway

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import com.github.fhenm.himataway.adapter.SimplePagerAdapter
import com.github.fhenm.himataway.fragment.mute.SourceFragment
import com.github.fhenm.himataway.fragment.mute.UserFragment
import com.github.fhenm.himataway.fragment.mute.WordFragment
import com.github.fhenm.himataway.util.ThemeUtil
import com.github.fhenm.himataway.R

class MuteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_mute)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        val viewPager = findViewById<View>(R.id.pager) as ViewPager
        viewPager.offscreenPageLimit = 3 // 3だと不要なんだけど一応...

        val simplePagerAdapter = SimplePagerAdapter(this, viewPager)
        simplePagerAdapter.addTab(UserFragment::class.java, null)
        simplePagerAdapter.addTab(SourceFragment::class.java, null)
        simplePagerAdapter.addTab(WordFragment::class.java, null)
        simplePagerAdapter.notifyDataSetChanged()

        val colorBlue = ThemeUtil.getThemeTextColor(R.attr.holo_blue)
        val colorWhite = ThemeUtil.getThemeTextColor(R.attr.text_color)

        /**
         * タブのラベル情報を配列に入れておく
         */
        val tabs = arrayOf(findViewById<View>(R.id.tab_user) as TextView, findViewById<View>(R.id.tab_source) as TextView, findViewById<View>(R.id.tab_word) as TextView)

        /**
         * タップしたら移動
         */
        for (i in tabs.indices) {
            tabs[i].setOnClickListener { viewPager.currentItem = i }
        }

        /**
         * 最初のタブを青くする
         */
        tabs[0].setTextColor(colorBlue)

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {

                /**
                 * タブのindexと選択されたpositionを比較して色を設定
                 */
                for (i in tabs.indices) {
                    tabs[i].setTextColor(if (i == position) colorBlue else colorWhite)
                }
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}
