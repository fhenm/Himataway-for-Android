package com.github.fhenm.himataway.listener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import com.github.fhenm.himataway.model.Row;
import com.github.fhenm.himataway.util.ActionUtil;
import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.fragment.AroundFragment;
import com.github.fhenm.himataway.fragment.TalkFragment;
import com.github.fhenm.himataway.settings.BasicSettings;
import twitter4j.Status;

public class StatusLongClickListener implements AdapterView.OnItemLongClickListener {

    private FragmentActivity mActivity;

    public StatusLongClickListener(Activity activity) {
        mActivity = (FragmentActivity) activity;
    }

    public TwitterAdapter getAdapter(AdapterView<?> adapterView) {
        return (TwitterAdapter) adapterView.getAdapter();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        TwitterAdapter twitterAdapter = getAdapter(adapterView);
        Row row = twitterAdapter.getItem(position);
        return handleRow(mActivity, row);
    }

    public static boolean handleRow(FragmentActivity activity, Row row) {
        if (row == null) {
            return false;
        }
        if (row.isDirectMessage()) {
            return false;
        }

        Status status = row.getStatus();
        final Status retweet = status.getRetweetedStatus();
        final Status source = retweet != null ? retweet : status;

        Bundle args = new Bundle();
        String action = BasicSettings.getLongTapAction();
        if (action.equals("quote")) {
            ActionUtil.doQuote(source, activity);
        } else if (action.equals("talk")) {
            if (source.getInReplyToStatusId() > 0) {
                TalkFragment dialog = new TalkFragment();
                args.putSerializable("status", source);
                dialog.setArguments(args);
                dialog.show(activity.getSupportFragmentManager(), "dialog");
            } else {
                return false;
            }
        } else if (action.equals("show_around")) {
            AroundFragment aroundFragment = new AroundFragment();
            Bundle aroundArgs = new Bundle();
            aroundArgs.putSerializable("status", source);
            aroundFragment.setArguments(aroundArgs);
            aroundFragment.show(activity.getSupportFragmentManager(), "dialog");
        } else if (action.equals("share_url")) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "https://twitter.com/" + status.getUser().getScreenName()
                    + "/status/" + String.valueOf(status.getId()));
            activity.startActivity(intent);
        } else if (action.equals("reply_all")) {
            ActionUtil.doReplyAll(source, activity);
        } else {
            return false;
        }
        return true;
    }
}
