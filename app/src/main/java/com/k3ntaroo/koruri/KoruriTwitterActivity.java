package com.k3ntaroo.koruri;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public abstract class KoruriTwitterActivity extends AppCompatActivity {
    private final static String KEY_NAME = "KoruriTwitterActivity";
    private final static int AUTH_REQ_CODE = 1001;

    protected long myId = -1;
    protected static Twitter twitter = TwitterFactory.getSingleton();

    protected final static String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    protected final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        SharedPreferences sharedPref = getApplicationContext()
                .getSharedPreferences(
                        getString(R.string.koruri_key),
                        Context.MODE_PRIVATE);
        String accessToken = sharedPref.getString(getString(R.string.access_token_path), "");
        String accessTokenSecret = sharedPref.getString(getString(R.string.access_token_secret_path), "");

        if (!"".equals(accessToken) && !"".equals(accessTokenSecret)) {
            twitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));
        } else {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivityForResult(intent, AUTH_REQ_CODE);
        }
    }

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

    protected long getMyId() {
        if (myId != -1) return myId;
        Thread th = new GetIdThread();
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return myId;
    }

    private class GetIdThread extends Thread {
        @Override public void run() {
            try {
                myId = twitter.getId();
            } catch (TwitterException e) {
                // give up
            }

        }
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

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == AUTH_REQ_CODE && resCode == RESULT_OK) {
            Log.d(KEY_NAME, "authorized!");
            recreate();
        }
    }

    protected void logout() {
        final SharedPreferences sharedPref = getApplicationContext()
                .getSharedPreferences(
                        getString(R.string.koruri_key),
                        Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(getString(R.string.access_token_path));
        editor.remove(getString(R.string.access_token_secret_path));
        editor.commit();
        twitter = TwitterFactory.getSingleton();

        twitter.setOAuthAccessToken(null);
        recreate();
    }
}
