package com.k3ntaroo.koruri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class AuthActivity extends KoruriTwitterActivity implements View.OnClickListener {
    private final String name = "AuthActivity";

    private EditText pinText;
    private RequestToken reqToken;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity);

        pinText = (EditText) findViewById(R.id.auth_pin_text);
        statusText = (TextView) findViewById(R.id.auth_status_message);

        Button authPageButton = (Button) findViewById(R.id.open_auth_page_button);
        authPageButton.setOnClickListener(this);

        Button doAuthButton = (Button) findViewById(R.id.do_auth_button);
        doAuthButton.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        Log.d(name, "click");
        if (v.getId() == R.id.open_auth_page_button) {
            new GetAuthURLThread().start();
        } else if (v.getId() == R.id.do_auth_button) {
            String pin = pinText.getText().toString();
            new GetAccessTokenThread(this.reqToken, pin).start();
        }
    }

    private class GetAuthURLThread extends Thread {
        @Override
        public void run() {
            try {
                final RequestToken reqToken = twitter.getOAuthRequestToken();
                Message msg = authURLHandler.obtainMessage(MSG_AUTH_URL, reqToken);
                authURLHandler.sendMessage(msg);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }
    }

    private final static int MSG_AUTH_URL = 1212;
    private Handler authURLHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_AUTH_URL) {
                reqToken = (RequestToken) msg.obj;
                final String authURL = reqToken.getAuthorizationURL();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authURL));
                startActivity(intent);
            }
            return false;
        }
    });

    private class GetAccessTokenThread extends Thread {
        private final RequestToken reqToken;
        private final String pin;

        private GetAccessTokenThread (final RequestToken reqToken, final String pin) {
            this.reqToken = reqToken;
            this.pin = pin;
        }

        @Override
        public void run() {
            if (null == this.reqToken || null == this.pin) {
                return;
            }
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(this.reqToken, this.pin);
                Message msg = accessTokenHandler.obtainMessage(MSG_ACCESS_TOKEN_URL, accessToken);
                accessTokenHandler.sendMessage(msg);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }
    }

    private final static int MSG_ACCESS_TOKEN_URL = 1213;
    private Handler accessTokenHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_ACCESS_TOKEN_URL) {
                final AccessToken accessToken = (AccessToken) msg.obj;
                storeAccessToken(accessToken);
                setResult(RESULT_OK);
                finish();
            }
            return false;
        }
    });
}
