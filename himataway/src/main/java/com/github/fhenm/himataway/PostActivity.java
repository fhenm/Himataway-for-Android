package com.github.fhenm.himataway;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.model.UserIconManager;
import com.github.fhenm.himataway.plugin.TwiccaPlugin;
import com.github.fhenm.himataway.settings.PostStockSettings;
import com.github.fhenm.himataway.task.SendDirectMessageTask;
import com.github.fhenm.himataway.task.UpdateStatusTask;
import com.github.fhenm.himataway.util.FileUtil;
import com.github.fhenm.himataway.util.ImageUtil;
import com.github.fhenm.himataway.util.MessageUtil;
import com.github.fhenm.himataway.util.ThemeUtil;
import com.github.fhenm.himataway.util.TwitterUtil;
import com.github.fhenm.himataway.widget.FontelloButton;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

public class PostActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_TWICCA = 3;
    private static final int REQUEST_PERMISSIONS_CAMERA = 1;
    private static final int OPTION_MENU_GROUP_TWICCA = 1;
    private static final int ERROR_CODE_DUPLICATE_STATUS = 187;
    private static final int ERROR_CODE_NOT_FOLLOW_DM = 150;
    private static final int MAX_IMAGE = 4;

    private Activity mContext;
    private Long mInReplyToStatusId;
    private File mImgPath;
    private Uri mImageUri;
    private AlertDialog mDraftDialog;
    private AlertDialog mHashtagDialog;
    private boolean mWidgetMode;
    private PostStockSettings mPostStockSettings;
    private ArrayList<String> mTextHistory = new ArrayList<String>();
    private List<ResolveInfo> mTwiccaPlugins;
    private ActionBarHolder mActionBarHolder;

    @BindView(R.id.in_reply_to_cancel) TextView mCancel;
    @BindView(R.id.in_reply_to_user_icon) ImageView mInReplyToUserIcon;
    @BindView(R.id.in_reply_to_status) TextView mInReplyToStatus;
    @BindView(R.id.in_reply_to_layout) RelativeLayout mInReplyToLayout;
    @BindView(R.id.switch_account_spinner) Spinner mSwitchAccountSpinner;
    @BindView(R.id.status_text) EditText mStatusText;
    @BindView(R.id.suddenly_button) Button mSuddenlyButton;
    @BindView(R.id.tweet_button) Button mTweetButton;
    @BindView(R.id.img_button) Button mImgButton;
    @BindView(R.id.draft_button) Button mDraftButton;
    @BindView(R.id.hashtag_button) Button mHashtagButton;
    @BindView(R.id.count) TextView mCount;
    @BindView(R.id.image_preview_container)
    ViewGroup mImagePreviewContainer;

    class ActionBarHolder {

        @BindView(R.id.title) TextView mTitle;
        @BindView(R.id.undo) FontelloButton mUndo;

        @OnClick(R.id.undo)
        void undo() {
            String text = mTextHistory.get(mTextHistory.size() - 1);
            if (mStatusText.getText() != null && text.equals(mStatusText.getText().toString())) {
                mTextHistory.remove(mTextHistory.size() - 1);
                if (mTextHistory.size() > 0) {
                    text = mTextHistory.get(mTextHistory.size() - 1);
                }
            }
            mStatusText.setText(text);
            mStatusText.setSelection(mStatusText.length());
            mTextHistory.remove(mTextHistory.size() - 1);
            if (mTextHistory.size() > 0 && text.equals(mStatusText.getText().toString())) {
                mTextHistory.remove(mTextHistory.size() - 1);
            }
            mUndo.setEnabled(mTextHistory.size() > 0);
        }

        public ActionBarHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @SuppressWarnings("MagicConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_post);
        ButterKnife.bind(this);
        mContext = this;

        // Wear からリプライを返す
        Intent intent = getIntent();
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence charSequence = remoteInput.getCharSequence(NotificationService.EXTRA_VOICE_REPLY);
            Status inReplyToStatus = (Status) intent.getSerializableExtra("inReplyToStatus");
            mInReplyToStatusId = inReplyToStatus.getId();
            String inReplyToUserScreenName = inReplyToStatus.getUser().getScreenName();
            if (inReplyToStatus.getRetweetedStatus() != null) {
                inReplyToStatus = inReplyToStatus.getRetweetedStatus();
                inReplyToUserScreenName = inReplyToStatus.getUser().getScreenName();
            }
            mInReplyToStatusId = inReplyToStatus.getId();
            mStatusText.setText("@" + inReplyToUserScreenName + " " + charSequence.toString());

            tweet();
            return;
        }

        // Wear からツイート
        if (intent.getBooleanExtra("wearable", false)) {
            mStatusText.setText(intent.getStringExtra("status"));
            tweet();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            int options = actionBar.getDisplayOptions();
            if ((options & ActionBar.DISPLAY_SHOW_CUSTOM) == ActionBar.DISPLAY_SHOW_CUSTOM) {
                actionBar.setDisplayShowCustomEnabled(false);
            } else {
                actionBar.setDisplayShowCustomEnabled(true);
                if (actionBar.getCustomView() == null) {
                    actionBar.setCustomView(R.layout.action_bar_post);
                    mActionBarHolder = new ActionBarHolder(actionBar.getCustomView());
                }
            }
        }

        UserIconManager.warmUpUserIconMap();

        registerForContextMenu(mImgButton);

        // アカウント切り替え
        ArrayList<AccessToken> accessTokens = AccessTokenManager.getAccessTokens();
        AccessTokenAdapter adapter = new AccessTokenAdapter(this, R.layout.spinner_switch_account);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSwitchAccountSpinner.setAdapter(adapter);

        if (accessTokens != null) {
            int i = 0;
            for (AccessToken accessToken : accessTokens) {
                adapter.add(accessToken);

                if (AccessTokenManager.getUserId() == accessToken.getUserId()) {
                    mSwitchAccountSpinner.setSelection(i);
                }
                i++;
            }
        }

        if (intent.getBooleanExtra("notification", false)) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancelAll();
        }
        mWidgetMode = intent.getBooleanExtra("widget", false);
        if (mWidgetMode) {
            mActionBarHolder.mTitle.setText(getString(R.string.widget_title_post_mode));
        } else {
            mActionBarHolder.mTitle.setText(getString(R.string.title_post));
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        String status = intent.getStringExtra("status");
        if (status != null) {
            mStatusText.setText(status);
        }

        int selection_start = intent.getIntExtra("selection", 0);
        if (selection_start > 0) {
            int selection_stop = intent.getIntExtra("selection_stop", 0);
            if (selection_stop > 0) {
                mStatusText.setSelection(selection_start, selection_stop);
            } else {
                mStatusText.setSelection(selection_start);
            }
        }

        Status inReplyToStatus = (Status) intent.getSerializableExtra("inReplyToStatus");
        if (inReplyToStatus != null) {
            if (inReplyToStatus.getRetweetedStatus() != null) {
                inReplyToStatus = inReplyToStatus.getRetweetedStatus();
            }
            mInReplyToStatusId = inReplyToStatus.getId();
            ImageUtil.displayRoundedImage(inReplyToStatus.getUser().getProfileImageURL(),
                    mInReplyToUserIcon);

            mInReplyToStatus.setText(inReplyToStatus.getText());

            // スクロール可能にするのに必要
            mInReplyToStatus.setMovementMethod(ScrollingMovementMethod.getInstance());
        } else {
            mInReplyToLayout.setVisibility(View.GONE);
        }

        if (intent.getData() != null) {
            String inReplyToStatusId = intent.getData().getQueryParameter("in_reply_to");
            if (inReplyToStatusId != null && inReplyToStatusId.length() > 0) {
                mInReplyToStatusId = Long.valueOf(inReplyToStatusId);
            }

            String text = intent.getData().getQueryParameter("text");
            String url = intent.getData().getQueryParameter("url");
            String hashtags = intent.getData().getQueryParameter("hashtags");
            if (text == null) {
                text = "";
            }
            if (url != null) {
                text += " " + url;
            }
            if (hashtags != null) {
                text += " #" + hashtags;
            }
            mStatusText.setText(text);
        }

        // ブラウザから来たとき
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {
                Uri imgUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                setImage(imgUri);
            } else if (intent.getExtras() != null) {
                String pageUri = intent.getExtras().getString(Intent.EXTRA_TEXT);
                String pageTitle = intent.getExtras().getString(Intent.EXTRA_SUBJECT);
                if (pageTitle == null) {
                    pageTitle = "";
                }
                if (pageUri != null) {
                    pageTitle += " " + pageUri;
                }
                mStatusText.setText(pageTitle);
            }
        }

        // 文字をカウントする
        if (mStatusText.getText() != null) {
            updateCount(mStatusText.getText().toString());
        }

        // 下書きとハッシュタグがあるかチェック
        mPostStockSettings = new PostStockSettings();
        if (mPostStockSettings.getDrafts().isEmpty()) {
            mDraftButton.setEnabled(false);
        }
        if (mPostStockSettings.getHashtags().isEmpty()) {
            mHashtagButton.setEnabled(false);
        }

        // 文字数をカウントしてボタンを制御する
        mStatusText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCount(s.toString());
                if (s.toString().startsWith("D ")) {
                    mImagePreviewContainer.removeAllViews();
                    mImgButton.setEnabled(false);
                } else {
                    mImgButton.setEnabled(true);
                }
            }

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 直近のと一緒なら保存しない
                if (mTextHistory.size() == 0 || !s.toString().equals(mTextHistory.get(mTextHistory.size() - 1))) {
                    mTextHistory.add(s.toString());
                }
                mActionBarHolder.mUndo.setEnabled(mTextHistory.size() > 0);
            }
        });
    }

    @OnClick(R.id.in_reply_to_cancel)
    void closeInReplyToLayout() {
        mInReplyToLayout.setVisibility(View.GONE);
    }

    @OnClick(R.id.hashtag_button)
    void showHashtag() {
        View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list, null);
        assert view != null;
        ListView listView = (ListView) view.findViewById(R.id.list);

        // ハッシュタグをViewに描写するアダプター
        final HashtagAdapter adapter = new HashtagAdapter(mContext, R.layout.row_word);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String hashtag = adapter.getItem(i);
                if (mStatusText.getText() != null) {
                    mStatusText.setText(mStatusText.getText().toString().concat(" ".concat(hashtag)));
                    mHashtagDialog.dismiss();
                }
            }
        });

        PostStockSettings postStockSettings = new PostStockSettings();

        for (String hashtag : postStockSettings.getHashtags()) {
            adapter.add(hashtag);
        }
        mHashtagDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.dialog_title_hashtag)
                .setView(view)
                .show();
    }

    @OnClick(R.id.draft_button)
    void showDraft() {
        View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list, null);
        assert view != null;
        ListView listView = (ListView) view.findViewById(R.id.list);

        // 下書きをViewに描写するアダプター
        final DraftAdapter adapter = new DraftAdapter(mContext, R.layout.row_word);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String draft = adapter.getItem(i);
                mStatusText.setText(draft);
                mDraftDialog.dismiss();
                adapter.remove(draft);
                mPostStockSettings.removeDraft(draft);
            }
        });

        PostStockSettings postStockSettings = new PostStockSettings();
        for (String draft : postStockSettings.getDrafts()) {
            adapter.add(draft);
        }
        mDraftDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.dialog_title_draft)
                .setView(view)
                .show();
    }

    @OnClick(R.id.suddenly_button)
    void setSuddenly() {
        assert mStatusText.getText() != null;
        String text = mStatusText.getText().toString();
        int selectStart = mStatusText.getSelectionStart();
        int selectEnd = mStatusText.getSelectionEnd();

        mStatusText.setText(TwitterUtil.convertSuddenly(text, selectStart, selectEnd));
    }

    @OnClick(R.id.img_button)
    void setImage() {
        mImgButton.showContextMenu();
    }

    @OnClick(R.id.tweet_button)
    void tweet() {
        MessageUtil.showProgressDialog(mContext, getString(R.string.progress_sending));
        String text = mStatusText.getText().toString();

        if (text.startsWith("D ")) {
            SendDirectMessageTask task = new SendDirectMessageTask((AccessToken) mSwitchAccountSpinner.getSelectedItem()) {
                @Override
                protected void onPostExecute(TwitterException e) {
                    MessageUtil.dismissProgressDialog();
                    if (e == null) {
                        mStatusText.setText("");
                        if (!mWidgetMode) {
                            finish();
                        }
                    } else if (e.getErrorCode() == ERROR_CODE_NOT_FOLLOW_DM) {
                        MessageUtil.showToast(getString(R.string.toast_update_status_not_Follow));
                    } else {
                        MessageUtil.showToast(R.string.toast_update_status_failure);
                    }
                }
            };
            task.execute(text);
        } else {
            StatusUpdate statusUpdate = new StatusUpdate(text);
            if (mInReplyToStatusId != null) {
                statusUpdate.setInReplyToStatusId(mInReplyToStatusId);
            }

            ArrayList<File> mImagePathList = new ArrayList<>();
            for (int i = 0; mImagePreviewContainer.getChildCount() > i; i++) {
                mImagePathList.add((File) mImagePreviewContainer.getChildAt(i).getTag());
            }
            UpdateStatusTask task = new UpdateStatusTask((AccessToken) mSwitchAccountSpinner.getSelectedItem(), mImagePathList) {
                @Override
                protected void onPostExecute(TwitterException e) {
                    MessageUtil.dismissProgressDialog();
                    if (e == null) {
                        mStatusText.setText("");
                        if (!mWidgetMode) {
                            finish();
                        } else {
                            mImagePreviewContainer.removeAllViews();
                            mTweetButton.setEnabled(false);
                        }
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


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("image_uri", mImageUri);
        outState.putSerializable("image_path", mImgPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final Parcelable imageUri = savedInstanceState.getParcelable("image_uri");
        mImageUri = (Uri) imageUri;
        final Serializable imagePath = savedInstanceState.getSerializable("image_path");
        mImgPath = (File) imagePath;

        if (mImgPath != null && mImgPath.exists()) {
            mTweetButton.setEnabled(true);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(getString(R.string.context_menu_title_photo_method));
        menu.add(0, REQUEST_GALLERY, 0, R.string.context_menu_photo_gallery);
        menu.add(0, REQUEST_CAMERA, 0, R.string.context_menu_photo_camera);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case REQUEST_GALLERY:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_GALLERY);
                return true;
            case REQUEST_CAMERA:
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS_CAMERA);
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
            }
        }
    }

    public void openCamera() {
        String filename = System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        mImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_GALLERY:
                setImage(data.getData());
                break;
            case REQUEST_CAMERA:
                setImage(mImageUri);
                break;
            case REQUEST_TWICCA:
                mStatusText.setText(data.getStringExtra(Intent.EXTRA_TEXT));
                break;
        }
    }

    private void setImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.image_preview, null);
            imageView.setTag(FileUtil.writeToTempFile(getCacheDir(), inputStream));

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mImagePreviewContainer.removeView(v);
                    mImgButton.setEnabled(true);

                    if (mImagePreviewContainer.getChildCount() == 0) {
                        mImagePreviewContainer.removeAllViews();
                        updateCount(mStatusText.getText().toString());
                    }
                }
            });
            imageView.setImageURI(uri);
            mImagePreviewContainer.addView(imageView);
            if (mStatusText.getText() != null) {
                updateCount(mStatusText.getText().toString());
            }

            if (mImagePreviewContainer.getChildCount() >= MAX_IMAGE) {
                mImgButton.setEnabled(false);
                return;
            }
            MessageUtil.showToast(R.string.toast_set_image_success);
            mTweetButton.setEnabled(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateCount(String str) {
        int textColor;

        int length = TwitterUtil.count(str);
        if (mImagePreviewContainer.getChildCount() > 0) {
            length -= 24;
        }
        // 文字数をオーバーした時は文字数を赤色に
        if (length < 0) {
            textColor = Color.RED;
        } else {
            textColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color);
        }
        mCount.setTextColor(textColor);
        mCount.setText(String.valueOf(length));

        if (length < 0) {
            // 文字数オーバー
            mTweetButton.setEnabled(false);
        } else if (str.isEmpty()) {
            // 何も入力されていない時も画像があれば投稿可
            mTweetButton.setEnabled(mImagePreviewContainer.getChildCount() > 0);
        } else {
            mTweetButton.setEnabled(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStatusText.getText() != null && mStatusText.getText().length() != 0) {
                new AlertDialog.Builder(PostActivity.this)
                        .setMessage(R.string.confirm_save_draft)
                        .setPositiveButton(
                                R.string.button_save,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 下書きとして保存する
                                        mPostStockSettings.addDraft(mStatusText.getText().toString());

                                        finish();
                                    }
                                }
                        )
                        .setNegativeButton(
                                R.string.button_destroy,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }
                        )
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post, menu);
        // twiccaプラグインを読む
        if (mTwiccaPlugins == null) {
            mTwiccaPlugins = TwiccaPlugin.getResolveInfo(this.getPackageManager(),
                    TwiccaPlugin.TWICCA_ACTION_EDIT_TWEET);
        }
        if (!mTwiccaPlugins.isEmpty()) {
            PackageManager pm = this.getPackageManager();
            int i = 0;
            for (ResolveInfo resolveInfo : mTwiccaPlugins) {
                if (pm == null || resolveInfo.activityInfo == null) {
                    continue;
                }
                String label = (String) resolveInfo.activityInfo.loadLabel(pm);
                if (label == null) {
                    continue;
                }
                menu.add(OPTION_MENU_GROUP_TWICCA, i, 100, label);
                i++;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == OPTION_MENU_GROUP_TWICCA) {
            ResolveInfo resolveInfo = mTwiccaPlugins.get(item.getItemId());
            if (resolveInfo.activityInfo != null && mStatusText.getText() != null) {
                Intent intent = TwiccaPlugin.createIntentEditTweet(
                        "", mStatusText.getText().toString(), "", 0, resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);
                startActivityForResult(intent, REQUEST_TWICCA);
            }
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.tweet_clear:
                mStatusText.setText("");
                break;
            case R.id.tweet_battery:
                // バッテリー情報をセットする
                mStatusText.setText(TwitterUtil.getBatteryStatus(mContext));
                break;
        }
        return true;
    }

    public class AccessTokenAdapter extends ArrayAdapter<AccessToken> {

        private LayoutInflater mInflater;
        private int mLayout;

        public AccessTokenAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayout = textViewResourceId;
        }

        @Override
        public void add(AccessToken accessToken) {
            super.add(accessToken);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
            }

            AccessToken accessToken = getItem(position);

            assert view != null;
            view.setPadding(16, 0, 0, 0);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            UserIconManager.displayUserIcon(accessToken.getUserId(), icon);
            ((TextView) view.findViewById(R.id.screen_name)).setText(accessToken.getScreenName());

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
            }

            AccessToken accessToken = getItem(position);

            assert view != null;
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            UserIconManager.displayUserIcon(accessToken.getUserId(), icon);
            ((TextView) view.findViewById(R.id.screen_name)).setText(accessToken.getScreenName());

            return view;
        }
    }


    public class DraftAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private int mLayout;

        public DraftAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayout = textViewResourceId;
        }

        @Override
        public void add(String draft) {
            super.add(draft);
        }

        @Override
        public void remove(String draft) {
            super.remove(draft);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
            }

            final String draft = getItem(position);

            assert view != null;
            ((TextView) view.findViewById(R.id.word)).setText(draft);

            view.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(draft);
                    mPostStockSettings.removeDraft(draft);
                }
            });
            return view;
        }
    }

    public class HashtagAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private int mLayout;

        public HashtagAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayout = textViewResourceId;
        }

        @Override
        public void add(String hashtag) {
            super.add(hashtag);
        }

        @Override
        public void remove(String hashtag) {
            super.remove(hashtag);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
            }

            final String hashtag = getItem(position);

            assert view != null;
            ((TextView) view.findViewById(R.id.word)).setText(hashtag);

            view.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(hashtag);
                    mPostStockSettings.removeHashtag(hashtag);
                }
            });
            return view;
        }
    }
}
