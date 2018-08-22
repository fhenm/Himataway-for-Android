package com.github.fhenm.himataway.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.fhenm.himataway.R
import com.github.fhenm.himataway.databinding.RecyclerRowWordBinding

data class MuteUser (
    val userId: Long,
    val screenName: String
)

class RecyclerMuteUserView constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr){

    /** 削除した時のハンドラ */
    var onItemRemoveListener : (MuteUser)->Unit = {}

    private var binding: RecyclerRowWordBinding

    init {
        binding = RecyclerRowWordBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun update(user: MuteUser){
        binding.word.setText("@" + user.screenName)

        binding.trash.setOnClickListener(OnClickListener {
            AlertDialog.Builder(context)
                    .setMessage(String.format(context.getString(R.string.confirm_destroy_mute), "@" + user.screenName))
                    .setPositiveButton(
                            R.string.button_yes,
                            DialogInterface.OnClickListener { dialog, which ->
                                onItemRemoveListener(user)
                            }
                    )
                    .setNegativeButton(
                            R.string.button_no,
                            DialogInterface.OnClickListener { dialog, which -> }
                    )
                    .show()
        })
    }
}

class RecyclerMuteUserViewHolder(private val view: RecyclerMuteUserView) : RecyclerView.ViewHolder(view) {
    fun update(user : MuteUser) {
        view.update(user)
    }
}

class RecyclerMuteUserAdapter(
        private val context: Context,
        private val users: MutableList<MuteUser>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** 削除した時のハンドラ */
    var onItemRemoveListener : (MuteUser)->Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val view = RecyclerMuteUserView(context).also { v -> v.onItemRemoveListener = onItemRemoveListener }
        return RecyclerMuteUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if(holder is RecyclerMuteUserViewHolder){
            holder.update(users[position])
        }
    }

    override fun getItemCount(): Int {
        return users.count()
    }

    fun add(user: MuteUser) {
        users.add(user)
    }

    fun remove(user: MuteUser) {
        users.remove(user)
    }
}
