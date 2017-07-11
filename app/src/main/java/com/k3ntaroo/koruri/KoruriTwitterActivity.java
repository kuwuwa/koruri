package com.k3ntaroo.koruri;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class KoruriTwitterActivity extends AppCompatActivity {
    protected Twitter twitter = TwitterFactory.getSingleton();

    protected Twitter getTwitter() {
        SharedPreferences sharedPref = getApplicationContext()
                .getSharedPreferences(
                        getString(R.string.koruri_key),
                        Context.MODE_PRIVATE);
        String accessToken = sharedPref.getString(getString(R.string.access_token_path), "");
        String accessTokenSecret = sharedPref.getString(getString(R.string.access_token_secret_path), "");

        if (!"".equals(accessToken) && !"".equals(accessTokenSecret)) {
            twitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));
        }

        return twitter;
    }

    protected void storeAccessToken(final AccessToken accessToken) {
        SharedPreferences sharedPref = getApplicationContext()
                .getSharedPreferences(
                        getString(R.string.koruri_key),
                        Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(
                getString(R.string.access_token_path),
                accessToken.getToken());
        editor.putString(
                getString(R.string.access_token_secret_path),
                accessToken.getTokenSecret());
        editor.commit();

        twitter.setOAuthAccessToken(accessToken);
    }
}
