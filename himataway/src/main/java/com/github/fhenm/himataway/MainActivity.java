package com.github.fhenm.himataway;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.SearchAdapter;
import com.github.fhenm.himataway.adapter.main.AccessTokenAdapter;
import com.github.fhenm.himataway.adapter.main.MainPagerAdapter;
import com.github.fhenm.himataway.event.AlertDialogEvent;
import com.github.fhenm.himataway.event.NewRecordEvent;
import com.github.fhenm.himataway.event.action.AccountChangeEvent;
import com.github.fhenm.himataway.event.action.GoToTopEvent;
import com.github.fhenm.himataway.event.action.OpenEditorEvent;
import com.github.fhenm.himataway.event.action.PostAccountChangeEvent;
import com.github.fhenm.himataway.event.connection.StreamingConnectionEvent;
import com.github.fhenm.himataway.event.settings.BasicSettingsChangeEvent;
import com.github.fhenm.himataway.fragment.common.SupportListInterface;
import com.github.fhenm.himataway.fragment.main.StreamingSwitchDialogFragment;
import com.github.fhenm.himataway.fragment.main.tab.MyDirectMessagesFragment;
import com.github.fhenm.himataway.fragment.main.tab.MyFavoritesFragment;
import com.github.fhenm.himataway.fragment.main.tab.MyInteractionsFragment;
import com.github.fhenm.himataway.fragment.main.tab.MyTimelineFragment;
import com.github.fhenm.himataway.fragment.main.tab.RecyclerSearchFragment;
import com.github.fhenm.himataway.fragment.main.tab.RecyclerUserListFragment;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.model.TabManager;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.model.UserIconManager;
import com.github.fhenm.himataway.settings.BasicSettings;
import com.github.fhenm.himataway.task.SendDirectMessageTask;
import com.github.fhenm.himataway.task.UpdateStatusTask;
import com.github.fhenm.himataway.util.KeyboardUtil;
import com.github.fhenm.himataway.util.MessageUtil;
import com.github.fhenm.himataway.util.ThemeUtil;
import com.github.fhenm.himataway.util.TwitterUtil;
import com.github.fhenm.himataway.widget.AutoCompleteEditText;
import com.github.fhenm.himataway.widget.ClearEditText;
import com.github.fhenm.himataway.widget.FontelloButton;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnLongClick;
import de.greenrobot.event.EventBus;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

@SuppressLint("InflateParams")
@SuppressWarnings("MagicConstant")
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACCOUNT_SETTING = 200;
    private static final int REQUEST_SETTINGS = 300;
    private static final int REQUEST_TAB_SETTINGS = 400;
    private static final int REQUEST_SEARCH = 500;
    private static final int ERROR_CODE_DUPLICATE_STATUS = 187;
    private static final Pattern USER_LIST_PATTERN = Pattern.compile("^(@[a-zA-Z0-9_]+)/(.*)$");
    private MainPagerAdapter mMainPagerAdapter;
    private ViewPager mViewPager;
    private Status mInReplyToStatus;
    private ActionBarDrawerToggle mDrawerToggle;
    private Activity mActivity;
    private AccessTokenAdapter mAccessTokenAdapter;
    private AccessToken mSwitchAccessToken;
    private boolean mFirstBoot = true;
    private SearchAdapter mSearchAdapter;
    private int mDefaultTextColor;
    private int mDisabledTextColor;
    private ActionBarHolder mActionBarHolder;

    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.quick_tweet_layout) LinearLayout mQuickTweetLayout;
    @BindView(R.id.tab_menus) LinearLayout mTabMenus;
    @BindView(R.id.account_list) ListView mDrawerList;
    @BindView(R.id.send_button) TextView mSendButton;
    @BindView(R.id.post_button) Button mPostButton;
    @BindView(R.id.quick_tweet_edit) ClearEditText mQuickTweetEdit;

    /**
     * ButterKnife for ActionBar
     */
    class ActionBarHolder {
        @BindView(R.id.action_bar_title) TextView title;
        @BindView(R.id.action_bar_sub_title) TextView subTitle;
        @BindView(R.id.action_bar_normal_layout) LinearLayout normalLayout;
        @BindView(R.id.action_bar_search_layout) FrameLayout searchLayout;
        @BindView(R.id.action_bar_search_text) AutoCompleteEditText searchText;
        @BindView(R.id.action_bar_search_button) TextView searchButton;
        @BindView(R.id.action_bar_search_cancel) TextView cancelButton;
        @BindView(R.id.action_bar_streaming_button) TextView streamingButton;

        @OnClick(R.id.action_bar_search_button)
        void actionBarSearchButton() {
            startSearch();
        }

        @OnClick(R.id.action_bar_search_cancel)
        void actionBarCancelButton() {
            cancelSearch();
        }

        @OnClick(R.id.action_bar_streaming_button)
        void actionBarToggleStreaming() {
            final boolean turnOn = !BasicSettings.getStreamingMode();
            DialogFragment dialog = StreamingSwitchDialogFragment.newInstance(turnOn);
            dialog.show(getSupportFragmentManager(), "dialog");
        }

        public ActionBarHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeUtil.setTheme(this);
        mActivity = this;

        /**
         * アクセストークンがない場合に認証用のアクティビティを起動する
         */
        if (!AccessTokenManager.hasAccessToken()) {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        /**
         * ビューで使う変数の初期化処理
         */
        mDefaultTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color);
        mDisabledTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color_disabled);

        /**
         * ActionBarの初期化処理
         */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_launcher_36dp);
            actionBar.setDisplayHomeAsUpEnabled(true);

            int options = actionBar.getDisplayOptions();
            if ((options & ActionBar.DISPLAY_SHOW_CUSTOM) == ActionBar.DISPLAY_SHOW_CUSTOM) {
                actionBar.setDisplayShowCustomEnabled(false);
            } else {
                actionBar.setDisplayShowCustomEnabled(true);
                if (actionBar.getCustomView() == null) {
                    actionBar.setCustomView(R.layout.action_bar_main);
                    mActionBarHolder = new ActionBarHolder(actionBar.getCustomView());
                    mSearchAdapter = new SearchAdapter(this, R.layout.row_auto_complete);
                    mActionBarHolder.searchText.setThreshold(0);
                    mActionBarHolder.searchText.setAdapter(mSearchAdapter);
                    mActionBarHolder.searchText.setOnItemClickListener(getActionBarAutoCompleteOnClickListener());
                    mActionBarHolder.searchText.setOnKeyListener(getOnKeyListener());
                }
            }
        }

        /**
         * 本体の初期化処理
         */
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        /**
         * 起動と同時にキーボードが出現するのを抑止、クイックモード時に起きる
         */
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mDrawerLayout.requestFocus();

        /**
         * ナビゲーションドロワーの初期化処理
         */
        mAccessTokenAdapter = new AccessTokenAdapter(this,
                R.layout.row_switch_account,
                ThemeUtil.getThemeTextColor(R.attr.holo_blue),
                ThemeUtil.getThemeTextColor(R.attr.text_color));

        View drawerFooterView = getLayoutInflater().inflate(R.layout.drawer_menu, null, false);
        mDrawerList.addFooterView(drawerFooterView, null, true);
        mDrawerList.setAdapter(mAccessTokenAdapter);
        mDrawerToggle = getActionBarDrawerToggle();
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // タブとかの初期化処理へ続く..
        setup();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (BasicSettings.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAB_SETTINGS:
                if (resultCode == RESULT_OK) {
                    setupTab();
                }
                break;
            case REQUEST_ACCOUNT_SETTING:
                if (resultCode == RESULT_OK) {
                    mSwitchAccessToken = (AccessToken) data.getSerializableExtra("accessToken");
                }
                if (mAccessTokenAdapter != null) {
                    mAccessTokenAdapter.clear();
                    ArrayList<AccessToken> accessTokens = AccessTokenManager.getAccessTokens();
                    if (accessTokens != null) {
                        for (AccessToken accessToken : accessTokens) {
                            mAccessTokenAdapter.add(accessToken);
                        }
                    }
                }
                break;
            case REQUEST_SETTINGS:
                if (resultCode == RESULT_OK) {
                    BasicSettings.init();
                    finish();
                    startActivity(new Intent(this, this.getClass()));
                }
                break;
            case REQUEST_SEARCH:
                if (resultCode == RESULT_OK) {
                    setupTab();
                } else if (resultCode == SearchActivity.RESULT_CREATE_SAVED_SEARCH) {
                    mSearchAdapter.reload();
                }
                cancelSearch();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mFirstBoot) {
            mFirstBoot = false;
            return;
        }

        BasicSettings.init();
        BasicSettings.resetNotification();
        EventBus.getDefault().post(new BasicSettingsChangeEvent());

        // アカウント名表示の設定反映
        int currentPosition = mViewPager.getCurrentItem();
        setTitle(mMainPagerAdapter.getPageTitle(currentPosition));

        if (BasicSettings.getQuickMode()) {
            showQuickPanel();
        } else {
            hideQuickPanel();
        }

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // フォントサイズの変更や他のアクティビティでのfav/RTを反映
//                try {
//                    mMainPagerAdapter.notifyDataSetChanged();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 1000);

        if (mSwitchAccessToken != null) {
            TwitterManager.switchAccessToken(mSwitchAccessToken);
            mSwitchAccessToken = null;
        }
        TwitterManager.resumeStreaming();
        if (TwitterManager.getTwitterStreamConnected()) {
            ThemeUtil.setThemeTextColor(mActionBarHolder.streamingButton, R.attr.holo_green);
        } else {
            if (BasicSettings.getStreamingMode()) {
                ThemeUtil.setThemeTextColor(mActionBarHolder.streamingButton, R.attr.holo_red);
            } else {
                mActionBarHolder.streamingButton.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    protected void onPause() {
        TwitterManager.pauseStreaming();
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 弄らないとアプリをバックボタンで閉じる度にタイムラインが初期化されてしまう（アクティビティがfinishされる）
     * moveTaskToBackはホームボタンを押した時と同じ動き
     */
    @Override
    public void onBackPressed() {
        if (mQuickTweetEdit.getText() != null && mQuickTweetEdit.getText().length() > 0) {
            mQuickTweetEdit.setText("");
            mInReplyToStatus = null;
        } else if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        Intent intent;
        switch (itemId) {
            case android.R.id.home:
                cancelSearch();
                break;
            case R.id.profile:
                intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("userId", AccessTokenManager.getUserId());
                startActivity(intent);
                break;
            case R.id.tab_settings:
                intent = new Intent(this, TabSettingsActivity.class);
                startActivityForResult(intent, REQUEST_TAB_SETTINGS);
                break;
            case R.id.action_bar_search_button:
                intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                break;
            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
                break;
            case R.id.official_website:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.official_website)));
                startActivity(intent);
                break;
            case R.id.feedback:
                EventBus.getDefault().post(new OpenEditorEvent(" #com.github.fhenm.himataway", null, null, null));
                break;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("signalButtonColor", mActionBarHolder.streamingButton.getCurrentTextColor());

        LinearLayout tab_menus = (LinearLayout) findViewById(R.id.tab_menus);
        int count = tab_menus.getChildCount();
        final int tabColors[] = new int[count];
        for (int i = 0; i < count; i++) {
            Button button = (Button) tab_menus.getChildAt(i);
            if (button == null) {
                continue;
            }
            tabColors[i] = button.getCurrentTextColor();
        }

        outState.putIntArray("tabColors", tabColors);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mActionBarHolder.streamingButton.setTextColor(savedInstanceState.getInt("signalButtonColor"));

        final int[] tabColors = savedInstanceState.getIntArray("tabColors");
        assert tabColors != null;
        LinearLayout tab_menus = (LinearLayout) findViewById(R.id.tab_menus);
        int count = Math.min(tab_menus.getChildCount(), tabColors.length);
        for (int i = 0; i < count; i++) {
            Button button = (Button) tab_menus.getChildAt(i);
            if (button == null) {
                continue;
            }
            button.setTextColor(tabColors[i]);
        }
    }

    /**
     * ActionBarでCustomView使ってるので自分で再実装
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void setTitle(CharSequence title) {
        if (mActionBarHolder.title != null) {
            Matcher matcher = USER_LIST_PATTERN.matcher(title);
            if (matcher.find()) {
                mActionBarHolder.title.setText(matcher.group(2));
                mActionBarHolder.subTitle.setText(matcher.group(1));
            } else {
                mActionBarHolder.title.setText(title);
                BasicSettings.DisplayAccountName displayAccountName = BasicSettings.getDisplayAccountName();
                switch (displayAccountName) {
                    case SCREEN_NAME:
                        mActionBarHolder.subTitle.setText("@" + AccessTokenManager.getScreenName());
                        break;
                    case DISPLAY_NAME:
                        mActionBarHolder.subTitle.setText(UserIconManager.getName(AccessTokenManager.getUserId()));
                        break;
                    case NONE:
                        mActionBarHolder.subTitle.setText("");
                        break;
                }
            }
        }
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

    public void showQuickPanel() {
        mQuickTweetLayout.setVisibility(View.VISIBLE);
        mQuickTweetEdit.setFocusable(true);
        mQuickTweetEdit.setFocusableInTouchMode(true);
        mQuickTweetEdit.setEnabled(true);
        BasicSettings.setQuickMod(true);
    }

    public void hideQuickPanel() {
        mQuickTweetEdit.setFocusable(false);
        mQuickTweetEdit.setFocusableInTouchMode(false);
        mQuickTweetEdit.setEnabled(false);
        mQuickTweetEdit.clearFocus();
        mQuickTweetLayout.setVisibility(View.GONE);
        mInReplyToStatus = null;
        BasicSettings.setQuickMod(false);
    }

    public void setupTab() {
        ArrayList<TabManager.Tab> tabs = TabManager.loadTabs();
        if (tabs.size() > 0) {
            TypedValue outValueTextColor = new TypedValue();
            TypedValue outValueBackground = new TypedValue();
            Resources.Theme theme = getTheme();
            if (theme != null) {
                theme.resolveAttribute(R.attr.menu_text_color, outValueTextColor, true);
                theme.resolveAttribute(R.attr.button_stateful, outValueBackground, true);
            }
            mTabMenus.removeAllViews();
            mMainPagerAdapter.clearTab();

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    (int) (60 * getResources().getDisplayMetrics().density + 0.5f),
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            int position = 0;
            for (TabManager.Tab tab : tabs) {
                Button button = new FontelloButton(this);
                button.setLayoutParams(layoutParams);
                button.setText(tab.getIcon());
                button.setTextSize(22);
                button.setTextColor(outValueTextColor.data);
                button.setBackgroundResource(outValueBackground.resourceId);
                button.setTag(position++);
                button.setOnClickListener(mMenuOnClickListener);
                button.setOnLongClickListener(mMenuOnLongClickListener);
                mTabMenus.addView(button);
                if (tab.id == TabManager.TIMELINE_TAB_ID) {
                    final Bundle args = new Bundle();
                    args.putBoolean("load", true);
                    mMainPagerAdapter.addTab(MyTimelineFragment.class, args, tab.getName(), tab.id);
                } else if (tab.id == TabManager.INTERACTIONS_TAB_ID) {
                    mMainPagerAdapter.addTab(MyInteractionsFragment.class, null, tab.getName(), tab.id);
                } else if (tab.id == TabManager.DIRECT_MESSAGES_TAB_ID) {
                    mMainPagerAdapter.addTab(MyDirectMessagesFragment.class, null, tab.getName(), tab.id);
                } else if (tab.id == TabManager.FAVORITES_TAB_ID) {
                    mMainPagerAdapter.addTab(MyFavoritesFragment.class, null, tab.getName(), tab.id);
                } else if (tab.id <= TabManager.SEARCH_TAB_ID) {
                    Bundle args = new Bundle();
                    args.putString("searchWord", tab.name);
                    mMainPagerAdapter.addTab(RecyclerSearchFragment.class, args, tab.getName(), tab.id, tab.name);
                } else {
                    Bundle args = new Bundle();
                    args.putLong("userListId", tab.id);
                    mMainPagerAdapter.addTab(RecyclerUserListFragment.class, args, tab.getName(), tab.id);
                }
            }
            mMainPagerAdapter.notifyDataSetChanged();

            /**
             * 起動時やタブ設定後にちゃんとタイトルとボタンのフォーカスを合わせる
             */
            int currentPosition = mViewPager.getCurrentItem();
            Button button = (Button) mTabMenus.getChildAt(currentPosition);
            if (button != null) {
                button.setSelected(true);
            }
            setTitle(mMainPagerAdapter.getPageTitle(currentPosition));
        }
    }

    /**
     * メニューをタップしたらページ移動（見ているページのメニューだったら一番上へスクロール）
     */
    private View.OnClickListener mMenuOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            SupportListInterface f = (SupportListInterface)mMainPagerAdapter.findFragmentByPosition(position);
            if (f == null) {
                return;
            }
            int id = mViewPager.getCurrentItem();
            if (id != position) {
                mViewPager.setCurrentItem(position); // 自動スワイプ
                if (f.isTop()) {
                    showTopView(); // 移動先のページが先頭のツイートを表示していたらボタン色を白に
                }
            } else {
                if (f.goToTop()) {
                    showTopView();
                }
            }
        }
    };

    /**
     * メニューをロングタップしたらリロード
     */
    private View.OnLongClickListener mMenuOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            int position = (Integer) view.getTag();
            SupportListInterface f = (SupportListInterface)mMainPagerAdapter.findFragmentByPosition(position);
            if (f == null) {
                return false;
            }
            f.reload();
            return true;
        }
    };

    private void setup() {

        /**
         * スワイプで動かせるタブを実装するのに最低限必要な実装
         */
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mMainPagerAdapter = new MainPagerAdapter(this, mViewPager);

        setupTab();

        findViewById(R.id.footer).setVisibility(View.VISIBLE);

        /**
         * タブは前後タブまでは状態が保持されるがそれ以上離れるとViewが破棄されてしまう、
         * あまりに使いづらいの上限を増やしている、指定値＋前後のタブまでが保持されるようになる
         * デフォルト値は1（表示しているタブの前後までしか保持されない）
         */
        mViewPager.setOffscreenPageLimit(10);

        /**
         * スワイプ移動でも移動先が未読アプしている場合、アピ解除判定を行う
         */
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                SupportListInterface f = (SupportListInterface)mMainPagerAdapter.findFragmentByPosition(position);
                f.firstLoad();
                if (f.isTop()) {
                    showTopView();
                }
                LinearLayout tab_menus = (LinearLayout) findViewById(R.id.tab_menus);
                int count = tab_menus.getChildCount();
                for (int i = 0; i < count; i++) {
                    Button button = (Button) tab_menus.getChildAt(i);
                    if (button == null) {
                        continue;
                    }
                    if (i == position) {
                        button.setSelected(true);
                    } else {
                        button.setSelected(false);
                    }
                }
                setTitle(mMainPagerAdapter.getPageTitle(position));
            }
        });

        /**
         * これはバターナイフで設定できなかった
         */
        mQuickTweetEdit.addTextChangedListener(mQuickTweetTextWatcher);

        if (BasicSettings.getQuickMode()) {
            showQuickPanel();
        }

        if (BasicSettings.getStreamingMode()) {
            TwitterManager.startStreaming();
        }
    }

    /**
     * 新しいレコードを見たアピ
     */
    public void showTopView() {
        LinearLayout tab_menus = (LinearLayout) findViewById(R.id.tab_menus);
        Button button = (Button) tab_menus.getChildAt(mViewPager.getCurrentItem());
        if (button != null) {
            ThemeUtil.setThemeTextColor(button, R.attr.menu_text_color);
        }
    }

    private void startSearch() {
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mActionBarHolder.normalLayout.setVisibility(View.GONE);
        mActionBarHolder.searchLayout.setVisibility(View.VISIBLE);
        mActionBarHolder.searchText.showDropDown();
        mActionBarHolder.searchText.setText("");
        KeyboardUtil.showKeyboard(mActionBarHolder.searchText);
    }

    public void cancelSearch() {
        mActionBarHolder.searchText.setText("");
        KeyboardUtil.hideKeyboard(mActionBarHolder.searchText);
        mActionBarHolder.searchLayout.setVisibility(View.GONE);
        mActionBarHolder.normalLayout.setVisibility(View.VISIBLE);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    @OnItemClick(R.id.account_list)
    void selectAccount(int position) {
        if (mAccessTokenAdapter.getCount() <= position) {
            Intent intent = new Intent(MainActivity.this, AccountSettingActivity.class);
            startActivityForResult(intent, REQUEST_ACCOUNT_SETTING);
            return;
        }
        AccessToken accessToken = mAccessTokenAdapter.getItem(position);
        if (AccessTokenManager.getUserId() != accessToken.getUserId()) {
            TwitterManager.switchAccessToken(accessToken);
            mAccessTokenAdapter.notifyDataSetChanged();
        }
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }

    @OnClick(R.id.send_button)
    void send() {
        String msg = mQuickTweetEdit.getString();
        if (msg.length() > 0) {
            MessageUtil.showProgressDialog(this, getString(R.string.progress_sending));

            if (msg.startsWith("D ")) {
                SendDirectMessageTask task = new SendDirectMessageTask(null) {
                    @Override
                    protected void onPostExecute(TwitterException e) {
                        MessageUtil.dismissProgressDialog();
                        if (e == null) {
                            mQuickTweetEdit.setText("");
                        } else {
                            MessageUtil.showToast(R.string.toast_update_status_failure);
                        }
                    }
                };
                task.execute(msg);
            } else {
                StatusUpdate statusUpdate = new StatusUpdate(msg);
                if (mInReplyToStatus != null) {
                    statusUpdate.setInReplyToStatusId(mInReplyToStatus.getId());
                    mInReplyToStatus = null;
                }

                UpdateStatusTask task = new UpdateStatusTask(null, null) {
                    @Override
                    protected void onPostExecute(TwitterException e) {
                        MessageUtil.dismissProgressDialog();
                        if (e == null) {
                            mQuickTweetEdit.setText("");
                        } else if (e.getErrorCode() == ERROR_CODE_DUPLICATE_STATUS) {
                            MessageUtil.showToast(getString(R.string.toast_update_status_already));
                        } else {
                            MessageUtil.showToast(R.string.toast_update_status_failure);
                        }
                    }
                };
                task.execute(statusUpdate);
            }
        }
    }

    @OnClick(R.id.post_button)
    void openPost() {
        Intent intent = new Intent(this, PostActivity.class);
        if (mQuickTweetLayout.getVisibility() == View.VISIBLE) {
            String msg = mQuickTweetEdit.getString();
            if (msg.length() > 0) {
                intent.putExtra("status", msg);
                intent.putExtra("selection", msg.length());
                if (mInReplyToStatus != null) {
                    intent.putExtra("inReplyToStatus", mInReplyToStatus);
                }
                mQuickTweetEdit.setText("");
                mQuickTweetEdit.clearFocus();
            }
        }
        startActivity(intent);
    }

    @OnLongClick(R.id.post_button)
    boolean toggleQuickTweet() {
        if (mQuickTweetLayout.getVisibility() == View.VISIBLE) {
            hideQuickPanel();
        } else {
            showQuickPanel();
        }
        return true;
    }

    private TextWatcher mQuickTweetTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            int textColor;
            int length = TwitterUtil.count(charSequence.toString());
            // 140文字をオーバーした時は文字数を赤色に
            if (length < 0) {
                textColor = Color.RED;
            } else if (charSequence.toString().isEmpty()) {
                textColor = mDisabledTextColor;
            } else {
                textColor = mDefaultTextColor;
            }
            TextView count = ((TextView) findViewById(R.id.count));
            count.setTextColor(textColor);
            count.setText(String.valueOf(length));

            if (length < 0 || charSequence.toString().isEmpty()) {
                // 文字数オーバー又は未入力の時はボタンを無効
                mSendButton.setEnabled(false);
            } else {
                mSendButton.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private ActionBarDrawerToggle getActionBarDrawerToggle() {
        int drawer = BasicSettings.getThemeName().equals("black") ?
                R.drawable.ic_dark_drawer :
                R.drawable.ic_dark_drawer;

        return new ActionBarDrawerToggle(
                this, mDrawerLayout, drawer, R.string.open, R.string.close) {
//        return new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close) {

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
    }

    private AdapterView.OnItemClickListener getActionBarAutoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionBarHolder.searchText.getText() == null) {
                    return;
                }
                Intent intent;
                String searchWord = mActionBarHolder.searchText.getString();
                KeyboardUtil.hideKeyboard(mActionBarHolder.searchText);
                if (mSearchAdapter.isSavedMode()) {
                    intent = new Intent(mActivity, SearchActivity.class);
                    intent.putExtra("query", searchWord);
                    startActivityForResult(intent, REQUEST_SEARCH);
                    return;
                }
                switch (i) {
                    case 0:
                        intent = new Intent(mActivity, SearchActivity.class);
                        intent.putExtra("query", searchWord);
                        startActivityForResult(intent, REQUEST_SEARCH);
                        break;
                    case 1:
                        intent = new Intent(mActivity, UserSearchActivity.class);
                        intent.putExtra("query", searchWord);
                        startActivityForResult(intent, REQUEST_SEARCH);
                        break;
                    case 2:
                        intent = new Intent(mActivity, ProfileActivity.class);
                        intent.putExtra("screenName", searchWord);
                        startActivity(intent);
                        break;
                }
            }
        };
    }

    // エンターキーで検索する
    private View.OnKeyListener getOnKeyListener() {
        return new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (mActionBarHolder.searchText.getText() == null) {
                        return false;
                    }
                    Intent intent;
                    String searchWord = mActionBarHolder.searchText.getString();
                    KeyboardUtil.hideKeyboard(mActionBarHolder.searchText);
                    intent = new Intent(mActivity, SearchActivity.class);
                    intent.putExtra("query", searchWord);
                    startActivityForResult(intent, REQUEST_SEARCH);
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * ダイアログ表示要求
     */
    public void onEventMainThread(AlertDialogEvent event) {
        event.getDialogFragment().show(getSupportFragmentManager(), "dialog");
    }

    /**
     * タイムラインなど一番上まで見たという合図
     */
    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GoToTopEvent event) {
        showTopView();
    }

    /**
     * リプや引用などのツイート要求、クイックモードでない場合はPostActivityへ
     */
    public void onEventMainThread(OpenEditorEvent event) {
        View singleLineTweet = findViewById(R.id.quick_tweet_layout);
        if (singleLineTweet != null && singleLineTweet.getVisibility() == View.VISIBLE) {
            mQuickTweetEdit.setText(event.getText());
            if (event.getSelectionStart() != null) {
                if (event.getSelectionStop() != null) {
                    mQuickTweetEdit.setSelection(event.getSelectionStart(), event.getSelectionStop());
                } else {
                    mQuickTweetEdit.setSelection(event.getSelectionStart());
                }
            }
            mInReplyToStatus = event.getInReplyToStatus();
            KeyboardUtil.showKeyboard(mQuickTweetEdit);
        } else {
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra("status", event.getText());
            if (event.getSelectionStart() != null) {
                intent.putExtra("selection", event.getSelectionStart());
            }
            if (event.getSelectionStop() != null) {
                intent.putExtra("selection_stop", event.getSelectionStop());
            }
            if (event.getInReplyToStatus() != null) {
                intent.putExtra("inReplyToStatus", event.getInReplyToStatus());
            }
            startActivity(intent);
        }
    }

    /**
     * ストリーミングAPI接続
     */
    public void onEventMainThread(StreamingConnectionEvent event) {
        if (BasicSettings.getStreamingMode()) {
            switch (event.getStatus()) {
                case STREAMING_CONNECT:
                    ThemeUtil.setThemeTextColor(mActionBarHolder.streamingButton, R.attr.holo_green);
                    break;
                case STREAMING_CLEANUP:
                    ThemeUtil.setThemeTextColor(mActionBarHolder.streamingButton, R.attr.holo_orange);
                    break;
                case STREAMING_DISCONNECT:
                    ThemeUtil.setThemeTextColor(mActionBarHolder.streamingButton, R.attr.holo_red);
                    break;
            }
        } else {
            mActionBarHolder.streamingButton.setTextColor(Color.WHITE);
        }
    }

    /**
     * アカウント変更
     */
    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(AccountChangeEvent event) {
        if (mAccessTokenAdapter != null) {
            mAccessTokenAdapter.notifyDataSetChanged();
        }
        setupTab();
        mViewPager.setCurrentItem(0);
        EventBus.getDefault().post(new PostAccountChangeEvent(mMainPagerAdapter.getItemId(mViewPager.getCurrentItem())));
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * ストリーミングで新しいツイートを受信
     * オートスクロールじゃない場合は対応するタブを青くする
     */
    public void onEventMainThread(NewRecordEvent event) {
        int position = event.getTabId() > TabManager.SEARCH_TAB_ID ? mMainPagerAdapter.findPositionById(event.getTabId()) : mMainPagerAdapter.findPositionBySearchWord(event.getSearchWord());
        if (position < 0) {
            return;
        }
        Button button = (Button) mTabMenus.getChildAt(position);
        if (button == null) {
            return;
        }
        if (mViewPager.getCurrentItem() == position && event.getAutoScroll()) {
            ThemeUtil.setThemeTextColor(button, R.attr.menu_text_color);
        } else {
            ThemeUtil.setThemeTextColor(button, R.attr.holo_blue);
        }
    }
}
