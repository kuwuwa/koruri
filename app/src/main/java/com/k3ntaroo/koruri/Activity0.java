package com.k3ntaroo.koruri;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class Activity0 extends AppCompatActivity {
    private final static int AUTH_REQ_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_0);

        Intent intent = new Intent(this, AuthActivity.class);
        startActivityForResult(intent, AUTH_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == AUTH_REQ_CODE) {
        }
    }
}
