package com.github.fhenm.himataway.fragment;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LongSparseArray;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;

import com.google.common.primitives.Longs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent;
import com.github.fhenm.himataway.listener.HeaderStatusClickListener;
import com.github.fhenm.himataway.listener.HeaderStatusLongClickListener;
import com.github.fhenm.himataway.model.Row;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.settings.BasicSettings;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;

/**
 * 会話を表示
 *
 * @author aska
 */
public class TalkFragment extends DialogFragment {

    private Twitter mTwitter;
    private TwitterAdapter mAdapter;
    private ListView mListView;
    private View mHeaderView;
    private View mFooterView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        FragmentActivity activity = getActivity();
        Dialog dialog = new Dialog(activity);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        dialog.setContentView(R.layout.list_talk);

        final Status status = (Status) getArguments().getSerializable("status");
        if (status == null) {
            return dialog;
        }

        mListView = (ListView) dialog.findViewById(R.id.list);

        mHeaderView = new View(activity);
        mFooterView = new View(activity);

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int inReplyToAreaPixels = status.getInReplyToStatusId() > 0 ? metrics.heightPixels : 0;

        if (BasicSettings.getTalkOrderNewest()) {
            mHeaderView.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT, 100));
            mFooterView.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT, inReplyToAreaPixels));
        } else {
            mHeaderView.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT, inReplyToAreaPixels));
            mFooterView.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT, 100));
        }

        mListView.addHeaderView(mHeaderView, null, false);
        mListView.addFooterView(mFooterView, null, false);

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = new TwitterAdapter(activity, R.layout.row_tweet);

        mListView.setAdapter(mAdapter);

        mListView.setOnScrollListener(mOnScrollListener);

        // タップ操作の登録など
        mListView.setOnItemClickListener(new HeaderStatusClickListener(activity));

        mListView.setOnItemLongClickListener(new HeaderStatusLongClickListener(getActivity()));

        mTwitter = TwitterManager.getTwitter();
        mAdapter.add(Row.newStatus(status));

        if (!BasicSettings.getTalkOrderNewest()) {
            mListView.setSelectionFromTop(1, 0);
        }

        if (status.getInReplyToStatusId() > 0) {
            new LoadTalk().execute(status.getInReplyToStatusId());
        } else {
            if (BasicSettings.getTalkOrderNewest()) {
                dialog.findViewById(R.id.guruguru_footer).setVisibility(View.GONE);
            } else {
                dialog.findViewById(R.id.guruguru_header).setVisibility(View.GONE);
            }
        }
        new LoadTalkReply().execute(status);

        return dialog;
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

    private AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            switch (scrollState) {
                case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                    int f = mListView.getFirstVisiblePosition();
                    int l = mListView.getLastVisiblePosition();
                    int c = mAdapter.getCount();
                    if (f > 0) {
                        mHeaderView.setLayoutParams(new AbsListView.LayoutParams(
                                AbsListView.LayoutParams.MATCH_PARENT, 0));
                    }
                    if (l <= c) {
                        mFooterView.setLayoutParams(new AbsListView.LayoutParams(
                                AbsListView.LayoutParams.MATCH_PARENT, 0));
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }
    };

    private class LoadTalk extends AsyncTask<Long, Void, twitter4j.Status> {

        public LoadTalk() {
            super();
        }

        @Override
        protected twitter4j.Status doInBackground(Long... params) {
            try {
                return mTwitter.showStatus(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(twitter4j.Status status) {
            Dialog dialog = getDialog();
            if (dialog == null) {
                return;
            }
            if (status != null) {

                if (BasicSettings.getTalkOrderNewest()) {
                    mAdapter.add(Row.newStatus(status));
                } else {
                    // 表示している要素の位置
                    int position = mListView.getLastVisiblePosition();

                    // 縦スクロール位置
                    View view = mListView.getChildAt(position);
                    int y = view != null ? view.getTop() : 0;

                    mAdapter.insert(Row.newStatus(status), 0);

                    mListView.setSelectionFromTop(position + 1, y);

                    if (mListView.getFirstVisiblePosition() > 0) {
                        mHeaderView.setLayoutParams(new AbsListView.LayoutParams(
                                AbsListView.LayoutParams.MATCH_PARENT, 0));
                    }
                }

                Long inReplyToStatusId = status.getInReplyToStatusId();
                if (inReplyToStatusId > 0) {
                    new LoadTalk().execute(inReplyToStatusId);
                } else {
                    if (BasicSettings.getTalkOrderNewest()) {
                        dialog.findViewById(R.id.guruguru_footer).setVisibility(View.GONE);
                    } else {
                        dialog.findViewById(R.id.guruguru_header).setVisibility(View.GONE);
                    }
                }
            } else {
                if (BasicSettings.getTalkOrderNewest()) {
                    dialog.findViewById(R.id.guruguru_footer).setVisibility(View.GONE);
                } else {
                    dialog.findViewById(R.id.guruguru_header).setVisibility(View.GONE);
                }
            }
        }
    }

    private class LoadTalkReply extends AsyncTask<twitter4j.Status, Void, ArrayList<twitter4j.Status>> {

        public LoadTalkReply() {
            super();
        }

        @Override
        protected ArrayList<twitter4j.Status> doInBackground(twitter4j.Status... params) {
            try {
                final twitter4j.Status sourceStatus = params[0];
                final Query toQuery = new Query("to:" + sourceStatus.getUser().getScreenName() + " AND filter:replies");
                toQuery.setCount(200);
                toQuery.setSinceId(sourceStatus.getId());
                toQuery.setResultType(Query.ResultType.recent);
                final QueryResult toResult = mTwitter.search(toQuery);
                final List<twitter4j.Status> searchStatuses = toResult.getTweets();
                if (toResult.hasNext()) {
                    final QueryResult nextResult = mTwitter.search(toResult.nextQuery());
                    searchStatuses.addAll(nextResult.getTweets());
                }

                final Query fromQuery = new Query("from:" + sourceStatus.getUser().getScreenName() + " AND filter:replies");
                fromQuery.setCount(200);
                fromQuery.setSinceId(sourceStatus.getId());
                fromQuery.setResultType(Query.ResultType.recent);
                final QueryResult fromResult = mTwitter.search(fromQuery);
                searchStatuses.addAll(fromResult.getTweets());

                LongSparseArray<Boolean> isLoadMap = new LongSparseArray<>();
                for (final twitter4j.Status status : searchStatuses) {
                    isLoadMap.put(status.getId(), true);
                }

                ArrayList<twitter4j.Status> lookupStatuses = new ArrayList<>();
                final ArrayList<Long> statusIds = new ArrayList<>();
                for (final twitter4j.Status status : searchStatuses) {
                    if (status.getInReplyToStatusId() > 0 && !isLoadMap.get(status.getInReplyToStatusId(), false)) {
                        statusIds.add(status.getInReplyToStatusId());
                        isLoadMap.put(status.getInReplyToStatusId(), true);
                        if (statusIds.size() == 100) {
                            lookupStatuses.addAll(mTwitter.lookup(Longs.toArray(statusIds)));
                            statusIds.clear();
                        }
                    }
                }

                if (statusIds.size() > 0) {
                    lookupStatuses.addAll(mTwitter.lookup(Longs.toArray(statusIds)));
                }

                searchStatuses.addAll(lookupStatuses);

                Collections.sort(searchStatuses, new Comparator<twitter4j.Status>() {

                    @Override
                    public int compare(twitter4j.Status arg0, twitter4j.Status arg1) {
                        if (arg0.getId() > arg1.getId()) {
                            return 1;
                        } else if (arg0.getId() == arg1.getId()) {
                            return 0;
                        } else {
                            return -1;
                        }
                    }
                });
                LongSparseArray<Boolean> isReplyMap = new LongSparseArray<>();
                isReplyMap.put(sourceStatus.getId(), true);
                ArrayList<twitter4j.Status> statuses = new ArrayList<>();
                for (final twitter4j.Status status : searchStatuses) {
                    if (isReplyMap.get(status.getInReplyToStatusId(), false)) {
                        statuses.add(status);
                        isReplyMap.put(status.getId(), true);
                    }
                }
                return statuses;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<twitter4j.Status> statuses) {
            Dialog dialog = getDialog();
            if (dialog == null) {
                return;
            }
            if (BasicSettings.getTalkOrderNewest()) {
                dialog.findViewById(R.id.guruguru_header).setVisibility(View.GONE);
            } else {
                dialog.findViewById(R.id.guruguru_footer).setVisibility(View.GONE);
            }
            if (statuses == null || statuses.size() == 0) {
                return;
            }
            if (BasicSettings.getTalkOrderNewest()) {
                // 表示している要素の位置
                int position = mListView.getLastVisiblePosition();

                // 縦スクロール位置
                View view = mListView.getChildAt(position);
                int y = view != null ? view.getTop() : 0;

                for (final twitter4j.Status status : statuses) {
                    mAdapter.insert(Row.newStatus(status), 0);
                }

                mListView.setSelectionFromTop(position + statuses.size(), y);

                if (mListView.getFirstVisiblePosition() > 0) {
                    mHeaderView.setLayoutParams(new AbsListView.LayoutParams(
                            AbsListView.LayoutParams.MATCH_PARENT, 0));
                }
            } else {
                for (final twitter4j.Status status : statuses) {
                    mAdapter.add(Row.newStatus(status));
                }
            }
        }
    }
}
