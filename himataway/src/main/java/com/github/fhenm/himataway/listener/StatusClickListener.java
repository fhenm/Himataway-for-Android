package com.github.fhenm.himataway.listener;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.fragment.dialog.StatusMenuFragment;

public class StatusClickListener implements AdapterView.OnItemClickListener {

    private FragmentActivity mFragmentActivity;

    public StatusClickListener(FragmentActivity fragmentActivity) {
        mFragmentActivity = fragmentActivity;
    }

    public TwitterAdapter getAdapter(AdapterView<?> adapterView) {
        return (TwitterAdapter) adapterView.getAdapter();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TwitterAdapter twitterAdapter = getAdapter(adapterView);
        StatusMenuFragment.newInstance(twitterAdapter.getItem(i))
                .show(mFragmentActivity.getSupportFragmentManager(), "dialog");
    }
}
