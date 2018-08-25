package com.github.fhenm.himataway.adapter

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.fhenm.himataway.ProfileActivity
import com.github.fhenm.himataway.databinding.RowUserBinding
import com.github.fhenm.himataway.util.ImageUtil
import twitter4j.User
import java.util.function.Predicate

class RecyclerUserView constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr){

    private val binding: RowUserBinding

    init {
        binding = RowUserBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun update(user: User){
        val iconUrl = user.biggerProfileImageURL
        ImageUtil.displayRoundedImage(iconUrl, binding.icon)

        binding.displayName.setText(user.name)
        binding.screenName.setText("@" + user.screenName)

        var descriptionString: String? = user.description
        if (descriptionString != null && descriptionString.length > 0) {
            val urls = user.descriptionURLEntities
            for (descriptionUrl in urls) {
                descriptionString = descriptionString!!.replace(descriptionUrl.url.toRegex(), descriptionUrl.expandedURL)
            }
            binding.description.setText(descriptionString)
            binding.description.setVisibility(View.VISIBLE)
        } else {
            binding.description.setVisibility(View.GONE)
        }

        if (user.isProtected) {
            binding.lock.setVisibility(View.VISIBLE)
        } else {
            binding.lock.setVisibility(View.INVISIBLE)
        }

        setOnClickListener({ v ->
            val intent = Intent(v.context, ProfileActivity::class.java)
            intent.putExtra("screenName", user.screenName)
            context.startActivity(intent)
        })
    }
}

class RecyclerUserViewHolder(private val view: RecyclerUserView) : RecyclerView.ViewHolder(view) {
    fun update(user : User) {
        view.update(user)
    }
}

class RecyclerUserAdapter(
        private val context: Context,
        private val users: MutableList<User>) :
        DataItemAdapter<User>() {

    // 未対応
    override var onItemClickListener: (User) -> Unit = {}
    override var onItemLongClickListener: (User) -> Boolean = { false }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = RecyclerUserView(context)
        return RecyclerUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is RecyclerUserViewHolder){
            holder.update(users[position])
        }
    }

    override fun getItemCount(): Int {
        return users.count()
    }

    override fun add(user: User) {
        users.add(user)
    }

    override fun insert(index: Int, item: User) {
        users.set(index, item)
        // TODO("not supported") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        users.clear()
    }

    override fun remove(id: Long) {
        for (user in users) {
            if (user.id ==id) users.remove(user)
        }
    }
}
