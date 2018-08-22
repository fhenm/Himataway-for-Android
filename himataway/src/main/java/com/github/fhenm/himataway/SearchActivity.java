package com.github.fhenm.himataway;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.github.fhenm.himataway.extensions.ActivityExtensionsKt;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.TwitterAdapter;
import com.github.fhenm.himataway.databinding.ActivitySearchBinding;
import com.github.fhenm.himataway.event.AlertDialogEvent;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent;
import com.github.fhenm.himataway.listener.StatusClickListener;
import com.github.fhenm.himataway.listener.StatusLongClickListener;
import com.github.fhenm.himataway.model.Row;
import com.github.fhenm.himataway.model.TabManager;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.task.AbstractAsyncTaskLoader;
import com.github.fhenm.himataway.util.KeyboardUtil;
import com.github.fhenm.himataway.util.MessageUtil;
import com.github.fhenm.himataway.util.ThemeUtil;
import com.github.fhenm.himataway.viewmodel.SearchActivityViewModel;
import com.github.fhenm.himataway.widget.ClearEditText;
import com.github.fhenm.himataway.widget.FontelloButton;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.SavedSearch;

public class SearchActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<QueryResult> {


    private ClearEditText mSearchWords;
    private FontelloButton mSearchButton;
    private ListView mSearchList;
    private ProgressBar mGuruguru;

    private TwitterAdapter mAdapter;
    private Query mNextQuery;
    public static int RESULT_CREATE_SAVED_SEARCH = 100;
    private SearchActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
//        setContentView(R.layout.activity_search);

        final ActivitySearchBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        viewModel = ViewModelProviders
                .of(this, new SearchActivityViewModel.Factory(
                        ActivityExtensionsKt.getTwitterRepo(this)
                ))
                .get(SearchActivityViewModel.class);

//        binding.viewModel = viewModel;

        mSearchWords = binding.searchWords;
        mSearchButton = binding.searchButton;
        mSearchList = binding.searchList;
        mGuruguru = binding.guruguru;

        binding.tweetButton.setOnClickListener(v -> {
            onClickTweetButton();
        });

        binding.searchButton.setOnClickListener(v -> {
            search();
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = new TwitterAdapter(this, R.layout.row_tweet);
        mSearchList.setAdapter(mAdapter);

        mSearchList.setOnItemClickListener(new StatusClickListener(this));
        mSearchList.setOnItemLongClickListener(new StatusLongClickListener(this));

        mSearchWords.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //EnterKeyが押されたかを判定
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    search();
                    return true;
                }
                return false;
            }
        });

        Intent intent = getIntent();
        String query = intent.getStringExtra("query");
        if (query != null) {
            mSearchWords.setText(query);
            mSearchButton.performClick();
        } else {
            KeyboardUtil.showKeyboard(mSearchWords);
        }

        mSearchList.setOnScrollListener(new AbsListView.OnScrollListener() {

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
    }

//    @OnClick(R.id.search_button)
//    void onClickSearchButton() {
//        search();
//    }

//    @OnClick(R.id.tweet_button)
    void onClickTweetButton() {
        if (mSearchWords.getText() == null) return;
        Intent intent = new Intent(this, PostActivity.class);
        intent.putExtra("status", " ".concat(mSearchWords.getText().toString()));
        startActivity(intent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.save_search:
                if (mSearchWords.getText() != null) {
                    new CreateSavedSearchTask().execute(mSearchWords.getText().toString());
                }
                break;
            case R.id.search_to_tab:
                ArrayList<TabManager.Tab> tabs = TabManager.loadTabs();
                String searchWord = mSearchWords.getText().toString();
                TabManager.Tab tab = new TabManager.Tab(TabManager.SEARCH_TAB_ID - Math.abs(searchWord.hashCode()));
                tab.name = searchWord;
                tabs.add(tab);
                TabManager.saveTabs(tabs);
                setResult(RESULT_OK);
                break;
        }
        return true;
    }

    private void additionalReading() {
        if (mNextQuery != null) {
            mGuruguru.setVisibility(View.VISIBLE);

            Bundle args = new Bundle(1);
            args.putSerializable("query", mNextQuery);
            getSupportLoaderManager().restartLoader(0, args, this);
            mNextQuery = null;
        }
    }

    private void search() {
        KeyboardUtil.hideKeyboard(mSearchWords);
        if (mSearchWords.getText() == null) return;
        mAdapter.clear();
        mSearchList.setVisibility(View.GONE);
        mGuruguru.setVisibility(View.VISIBLE);
        mNextQuery = null;

        Query query = new Query(mSearchWords.getText().toString().concat(" exclude:retweets"));

        Bundle args = new Bundle(1);
        args.putSerializable("query", query);
        getSupportLoaderManager().restartLoader(0, args, this).forceLoad();
    }

    @Override
    public Loader<QueryResult> onCreateLoader(int id, Bundle args) {
        Query query = (Query) args.getSerializable("query");
        return new SearchLoader(this, query);
    }

    @Override
    public void onLoadFinished(Loader<QueryResult> loader, QueryResult queryResult) {
        if (queryResult == null) {
            MessageUtil.showToast(R.string.toast_load_data_failure);
            return;
        }
        if (queryResult.hasNext()) {
            mNextQuery = queryResult.nextQuery();
        }
        int count = mAdapter.getCount();
        List<twitter4j.Status> statuses = queryResult.getTweets();
        for (twitter4j.Status status : statuses) {
            mAdapter.add(Row.newStatus(status));
        }

        mSearchList.setVisibility(View.VISIBLE);
        if (count == 0) {
            mSearchList.setSelection(0);
        }
        mGuruguru.setVisibility(View.GONE);

        // インテント経由で検索時にうまく閉じてくれないので入れている
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mSearchWords.getWindowToken(), 0);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<QueryResult> loader) {

    }

    public static class SearchLoader extends AbstractAsyncTaskLoader<QueryResult> {

        private Query mQuery;

        public SearchLoader(Context context, Query query) {
            super(context);
            mQuery = query;
        }

        @Override
        public QueryResult loadInBackground() {
            try {
                return TwitterManager.getTwitter().search(mQuery);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class CreateSavedSearchTask extends AsyncTask<String, Void, SavedSearch> {
        @Override
        protected SavedSearch doInBackground(String... params) {
            String query = params[0];
            try {
                return TwitterManager.getTwitter().createSavedSearch(query);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(SavedSearch savedSearch) {
            if (savedSearch == null) {
                return;
            }
            setResult(RESULT_CREATE_SAVED_SEARCH);
            MessageUtil.showToast(getString(R.string.toast_save_success));
        }
    }
}
