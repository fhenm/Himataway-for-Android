package com.github.fhenm.himataway

import android.databinding.DataBindingUtil
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import com.github.fhenm.himataway.adapter.SimplePagerAdapter
import com.github.fhenm.himataway.databinding.ActivityUserListBinding
import com.github.fhenm.himataway.fragment.list.UserListStatusesFragment
import com.github.fhenm.himataway.fragment.list.UserMemberFragment
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.model.UserListCache
import com.github.fhenm.himataway.util.MessageUtil
import com.github.fhenm.himataway.util.ThemeUtil
import com.github.fhenm.himataway.R
import twitter4j.UserList

class UserListActivity : AppCompatActivity() {

    private lateinit var mUserList: UserList
    private var mIsFollowing: Boolean = false
    private var mCurrentPosition = 0
    private var mColorBlue: Int = 0
    private var mColorWhite: Int = 0

    private lateinit var binding: ActivityUserListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_list)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        val intent = intent
        val users = intent.getSerializableExtra("userList") as UserList?
        if (users == null) {
            return
        }
        mUserList = users;
        mIsFollowing = mUserList.isFollowing

        mColorBlue = ThemeUtil.getThemeTextColor(R.attr.holo_blue)
        mColorWhite = ThemeUtil.getThemeTextColor(R.attr.text_color)
        binding.usersLabel.setTextColor(mColorBlue)

        title = mUserList.fullName

        /**
         * スワイプで動かせるタブを実装するのに最低限必要な実装
         */
        val pagerAdapter = SimplePagerAdapter(this, binding.listPager)
        val args = Bundle()
        args.putLong("listId", mUserList.id)

        pagerAdapter.addTab(UserMemberFragment::class.java, args)
        pagerAdapter.addTab(UserListStatusesFragment::class.java, args)
        pagerAdapter.notifyDataSetChanged()
        binding.listPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    binding.usersLabel.setTextColor(mColorBlue)
                } else {
                    binding.tweetsLabel.setTextColor(mColorBlue)
                }

                if (mCurrentPosition == 0) {
                    binding.usersLabel.setTextColor(mColorWhite)
                } else {
                    binding.tweetsLabel.setTextColor(mColorWhite)
                }

                mCurrentPosition = position
            }
        })

        binding.usersLabel.setOnClickListener {
            binding.listPager.currentItem = 0
        }

        binding.tweetsLabel.setOnClickListener {
            binding.listPager.currentItem = 1
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_CREATE, Menu.NONE, R.string.menu_create_user_list_subscription)
        menu.add(Menu.NONE, MENU_DESTROY, Menu.NONE, R.string.menu_destroy_user_list_subscription)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val create = menu.findItem(MENU_CREATE)
        val destroy = menu.findItem(MENU_DESTROY)
        if (create == null || destroy == null) {
            return false
        }
        if (mIsFollowing!!) {
            create.isVisible = false
            destroy.isVisible = true
        } else {
            create.isVisible = true
            destroy.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            MENU_CREATE -> object : AsyncTask<Void, Void, Boolean>() {
                override fun doInBackground(vararg params: Void): Boolean? {
                    try {
                        TwitterManager.getTwitter().createUserListSubscription(mUserList!!.id)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }

                }

                override fun onPostExecute(success: Boolean?) {
                    if (success!!) {
                        MessageUtil.showToast(R.string.toast_create_user_list_subscription_success)
                        mIsFollowing = true
                        val userLists = UserListCache.getUserLists()
                        userLists?.add(0, mUserList)
                    } else {
                        MessageUtil.showToast(R.string.toast_create_user_list_subscription_failure)
                    }
                }
            }.execute()
            MENU_DESTROY -> object : AsyncTask<Void, Void, Boolean>() {
                override fun doInBackground(vararg params: Void): Boolean? {
                    try {
                        TwitterManager.getTwitter().destroyUserListSubscription(mUserList!!.id)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }

                }

                override fun onPostExecute(success: Boolean?) {
                    if (success!!) {
                        MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_success)
                        mIsFollowing = false
                        val userLists = UserListCache.getUserLists()
                        userLists?.remove(mUserList)
                    } else {
                        MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_failure)
                    }
                }
            }.execute()
        }
        return true
    }

    companion object {
        private val MENU_CREATE = 1
        private val MENU_DESTROY = 2
    }
}
