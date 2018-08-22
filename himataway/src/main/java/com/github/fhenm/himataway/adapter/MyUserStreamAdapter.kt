package com.github.fhenm.himataway.adapter

import android.os.AsyncTask
import android.os.Handler
import com.jakewharton.rxrelay2.PublishRelay

import de.greenrobot.event.EventBus
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import com.github.fhenm.himataway.event.model.NotificationEvent
import com.github.fhenm.himataway.event.model.StreamingCreateFavoriteEvent
import com.github.fhenm.himataway.event.model.StreamingCreateStatusEvent
import com.github.fhenm.himataway.event.model.StreamingDestroyMessageEvent
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent
import com.github.fhenm.himataway.event.model.StreamingUnFavoriteEvent
import com.github.fhenm.himataway.model.AccessTokenManager
import com.github.fhenm.himataway.model.FavRetweetManager
import com.github.fhenm.himataway.model.Relationship
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.settings.MuteSettings
import twitter4j.DirectMessage
import twitter4j.Status
import twitter4j.StatusDeletionNotice
import twitter4j.User
import twitter4j.UserStreamAdapter

class MyUserStreamAdapter : UserStreamAdapter() {

    private var mStopped: Boolean = false
    private var mPause: Boolean = false
    private val mStreamingCreateStatusEvents = ArrayList<StreamingCreateStatusEvent>()
    private val mStreamingDestroyStatusEvents = ArrayList<StreamingDestroyStatusEvent>()
    private val mStreamingCreateFavoriteEvents = ArrayList<StreamingCreateFavoriteEvent>()
    private val mStreamingUnFavoriteEvents = ArrayList<StreamingUnFavoriteEvent>()
    private val mStreamingDestroyMessageEvents = ArrayList<StreamingDestroyMessageEvent>()

    private val _onCreateStatus = PublishRelay.create<StreamingCreateStatusEvent>()
    val onCreateStatus : Flowable<StreamingCreateStatusEvent> = _onCreateStatus.toFlowable(BackpressureStrategy.LATEST)

    fun stop() {
        mStopped = true
    }

    fun start() {
        mStopped = false
    }

    fun pause() {
        mPause = true
    }

    fun resume() {
        mPause = false
        Handler().post {
            for (event in mStreamingCreateStatusEvents) {
                EventBus.getDefault().post(event)
                _onCreateStatus.accept(event)
            }
            for (event in mStreamingDestroyStatusEvents) {
                EventBus.getDefault().post(event)
            }
            for (event in mStreamingCreateFavoriteEvents) {
                EventBus.getDefault().post(event)
            }
            for (event in mStreamingUnFavoriteEvents) {
                EventBus.getDefault().post(event)
            }
            for (event in mStreamingDestroyMessageEvents) {
                EventBus.getDefault().post(event)
            }
            mStreamingCreateStatusEvents.clear()
            mStreamingDestroyStatusEvents.clear()
            mStreamingCreateFavoriteEvents.clear()
            mStreamingUnFavoriteEvents.clear()
            mStreamingDestroyMessageEvents.clear()
        }
    }

    override fun onStatus(status: Status?) {
        if (mStopped) {
            return
        }
        if (!Relationship.isVisible(status)) {
            return
        }
        val row = Row.newStatus(status)
        if (MuteSettings.isMute(row)) {
            return
        }
        val userId = AccessTokenManager.getUserId()
        val retweetedStatus = status!!.retweetedStatus
        if (status.inReplyToUserId == userId || retweetedStatus != null && retweetedStatus.user.id == userId) {
            EventBus.getDefault().post(NotificationEvent(row))
        }
        if (mPause) {
            mStreamingCreateStatusEvents.add(StreamingCreateStatusEvent(row))
        } else {
            EventBus.getDefault().post(StreamingCreateStatusEvent(row))
            _onCreateStatus.accept(StreamingCreateStatusEvent(row))
        }
    }

    override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice?) {
        if (mStopped) {
            return
        }
        if (mPause) {
            mStreamingDestroyStatusEvents.add(StreamingDestroyStatusEvent(statusDeletionNotice!!.statusId))
        } else {
            EventBus.getDefault().post(StreamingDestroyStatusEvent(statusDeletionNotice!!.statusId))
        }
    }

    override fun onFavorite(source: User?, target: User?, status: Status?) {
        if (mStopped) {
            return
        }
        val row = Row.newFavorite(source, target, status)
        // 自分の fav を反映
        if (source!!.id == AccessTokenManager.getUserId()) {
            FavRetweetManager.setFav(status!!.id)
            EventBus.getDefault().post(StreamingCreateFavoriteEvent(row))
            return
        }
        EventBus.getDefault().post(NotificationEvent(row))
        object : AsyncTask<Row, Void, twitter4j.Status>() {
            private var mRow: Row? = null
            override fun doInBackground(vararg params: Row): twitter4j.Status? {
                mRow = params[0]
                try {
                    return TwitterManager.getTwitter().showStatus(mRow!!.status.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }

            }

            override fun onPostExecute(status: twitter4j.Status?) {
                if (status != null) {
                    mRow!!.status = status
                }
                if (mPause) {
                    mStreamingCreateFavoriteEvents.add(StreamingCreateFavoriteEvent(mRow))
                } else {
                    EventBus.getDefault().post(StreamingCreateFavoriteEvent(mRow))
                }
            }
        }.execute(row)
    }

    override fun onUnfavorite(arg0: User?, arg1: User?, arg2: Status?) {
        if (mStopped) {
            return
        }
        // 自分の unfav を反映
        if (arg0!!.id == AccessTokenManager.getUserId()) {
            FavRetweetManager.removeFav(arg2!!.id)
        }
        if (mPause) {
            mStreamingUnFavoriteEvents.add(StreamingUnFavoriteEvent(arg0, arg2))
        } else {
            EventBus.getDefault().post(StreamingUnFavoriteEvent(arg0, arg2))
        }
    }

    override fun onDirectMessage(directMessage: DirectMessage?) {
        if (mStopped) {
            return
        }
        val row = Row.newDirectMessage(directMessage)
        if (MuteSettings.isMute(row)) {
            return
        }
        EventBus.getDefault().post(NotificationEvent(row))
        if (mPause) {
            mStreamingCreateStatusEvents.add(StreamingCreateStatusEvent(row))
        } else {
            EventBus.getDefault().post(StreamingCreateStatusEvent(row))
        }
    }

    override fun onDeletionNotice(directMessageId: Long, userId: Long) {
        if (mStopped) {
            return
        }
        if (mPause) {
            mStreamingDestroyMessageEvents.add(StreamingDestroyMessageEvent(directMessageId))
        } else {
            EventBus.getDefault().post(StreamingDestroyMessageEvent(directMessageId))
        }
    }
}
