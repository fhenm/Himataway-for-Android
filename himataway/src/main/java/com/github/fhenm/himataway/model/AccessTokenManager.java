package com.github.fhenm.himataway.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;

import com.github.fhenm.himataway.himatawayApplication;
import twitter4j.auth.AccessToken;

public class AccessTokenManager {

    private static final String TOKENS = "tokens";
    private static final String PREF_NAME = "twitter_access_token";
    private static AccessToken sAccessToken;

    private static SharedPreferences getSharedPreferences() {
        return himatawayApplication.getApplication()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean hasAccessToken() {
        return getAccessToken() != null;
    }

    public static ArrayList<AccessToken> getAccessTokens() {
        String json = getSharedPreferences().getString(TOKENS, null);
        if (json == null) {
            return null;
        }

        Gson gson = new Gson();
        AccountSettings accountSettings = gson.fromJson(json, AccountSettings.class);
        return accountSettings.accessTokens;
    }

    public static AccessToken getAccessToken() {
        if (sAccessToken != null) {
            return sAccessToken;
        }

        String json = getSharedPreferences().getString(TOKENS, null);
        if (json == null) {
            sAccessToken = null;
            return null;
        }

        Gson gson = new Gson();
        AccountSettings accountSettings = gson.fromJson(json, AccountSettings.class);
        if (accountSettings == null || accountSettings.accessTokens == null || accountSettings.accessTokens.size() == 0) {
            sAccessToken = null;
            return null;
        }
        sAccessToken = accountSettings.accessTokens.get(accountSettings.index);
        return sAccessToken;
    }

    public static void setAccessToken(AccessToken accessToken) {

        sAccessToken = accessToken;

        TwitterManager.getTwitter().setOAuthAccessToken(sAccessToken);

        SharedPreferences preferences = getSharedPreferences();
        String json = preferences.getString(TOKENS, null);
        Gson gson = new Gson();

        AccountSettings accountSettings;
        if (json != null) {
            accountSettings = gson.fromJson(json, AccountSettings.class);

            boolean existUser = false;
            int i = 0;
            for (AccessToken sharedAccessToken : accountSettings.accessTokens) {
                if (accessToken.getUserId() == sharedAccessToken.getUserId()) {
                    accountSettings.accessTokens.set(i, accessToken);
                    accountSettings.index = i;
                    existUser = true;
                    /*なんかよくわからんけどもにゅもにゅしてるからbreakしといたよ*/
                    break;
                }
                i++;
            }

            if (!existUser) {
                accountSettings.index = accountSettings.accessTokens.size();
                accountSettings.accessTokens.add(sAccessToken);
            }
        } else {
            accountSettings = new AccountSettings();
            accountSettings.accessTokens = new ArrayList<AccessToken>();
            accountSettings.accessTokens.add(sAccessToken);
        }

        String exportJson = gson.toJson(accountSettings);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TOKENS, exportJson);
        editor.apply();
    }

    public static void removeAccessToken(AccessToken removeAccessToken) {
        SharedPreferences preferences = getSharedPreferences();
        String json = preferences.getString(TOKENS, null);
        Gson gson = new Gson();

        AccountSettings accountSettings = gson.fromJson(json, AccountSettings.class);
        AccessToken currentAccessToken = accountSettings.accessTokens.get(accountSettings.index);

        //削除対象が現在のカレントだったら
        if (currentAccessToken.getUserId() == removeAccessToken.getUserId()) {//要素が削除したら消える場合
            sAccessToken = null;
            if(accountSettings.accessTokens.size() == 1){
                accountSettings.index = 0;
            }
            else if(accountSettings.accessTokens.size() > accountSettings.index && accountSettings.index == 0){//カレント要素が0であったら
                accountSettings.index++;
                sAccessToken = accountSettings.accessTokens.get(accountSettings.index);
            }
            else{
                accountSettings.index--;
                sAccessToken = accountSettings.accessTokens.get(accountSettings.index);
            }
        }
        else{//それ以外
            /**
             * 現在設定されているAccessTokenより先に削除すべきAccessTokenがある場合indexをデクリメントする
             * これをしないと位置がずれる
             */
            for (AccessToken accessToken : accountSettings.accessTokens) {
                if (accessToken.getUserId() == removeAccessToken.getUserId()) {
                    accountSettings.index--;
                    break;
                }
                if (accessToken.getUserId() == currentAccessToken.getUserId()) {
                    break;
                }
            }
        }
        accountSettings.accessTokens.remove(removeAccessToken);

        String exportJson = gson.toJson(accountSettings);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TOKENS, exportJson);
        editor.apply();
    }

    public static long getUserId() {
        if (sAccessToken == null) {
            return -1L;
        }
        return sAccessToken.getUserId();
    }

    public static String getScreenName() {
        if (sAccessToken == null) {
            return "";
        }
        return sAccessToken.getScreenName();
    }

    public static class AccountSettings {
        int index;
        ArrayList<AccessToken> accessTokens;
    }
}
