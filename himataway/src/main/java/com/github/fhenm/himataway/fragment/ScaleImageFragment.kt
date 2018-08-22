package com.github.fhenm.himataway.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nostra13.universalimageloader.core.ImageLoader
import com.github.fhenm.himataway.widget.ScaleImageView

class ScaleImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val activity = activity

        val imageView = ScaleImageView(activity)
        imageView.setActivity(activity)
        val imageUrl = arguments.getString("url")

        ImageLoader.getInstance().displayImage(imageUrl, imageView)

        return imageView
    }
}
