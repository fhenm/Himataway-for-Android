package com.github.fhenm.himataway.adapter

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.fhenm.himataway.ProfileActivity
import com.github.fhenm.himataway.R
import com.github.fhenm.himataway.UserListActivity
import com.github.fhenm.himataway.databinding.RowUserListBinding
import com.github.fhenm.himataway.util.ImageUtil
import twitter4j.UserList

class RecyclerUserListView constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr){

    private val binding: RowUserListBinding

    init {
        binding = RowUserListBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun update(userList: UserList){
        val iconUrl = userList.user.biggerProfileImageURL
        ImageUtil.displayRoundedImage(iconUrl, binding.icon)

        binding.listName.text = userList.name
        binding.screenName.setText(userList.getUser().getScreenName() + context.getString(R.string.label_created_by))
        binding.description.setText(userList.getDescription())
        binding.memberCount.setText(userList.getMemberCount().toString() + context.getString(R.string.label_members))

        binding.icon.setOnClickListener { v ->
            val intent = Intent(v.context, ProfileActivity::class.java)
            intent.putExtra("screenName", userList.getUser().getScreenName())
            context.startActivity(intent)
        }

        binding.root.setOnClickListener({ v ->
            val intent = Intent(v.context,
                    UserListActivity::class.java)
            intent.putExtra("userList", userList)
            context.startActivity(intent)
        })
    }
}

class RecyclerUserListViewHolder(private val view: RecyclerUserListView) : RecyclerView.ViewHolder(view) {
    fun update(userList : UserList) {
        view.update(userList)
    }
}

class RecyclerUserListAdapter(
        private val context: Context,
        private val userLists: MutableList<UserList>) :
        DataItemAdapter<UserList>() {

    // 未対応
    override var onItemClickListener: (UserList) -> Unit = {}
    override var onItemLongClickListener: (UserList) -> Boolean = { false }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = RecyclerUserListView(context)
        return RecyclerUserListViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is RecyclerUserListViewHolder){
            holder.update(userLists[position])
        }
    }

    override fun getItemCount(): Int {
        return userLists.count()
    }

    override fun add(userList: UserList) {
        userLists.add(userList)
    }

    override fun insert(index: Int, item: UserList) {
        TODO("not supported") //To change body of created functions use File | Settings | File Templates.
    }

    // 未対応
    override fun clear() {}
    override fun remove(id: Long) { }
}
