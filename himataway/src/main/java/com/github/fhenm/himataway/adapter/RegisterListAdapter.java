package com.github.fhenm.himataway.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.model.UserListWithRegistered;
import com.github.fhenm.himataway.util.MessageUtil;

public class RegisterListAdapter extends ArrayAdapter<UserListWithRegistered> {

    private LayoutInflater mInflater;
    private int mLayout;
    private long[] mUserId;

    public RegisterListAdapter(Context context, int textViewResourceId, long userId) {
        super(context, textViewResourceId);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = textViewResourceId;
        mUserId = new long[]{userId};
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // ビューを受け取る
        View view = convertView;
        if (view == null) {
            // 受け取ったビューがnullなら新しくビューを生成
            view = mInflater.inflate(this.mLayout, null);
            if (view == null) {
                return null;
            }
        }

        final UserListWithRegistered userListWithRegistered = getItem(position);
        final CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        if (checkbox != null) {
            checkbox.setText(userListWithRegistered.getUserList().getName());
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(userListWithRegistered.isRegistered());
            checkbox.setTag(userListWithRegistered.getUserList().getId());
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b == userListWithRegistered.isRegistered()) {
                        return;
                    }
                    userListWithRegistered.setRegistered(b);
                    MessageUtil.showProgressDialog(getContext(), getContext().getString(R.string.progress_process));
                    if (b) {
                        CreateUserListMembersTask task = new CreateUserListMembersTask() {
                            @Override
                            protected void onPostExecute(Boolean success) {
                                MessageUtil.dismissProgressDialog();
                                if (success) {
                                    MessageUtil.showToast(R.string.toast_add_to_list_success);
                                } else {
                                    MessageUtil.showToast(R.string.toast_add_to_list_failure);
                                    userListWithRegistered.setRegistered(false);
                                    notifyDataSetChanged();
                                }
                            }

                        };
                        task.execute(userListWithRegistered.getUserList().getId());
                    } else {
                        DestroyUserListMembersTask task = new DestroyUserListMembersTask() {

                            @Override
                            protected void onPostExecute(Boolean success) {
                                MessageUtil.dismissProgressDialog();
                                if (success) {
                                    MessageUtil.showToast(R.string.toast_remove_from_list_success);
                                } else {
                                    MessageUtil.showToast(R.string.toast_remove_from_list_failure);
                                    userListWithRegistered.setRegistered(true);
                                    notifyDataSetChanged();
                                }
                            }
                        };
                        task.execute(userListWithRegistered.getUserList().getId());
                    }
                }
            });
        }
        return view;
    }

    private class CreateUserListMembersTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                TwitterManager.getTwitter().createUserListMembers(params[0], mUserId);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private class DestroyUserListMembersTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                TwitterManager.getTwitter().destroyUserListMembers(params[0], mUserId);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
