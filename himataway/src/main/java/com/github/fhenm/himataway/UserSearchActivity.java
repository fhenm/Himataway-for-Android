package com.github.fhenm.himataway;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import com.github.fhenm.himataway.adapter.RecyclerUserAdapter;
import com.github.fhenm.himataway.extensions.RecyclerViewExtensionsKt;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.util.KeyboardUtil;
import com.github.fhenm.himataway.util.MessageUtil;
import com.github.fhenm.himataway.util.ThemeUtil;
import kotlin.Unit;
import com.github.fhenm.himataway.databinding.ActivityUserSearchBinding;
import twitter4j.ResponseList;
import twitter4j.User;

import java.util.ArrayList;

public class UserSearchActivity extends AppCompatActivity {

    private String mSearchWord;
    private int mPage = 1;
    private RecyclerUserAdapter mAdapter;
    private boolean mAutoLoading = false;
    private ActivityUserSearchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_search);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.recyclerView.setVisibility(View.GONE);

        // ユーザをViewに描写するアダプター
        mAdapter = new RecyclerUserAdapter(this, new ArrayList<>());
        binding.recyclerView.setAdapter(mAdapter);

        binding.search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        binding.searchText.setOnKeyListener(new View.OnKeyListener() {
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
            binding.searchText.setText(query);
            binding.search.performClick();
        } else {
            KeyboardUtil.showKeyboard(binding.searchText);
        }

        // 追加読み込み
        RecyclerViewExtensionsKt.addOnPagingListener(binding.recyclerView, () -> {
            additionalReading();
            return Unit.INSTANCE;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    private void additionalReading() {
        if (!mAutoLoading) {
            return;
        }
        binding.guruguru.setVisibility(View.VISIBLE);
        mAutoLoading = false;
        new UserSearchTask().execute(mSearchWord);
    }

    private void search() {
        KeyboardUtil.hideKeyboard(binding.searchText);
        if (binding.searchText.getText() == null) return;
        mAdapter.clear();
        mPage = 1;
        binding.recyclerView.setVisibility(View.GONE);
        binding.guruguru.setVisibility(View.VISIBLE);
        mSearchWord = binding.searchText.getText().toString();
        new UserSearchTask().execute(mSearchWord);
    }

    private class UserSearchTask extends AsyncTask<String, Void, ResponseList<User>> {
        @Override
        protected ResponseList<User> doInBackground(String... params) {
            String query = params[0];
            try {
                return TwitterManager.getTwitter().searchUsers(query, mPage);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<User> users) {
            binding.guruguru.setVisibility(View.GONE);
            if (users == null) {
                MessageUtil.showToast(R.string.toast_load_data_failure);
                return;
            }
            for (User user : users) {
                mAdapter.add(user);
            }
            if (users.size() == 20) {
                mAutoLoading = true;
                mPage++;
            }
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
