package com.github.fhenm.himataway.repository

import com.github.fhenm.himataway.adapter.MyUserStreamAdapter
import com.github.fhenm.himataway.model.AccessTokenManager
import com.github.fhenm.himataway.model.Profile
import com.github.fhenm.himataway.model.Relationship
import twitter4j.*
import java.util.concurrent.Executors
import kotlin.coroutines.experimental.suspendCoroutine

class TwitterRepository(
        private val twitter: twitter4j.Twitter,
        val userStreamAdapter: MyUserStreamAdapter
) {

    private val twitterExecutor = Executors.newSingleThreadExecutor() // TODO DIする

    val onCreateStatus = userStreamAdapter.onCreateStatus

    /**
     * UserId、またはスクリーン名からプロフィールを読む
     */
    suspend fun loadProfile(userId: Long?, screenName: String?): Profile {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    if (userId == null && screenName == null) {
                        cont.resume(Profile().also { prof ->
                            prof.error = "(userId and screenName are null)"
                        })
                        return@submit
                    }

                    // userId が null でなければ ID から、null なら screenName から User を取得する
                    val user =  userId?.let { id -> twitter.showUser(id) } ?: twitter.showUser(screenName)
                    val relationship = twitter.showFriendship(AccessTokenManager.getUserId(), user.id) // TODO AccessTokenManager を外出しする

                    cont.resume(Profile().also { prof ->
                        prof.relationship = relationship
                        prof.user = user
                    })
                } catch (e: TwitterException) {
                    e.printStackTrace()
                    cont.resume(Profile().also { prof ->
                        prof.error = "(userId:${userId}, code:${e.errorCode})"
                    })
                }
            }
        }
    }

    suspend fun loadFriendList(userId:Long, cursor:Long) : PagableResponseList<User> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.getFriendsList(userId, cursor)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadFollowersList(userId:Long, cursor:Long) : PagableResponseList<User> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.getFollowersList(userId, cursor)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadUserListMemberships(userId:Long, cursor:Long) : PagableResponseList<UserList> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.getUserListMemberships(userId, cursor)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadUserListMembers(userId:Long, cursor:Long) : PagableResponseList<User> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.getUserListMembers(userId, cursor)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadRetweets(statusId:Long) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.getRetweets(statusId)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadMyTimeline(maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getHomeTimeline(paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadMyDirectMessages(maxId:Long, count:Int) : ResponseList<DirectMessage> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getDirectMessages(paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadMyMentionsTimeline(maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getMentionsTimeline(paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadMyFavorites(maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getFavorites(paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadUserListStatuses(userListId:Long, maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getUserListStatuses(userListId, paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadUserTimeline(userId:Long, maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getUserTimeline(userId, paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun loadFavorites(userId:Long, maxId:Long, count:Int) : ResponseList<Status> {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val paging = Paging().also { p ->
                        if (maxId > 0) {
                            p.maxId = maxId - 1
                            p.count = count
                        }
                    }

                    val res = twitter.getFavorites(userId, paging)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun search(keyword:String) : QueryResult {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val query = Query( "${keyword} exclude:retweets")
                    val res = twitter.search(query)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    suspend fun search(query:Query) : QueryResult {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val res = twitter.search(query)
                    cont.resume(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resumeWithException(e)
                }
            }
        }
    }

    /**
     * 指定ユーザーの公式ミュートをOn/Offする（enabled = true なら ON にする）
     */
    suspend fun updateOfficialMuteEnabled(userId: Long, enabled: Boolean) : Boolean {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    if (enabled) {
                        twitter.createMute(userId)
                        Relationship.setOfficialMute(userId)
                    } else {
                        twitter.destroyMute(userId)
                        Relationship.removeOfficialMute(userId)
                    }
                    cont.resume(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(false)
                }
            }
        }
    }

    /**
     * 指定ユーザーの通知とリツイート表示をON/OFFする
     */
    suspend fun updateFriendship(
            userId: Long, enableDeviceNotification : Boolean, enabled: Boolean): Boolean {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    twitter.updateFriendship(userId, enableDeviceNotification, enabled)

                    if (enabled) {
                        Relationship.removeNoRetweet(userId)
                    } else {
                        Relationship.setNoRetweet(userId)
                    }
                    cont.resume(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(false)
                }
            }
        }
    }

    suspend fun updateBlockEnabled(userId: Long, enabled: Boolean): Boolean {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {

                    if (enabled) {
                        twitter.createBlock(userId)
                        Relationship.setBlock(userId)
                    } else {
                        twitter.destroyBlock(userId)
                        Relationship.removeBlock(userId)
                    }
                    cont.resume(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(false)
                }
            }
        }
    }

    /**
     * 指定ユーザーをスパム報告する
     */
    suspend fun reportSpam(userId: Long): Boolean {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    twitter.reportSpam(userId)
                    Relationship.setBlock(userId)

                    cont.resume(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(false)
                }
            }
        }
    }

    /**
     * 指定した Id のツイートを取得する
     */
    suspend fun loadStatus(statusId: Long): Status {
        return suspendCoroutine { cont ->
            twitterExecutor.submit {
                try {
                    val status = twitter.showStatus(statusId)

                    cont.resume(status)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

}