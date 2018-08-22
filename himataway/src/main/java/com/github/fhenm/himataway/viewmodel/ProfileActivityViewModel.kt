package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Log
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import com.github.fhenm.himataway.R
import com.github.fhenm.himataway.extensions.get
import com.github.fhenm.himataway.model.Profile
import com.github.fhenm.himataway.repository.TwitterRepository

class ProfileActivityViewModel(
        private val twitterRepo: TwitterRepository
) : ViewModel() {
    private val TAG = "ProfileActivityViewModel"

    class Factory(
            private val twitterRepo: TwitterRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileActivityViewModel(twitterRepo) as T
    }

    // Toastの表示メッセージ
    private val _toastRequest = MutableLiveData<Int>()
    val toastRequest : LiveData<Int> = _toastRequest

    // 画面再起動メッセージ(引数は UserId)
    private val _restartRequest = MutableLiveData<Long>()
    val restartRequest : LiveData<Long> = _restartRequest

    // プログレスの表示メッセージ（空文字の場合、プログレスを非表示にする ← TODO わかりづらい）
    private val _progressRequest = MutableLiveData<String>()
    val progressRequest : LiveData<String> = _progressRequest

    // Twitter のプロフィール
    private val _profile = MutableLiveData<Profile>()
    val profile : LiveData<Profile> = _profile

    // 読み込んだ Profile
    private val currentProfile = MutableLiveData<Profile>()

    /** プロフィールを読み込む */
    fun loadProfile(userId: Long?, screenName: String?) {
        launch(UI) {
            val profile = twitterRepo.loadProfile(userId, screenName)

            // エラーでなければ、読み込んだ User をカレントとして設定する
            if (profile.error == null) {
                currentProfile.postValue(profile)
            }

            _profile.postValue(profile)
        }
    }

    /** カレントユーザーの公式ミュートON/OFF */
    fun updateOfficialMute(enabled:Boolean) {
        launch(UI) {
            val userId = currentProfile.get()?.user?.id
            if (userId == null) {
                Log.e(TAG, "updateOfficialMute failed. - currentProfile is invalid.")
                return@launch
            }

            val success = twitterRepo.updateOfficialMuteEnabled(userId, enabled)

            _progressRequest.postValue("")
            if (success) {
                _toastRequest.postValue(when(enabled) {
                    true -> R.string.toast_create_official_mute_success
                    else -> R.string.toast_destroy_official_mute_success
                })
                _restartRequest.postValue(userId)
            } else {
                _toastRequest.postValue(when(enabled){
                    true -> R.string.toast_create_official_mute_failure
                    else -> R.string.toast_destroy_official_mute_failure
                })
            }
        }
    }

    /** カレントユーザーのリツイート表示ON/OFF */
    fun updateFriendshipRetweetEnabled(enabled:Boolean) {
        launch(UI) {
            val userId = currentProfile.value?.user?.id
            val relation = currentProfile.value?.relationship
            if (userId == null || relation == null) {
                Log.e(TAG, "updateFriendshipRetweetEnabled failed. - currentProfile is invalid.")
                return@launch
            }

            val success = twitterRepo.updateFriendship(
                    userId, relation.isSourceNotificationsEnabled, enabled)

            _progressRequest.postValue("")
            if (success) {
                _toastRequest.postValue(when(enabled) {
                    true -> R.string.toast_destroy_no_retweet_success
                    else -> R.string.toast_create_no_retweet_success
                })
                _restartRequest.postValue(userId)
            } else {
                _toastRequest.postValue(when(enabled){
                    true -> R.string.toast_destroy_no_retweet_failure
                    else -> R.string.toast_create_no_retweet_failure
                })
            }
        }
    }

    /** カレントユーザーのブロックON/OFF */
    fun updateBlockEnabled(enabled:Boolean) {
        launch(UI) {
            val userId = currentProfile.get()?.user?.id
            if (userId == null) {
                Log.e(TAG, "updateBlockEnabled failed. - currentProfile is invalid.")
                return@launch
            }

            val success = twitterRepo.updateBlockEnabled(userId, enabled)

            _progressRequest.postValue("")
            if (success) {
                _toastRequest.postValue(when(enabled) {
                    true -> R.string.toast_create_block_success
                    else -> R.string.toast_destroy_block_success
                })
                _restartRequest.postValue(userId)
            } else {
                _toastRequest.postValue(when(enabled){
                    true -> R.string.toast_create_block_failure
                    else -> R.string.toast_destroy_block_failure
                })
            }
        }
    }

    /** カレントユーザースパムとして報告 */
    fun reportSpam() {
        launch(UI) {
            val userId = currentProfile.get()?.user?.id
            if (userId == null) {
                Log.e(TAG, "reportSpam failed. - currentProfile is invalid.")
                return@launch
            }

            val success = twitterRepo.reportSpam(userId)

            _progressRequest.postValue("")
            if (success) {
                _toastRequest.postValue(R.string.toast_report_spam_success)
                _restartRequest.postValue(userId)
            } else {
                _toastRequest.postValue(R.string.toast_report_spam_failure)
            }
        }
    }
}