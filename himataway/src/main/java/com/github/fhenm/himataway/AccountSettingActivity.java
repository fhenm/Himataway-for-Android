package com.github.fhenm.himataway;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.account.AccessTokenAdapter;
import com.github.fhenm.himataway.fragment.dialog.AccountSwitchDialogFragment;
import com.github.fhenm.himataway.listener.OnTrashListener;
import com.github.fhenm.himataway.listener.RemoveAccountListener;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.util.MessageUtil;
import com.github.fhenm.himataway.util.ThemeUtil;
import twitter4j.auth.AccessToken;

public class AccountSettingActivity extends AppCompatActivity implements RemoveAccountListener {

    private AccessTokenAdapter mAccountAdapter;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ThemeUtil.setTheme(this);
            setContentView(R.layout.activity_account_setting);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            mAccountAdapter = new AccessTokenAdapter(this, R.layout.row_account);
            for (AccessToken accessToken : AccessTokenManager.getAccessTokens()) {
                mAccountAdapter.add(accessToken);
            }

            mAccountAdapter.setOnTrashListener(new OnTrashListener() {
                @Override
                public void onTrash(int position) {
                    AccountSwitchDialogFragment.newInstance(mAccountAdapter.getItem(position)).show(
                            getSupportFragmentManager(), "dialog");
                }
            });

        AccountSettingActivity accountSettingActivity = this;
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mAccountAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AccessToken accessToken = mAccountAdapter.getItem(i);
                if (AccessTokenManager.getUserId() != accessToken.getUserId()) {
                    Intent data = new Intent();
                    data.putExtra("accessToken", accessToken);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                AccessToken accessToken = mAccountAdapter.getItem(i);
                if (AccessTokenManager.getUserId() == accessToken.getUserId()) {
                    //Intent data = new Intent();
                    //data.putExtra("accessToken", accessToken);
                    //setResult(RESULT_OK, data);
                    new AlertDialog.Builder(accountSettingActivity)
                            .setTitle(getString(R.string.title_accounttoken_settings))
                            .setMessage(getString(R.string.confirm_destroy_account))
                            .setPositiveButton(getString(R.string.button_ok_destroy_account), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK button pressed
                                    MessageUtil.showProgressDialog(accountSettingActivity, getString(R.string.progress_process));
                                    removeAccount(accessToken);
                                    if(AccessTokenManager.hasAccessToken()){
                                        MessageUtil.dismissProgressDialog();
                                    }
                                    else{
                                        MessageUtil.dismissProgressDialog();
                                        Intent intent = new Intent(accountSettingActivity, SignInActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        accountSettingActivity.startActivity(intent);
                                    }
                                    //MessageUtil.dismissProgressDialog();
                                    //MessageUtil.showToast(R.string.toast_connection_failure);
                                }
                            })
                            .setNegativeButton(getString(R.string.button_cancel_destroy_account), null)
                            .show();
                }
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account:
                Intent intent = new Intent(this, SignInActivity.class);
                intent.putExtra("add_account", true);
                startActivity(intent);
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void removeAccount(AccessToken accessToken) {
        mAccountAdapter.remove(accessToken);
        AccessTokenManager.removeAccessToken(accessToken);
    }
}
