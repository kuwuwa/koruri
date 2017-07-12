package com.k3ntaroo.koruri;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;

public class TweetDetailActivity extends KoruriTwitterActivity implements View.OnClickListener {
    private final static String className = TweetDetailActivity.class.getName();
    public final static String STATUS_KEY = className + "#status";

    private Context ctx = this;
    private Status status = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        status = (Status) getIntent().getSerializableExtra(STATUS_KEY);

        if (null == status) {
            Toast.makeText(this, "failed to show the tweet detail", Toast.LENGTH_SHORT);
            finish();
        }

        setContentView(R.layout.post_detail);
        TextView authorView = (TextView) findViewById(R.id.post_author);
        String author = "@" + status.getUser().getScreenName() + "/" + status.getUser().getName();
        authorView.setText(author);

        TextView contentView = (TextView) findViewById(R.id.post_content);
        contentView.setText(status.getText());

        TextView dateView = (TextView) findViewById(R.id.post_date);
        dateView.setText(SIMPLE_DATE_FORMAT.format(status.getCreatedAt()));

        Button replyButton = (Button) findViewById(R.id.reply_button);
        replyButton.setOnClickListener(this);

        Button likeButton = (Button) findViewById(R.id.like_post_button);
        likeButton.setOnClickListener(this);

        Button retweetButton = (Button) findViewById(R.id.retweet_post_button);
        retweetButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.reply_button) {
            EditText replyText = (EditText) findViewById(R.id.reply_text);
            String content = replyText.getText().toString();
            if (!"".equals(content)) {
                Thread th = new SendReplyThread(content);
                th.start();
            }
        } else if (v.getId() == R.id.retweet_post_button) {
        } else if (v.getId() == R.id.like_post_button) {
            Thread th = new LikePostThread();
            th.start();
        }
    }

    private class SendReplyThread extends Thread {
        private String content;

        SendReplyThread(String content) {
            this.content = content;
        }

        @Override public void run() {
            StatusUpdate upd = new StatusUpdate(content);
            upd.setInReplyToStatusId(status.getId());
            try {
                twitter.updateStatus(upd);
                // Toast.makeText(ctx, "success :)", Toast.LENGTH_SHORT);
                Message msg = handler.obtainMessage(MSG_REPLY_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_REPLY_FAILED);
                handler.sendMessage(msg);

            }
        }
    }

    private class LikePostThread extends Thread {
        @Override public void run() {
            try {
                twitter.createFavorite(status.getId());
            } catch (TwitterException e) {

            }
        }
    }

    private static final int MSG_REPLY_SUCCESS = 1301;
    private static final int MSG_REPLY_FAILED = 1302;
    private Handler handler= new Handler(new Handler.Callback() {
        @Override public boolean handleMessage (Message msg){
            if (msg.what == MSG_REPLY_SUCCESS) {
                Toast.makeText(ctx, "success :)", Toast.LENGTH_SHORT);
                EditText replyText = (EditText) findViewById(R.id.reply_text);
                replyText.setText("");
            } else if (msg.what == MSG_REPLY_FAILED) {
                Toast.makeText(ctx, "failed :(", Toast.LENGTH_SHORT);
            }
            return false;
        }
    });
}
