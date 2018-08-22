package com.github.fhenm.himataway;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.event.AlertDialogEvent;
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.listener.StatusClickListener;
import com.github.fhenm.himataway.listener.StatusLongClickListener;
import com.github.fhenm.himataway.model.Row;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.Status;

/**
 * ツイート表示用のアクティビティ
 */
public class StatusActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private TwitterAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // インテント経由での起動をサポート
        Intent intent = getIntent();
        if (intent.getBooleanExtra("notification", false)) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancelAll();
        }
        long statusId;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri == null || uri.getPath() == null) {
                return;
            }
            if (uri.getPath().contains("photo")) {
                Intent scaleImage = new Intent(this, ScaleImageActivity.class);
                scaleImage.putExtra("url", uri.toString());
                startActivity(scaleImage);
                finish();
                return;
            } else if (uri.getPath().contains("video")) {
                Intent videoActivity = new Intent(this, VideoActivity.class);
                videoActivity.putExtra("statusUrl", uri.toString());
                startActivity(videoActivity);
                finish();
                return;
            } else {
                statusId = Long.parseLong(uri.getLastPathSegment());
            }
        } else {
            statusId = intent.getLongExtra("id", -1L);
        }

        setContentView(R.layout.activity_status);

        ListView listView = (ListView) findViewById(R.id.list);

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(listView);

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = new TwitterAdapter(this, R.layout.row_tweet);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new StatusClickListener(this));

        listView.setOnItemLongClickListener(new StatusLongClickListener(this));

        if (statusId > 0) {
            showProgressDialog(getString(R.string.progress_loading));
            new LoadTask().execute(statusId);
        } else {
            Status status = (Status) intent.getSerializableExtra("status");
            if (status != null) {
                mAdapter.add(Row.newStatus(status));
                long inReplyToStatusId = status.getInReplyToStatusId();
                if (inReplyToStatusId > 0) {
                    showProgressDialog(getString(R.string.progress_loading));
                    new LoadTask().execute(inReplyToStatusId);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(AlertDialogEvent event) {
        event.getDialogFragment().show(getSupportFragmentManager(), "dialog");
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StatusActionEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StreamingDestroyStatusEvent event) {
        mAdapter.removeStatus(event.getStatusId());
    }

    private void showProgressDialog(String message) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    private class LoadTask extends AsyncTask<Long, Void, Status> {

        public LoadTask() {
            super();
        }

        @Override
        protected twitter4j.Status doInBackground(Long... params) {
            try {
                return TwitterManager.getTwitter().showStatus(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(twitter4j.Status status) {
            dismissProgressDialog();
            if (status != null) {
                mAdapter.add(Row.newStatus(status));
                mAdapter.notifyDataSetChanged();
                Long inReplyToStatusId = status.getInReplyToStatusId();
                if (inReplyToStatusId > 0) {
                    new LoadTask().execute(inReplyToStatusId);
                }
            } else {
                MessageUtil.showToast(R.string.toast_load_data_failure);
            }
        }
    }
}
