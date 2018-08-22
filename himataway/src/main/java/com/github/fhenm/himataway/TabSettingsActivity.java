package com.github.fhenm.himataway;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.BindView;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.model.TabManager;
import com.github.fhenm.himataway.util.ThemeUtil;
import com.github.fhenm.himataway.widget.FontelloTextView;

public class TabSettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CHOOSE_USER_LIST = 100;
    private static final int HIGH_LIGHT_COLOR = Color.parseColor("#9933b5e5");
    private static final int DEFAULT_COLOR = Color.TRANSPARENT;

    private TabAdapter mAdapter;
    private ListView mListView;
    private TabManager.Tab mDragTab;
    private boolean mSortable = false;
    private int mToPosition;
    private boolean mRemoveMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_tab_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView) findViewById(R.id.list);

        mAdapter = new TabAdapter(this, R.layout.row_tag);
        for (TabManager.Tab tab : TabManager.loadTabs()) {
            mAdapter.add(tab);
        }

        mListView.setAdapter(mAdapter);
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!mSortable) {
                    return false;
                }
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int position = mListView.pointToPosition((int) motionEvent.getX(), (int) motionEvent.getY());
                        if (position < 0) {
                            break;
                        }
                        if (position != mToPosition) {
                            mToPosition = position;
                            mAdapter.remove(mDragTab);
                            mAdapter.insert(mDragTab, mToPosition);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        mAdapter.setCurrentTab(null);
                        mSortable = false;
                        return true;
                    }
                }
                return false;
            }
        });

        Switch modeSwitch = (Switch) findViewById(R.id.mode_switch);
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mRemoveMode = b;
                mAdapter.notifyDataSetChanged();
            }
        });

        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabManager.saveTabs(mAdapter.getTabs());
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    public void startDrag(TabManager.Tab tab) {
        mDragTab = tab;
        mToPosition = 0;
        mSortable = true;
        mAdapter.setCurrentTab(mDragTab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tab_setting, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem homeMenuItem = menu.findItem(R.id.menu_add_home_tab);
        if (homeMenuItem != null) {
            homeMenuItem.setVisible(!mAdapter.hasTabId(TabManager.TIMELINE_TAB_ID));
        }
        MenuItem interactionsMenuItem = menu.findItem(R.id.menu_add_interactions_tab);
        if (interactionsMenuItem != null) {
            interactionsMenuItem.setVisible(!mAdapter.hasTabId(TabManager.INTERACTIONS_TAB_ID));
        }
        MenuItem directMessagesMenuItem = menu.findItem(R.id.menu_add_direct_messages_tab);
        if (directMessagesMenuItem != null) {
            directMessagesMenuItem.setVisible(!mAdapter.hasTabId(TabManager.DIRECT_MESSAGES_TAB_ID));
        }
        MenuItem favoritesMenuItem = menu.findItem(R.id.menu_add_favorites_tab);
        if (favoritesMenuItem != null) {
            favoritesMenuItem.setVisible(!mAdapter.hasTabId(TabManager.FAVORITES_TAB_ID));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_add_home_tab:
                mAdapter.insert(new TabManager.Tab(TabManager.TIMELINE_TAB_ID), 0);
                break;
            case R.id.menu_add_interactions_tab:
                mAdapter.insert(new TabManager.Tab(TabManager.INTERACTIONS_TAB_ID), 0);
                break;
            case R.id.menu_add_direct_messages_tab:
                mAdapter.insert(new TabManager.Tab(TabManager.DIRECT_MESSAGES_TAB_ID), 0);
                break;
            case R.id.menu_add_favorites_tab:
                mAdapter.insert(new TabManager.Tab(TabManager.FAVORITES_TAB_ID), 0);
                break;
            case R.id.menu_user_list_tab:
                TabManager.saveTabs(mAdapter.getTabs());
                Intent intent = new Intent(this, ChooseUserListsActivity.class);
                setResult(RESULT_OK);
                startActivityForResult(intent, REQUEST_CHOOSE_USER_LIST);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHOOSE_USER_LIST:
                if (resultCode == RESULT_OK) {
                    mAdapter.clear();
                    for (TabManager.Tab tab : TabManager.loadTabs()) {
                        mAdapter.add(tab);
                    }
                    mAdapter.notifyDataSetChanged();
                    mListView.invalidateViews();
                    setResult(RESULT_OK);
                }
                break;

        }
    }

    public class TabAdapter extends ArrayAdapter<TabManager.Tab> {

        class ViewHolder {
            @BindView(R.id.handle) FontelloTextView mHandle;
            @BindView(R.id.tab_icon) FontelloTextView mTabIcon;
            @BindView(R.id.name) TextView mName;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }

        private ArrayList<TabManager.Tab> mTabs = new ArrayList<>();
        private LayoutInflater mInflater;
        private int mLayout;
        private TabManager.Tab mCurrentTab;

        public TabAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayout = textViewResourceId;
        }

        public void setCurrentTab(TabManager.Tab tab) {
            mCurrentTab = tab;
            notifyDataSetChanged();
        }

        public ArrayList<TabManager.Tab> getTabs() {
            return mTabs;
        }

        @Override
        public void add(TabManager.Tab tab) {
            super.add(tab);
            mTabs.add(tab);
        }

        @Override
        public void insert(TabManager.Tab tab, int position) {
            super.insert(tab, position);
            mTabs.add(position, tab);
        }

        @Override
        public void remove(TabManager.Tab tab) {
            super.remove(tab);
            mTabs.remove(tab);
        }

        @Override
        public void clear() {
            super.clear();
            mTabs.clear();
        }

        public boolean hasTabId(Long tabId) {
            for (TabManager.Tab tab : mTabs) {
                if (tab.id.equals(tabId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
                if (view == null) {
                    return null;
                }
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final TabManager.Tab tab = mTabs.get(position);

            viewHolder.mTabIcon.setText(tab.getIcon());
            viewHolder.mName.setText(tab.getName());

            if (mRemoveMode) {
                viewHolder.mHandle.setText(R.string.fontello_trash);
                viewHolder.mHandle.setOnTouchListener(null);
                viewHolder.mHandle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mAdapter.remove(tab);
                    }
                });
            } else {
                viewHolder.mHandle.setText(R.string.fontello_menu);
                viewHolder.mHandle.setOnClickListener(null);
                viewHolder.mHandle.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            startDrag(tab);
                            return true;
                        }
                        return false;
                    }
                });
            }

            if (mCurrentTab != null && mCurrentTab == tab) {
                view.setBackgroundColor(HIGH_LIGHT_COLOR);
            } else {
                view.setBackgroundColor(DEFAULT_COLOR);
            }

            return view;
        }
    }
}
