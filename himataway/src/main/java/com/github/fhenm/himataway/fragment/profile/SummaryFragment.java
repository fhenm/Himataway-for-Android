package com.github.fhenm.himataway.fragment.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.fhenm.himataway.EditProfileActivity;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.ScaleImageActivity;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.task.DestroyBlockTask;
import com.github.fhenm.himataway.task.DestroyFriendshipTask;
import com.github.fhenm.himataway.task.FollowTask;
import com.github.fhenm.himataway.util.ImageUtil;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.Relationship;
import twitter4j.User;

/**
 * プロフィール上部の左面
 */
public class SummaryFragment extends Fragment {

    private boolean mFollowFlg;
    private boolean mBlocking;
    private boolean mRuntimeFlg;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_profile_summary, container, false);
        if (v == null) {
            return null;
        }

        final User user = (User) getArguments().getSerializable("user");
        final Relationship relationship = (Relationship) getArguments().getSerializable("relationship");
        if (user == null || relationship == null) {
            return null;
        }

        mFollowFlg = relationship.isSourceFollowingTarget();
        mBlocking = relationship.isSourceBlockingTarget();

        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        TextView name = (TextView) v.findViewById(R.id.name);
        TextView screenName = (TextView) v.findViewById(R.id.screen_name);
        TextView followedBy = (TextView) v.findViewById(R.id.followed_by);
        final TextView follow = (TextView) v.findViewById(R.id.follow);
        TextView lock = (TextView) v.findViewById(R.id.lock);
        lock.setVisibility(View.GONE);

        String iconUrl = user.getBiggerProfileImageURL();
        ImageUtil.displayRoundedImage(iconUrl, icon);

        // アイコンタップで拡大
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ScaleImageActivity.class);
                intent.putExtra("url", user.getOriginalProfileImageURL());
                startActivity(intent);
            }
        });

        name.setText(user.getName());
        screenName.setText("@" + user.getScreenName());

        if (user.isProtected()) {
            lock.setVisibility(View.VISIBLE);
        }
        if (relationship.isSourceFollowedByTarget()) {
            followedBy.setText(R.string.label_followed_by_target);
        } else {
            followedBy.setText("");
        }

        follow.setVisibility(View.VISIBLE);
        if (user.getId() == AccessTokenManager.getUserId()) {
            follow.setText(R.string.button_edit_profile);
        } else if (mFollowFlg) {
            follow.setText(R.string.button_unfollow);
        } else if (mBlocking) {
            follow.setText(R.string.button_blocking);
        } else {
            follow.setText(R.string.button_follow);
        }
        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRuntimeFlg) {
                    return;
                }
                if (user.getId() == AccessTokenManager.getUserId()) {
                    Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                    startActivity(intent);
                } else if (mFollowFlg) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.confirm_unfollow)
                            .setPositiveButton(
                                    R.string.button_unfollow,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mRuntimeFlg = true;
                                            MessageUtil.showProgressDialog(getActivity(), getString(R.string.progress_process));
                                            DestroyFriendshipTask task = new DestroyFriendshipTask() {
                                                @Override
                                                protected void onPostExecute(Boolean success) {
                                                    MessageUtil.dismissProgressDialog();
                                                    if (success) {
                                                        MessageUtil.showToast(R.string.toast_destroy_friendship_success);
                                                        follow.setText(R.string.button_follow);
                                                        mFollowFlg = false;
                                                    } else {
                                                        MessageUtil.showToast(R.string.toast_destroy_friendship_failure);
                                                    }
                                                    mRuntimeFlg = false;
                                                }
                                            };
                                            task.execute(user.getId());
                                        }
                                    }
                            )
                            .setNegativeButton(
                                    R.string.button_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }
                            )
                            .show();
                } else if (mBlocking) {
                    // TODO:
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.confirm_destroy_block)
                            .setPositiveButton(
                                    R.string.button_destroy_block,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mRuntimeFlg = true;
                                            MessageUtil.showProgressDialog(getActivity(), getString(R.string.progress_process));
                                            DestroyBlockTask task = new DestroyBlockTask() {
                                                @Override
                                                protected void onPostExecute(Boolean success) {
                                                    MessageUtil.dismissProgressDialog();
                                                    if (success) {
                                                        MessageUtil.showToast(R.string.toast_destroy_block_success);
                                                        follow.setText(R.string.button_follow);
                                                        mBlocking = false;
                                                    } else {
                                                        MessageUtil.showToast(R.string.toast_destroy_block_failure);
                                                    }
                                                    mRuntimeFlg = false;
                                                }
                                            };
                                            task.execute(user.getId());
                                        }
                                    }
                            )
                            .setNegativeButton(
                                    R.string.button_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }
                            )
                            .show();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.confirm_follow)
                            .setPositiveButton(
                                    R.string.button_follow,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mRuntimeFlg = true;
                                            MessageUtil.showProgressDialog(getActivity(), getString(R.string.progress_process));
                                            FollowTask task = new FollowTask() {
                                                @Override
                                                protected void onPostExecute(Boolean success) {
                                                    MessageUtil.dismissProgressDialog();
                                                    if (success) {
                                                        MessageUtil.showToast(R.string.toast_follow_success);
                                                        follow.setText(R.string.button_unfollow);
                                                        mFollowFlg = true;
                                                    } else {
                                                        MessageUtil.showToast(R.string.toast_follow_failure);
                                                    }
                                                    mRuntimeFlg = false;
                                                }
                                            };
                                            task.execute(user.getId());
                                        }
                                    }
                            )
                            .setNegativeButton(
                                    R.string.button_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }
                            )
                            .show();
                }
            }
        });
        return v;
    }
}
