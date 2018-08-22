package com.github.fhenm.himataway.listener;

import twitter4j.auth.AccessToken;

public interface RemoveAccountListener {
    void removeAccount(AccessToken accessToken);
}
