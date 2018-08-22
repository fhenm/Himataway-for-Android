package com.github.fhenm.himataway.fragment.list;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.listener.StatusClickListener;
import com.github.fhenm.himataway.listener.StatusLongClickListener;
import com.github.fhenm.himataway.model.Row;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.settings.BasicSettings;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;

public class UserListStatusesFragment extends Fragment {

    private TwitterAdapter mAdapter;
    private ListView mListView;
    private long mListId;
    private ProgressBar mFooter;
    private boolean mAutoLoader = false;
    private long mMaxId = 0L;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.list_guruguru, container, false);
        if (v == null) {
            return null;
        }

        mListId = getArguments().getLong("listId");

        // リストビューの設定
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setVisibility(View.GONE);

        mFooter = (ProgressBar) v.findViewById(R.id.guruguru);

        mAdapter = new TwitterAdapter(getActivity(), R.layout.row_tweet);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new StatusClickListener(getActivity()));

        mListView.setOnItemLongClickListener(new StatusLongClickListener(getActivity()));

        new UserListTask().execute(mListId);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 最後までスクロールされたかどうかの判定
                if (totalItemCount == firstVisibleItem + visibleItemCount) {
                    additionalReading();
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StatusActionEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StreamingDestroyStatusEvent event) {
        mAdapter.removeStatus(event.getStatusId());
    }

    private void additionalReading() {
        if (!mAutoLoader) {
            return;
        }
        mFooter.setVisibility(View.VISIBLE);
        mAutoLoader = false;
        new UserListTask().execute(mListId);
    }


    private class UserListTask extends AsyncTask<Long, Void, ResponseList<Status>> {
        @Override
        protected ResponseList<twitter4j.Status> doInBackground(Long... params) {
            try {
                Paging paging = new Paging();
                if (mMaxId > 0) {
                    paging.setMaxId(mMaxId - 1);
                    paging.setCount(BasicSettings.getPageCount());
                }
                return TwitterManager.getTwitter().getUserListStatuses(params[0], paging);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            mFooter.setVisibility(View.GONE);

            if (statuses == null || statuses.size() == 0) {
                return;
            }

            for (twitter4j.Status status : statuses) {
                if (mMaxId == 0L || mMaxId > status.getId()) {
                    mMaxId = status.getId();
                }
                mAdapter.add(Row.newStatus(status));
            }
            mAutoLoader = true;
            mListView.setVisibility(View.VISIBLE);
        }
    }

}
