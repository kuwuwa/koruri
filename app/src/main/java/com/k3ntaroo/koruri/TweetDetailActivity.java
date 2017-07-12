package com.k3ntaroo.koruri;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.User;

public class TweetDetailActivity extends KoruriTwitterActivity implements View.OnClickListener {
    private final static String className = TweetDetailActivity.class.getName();
    public final static String STATUS_KEY = className + "#status";

    private Context ctx = this;
    private Status detailedStatus = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        detailedStatus = (Status) getIntent().getSerializableExtra(STATUS_KEY);

        if (null == detailedStatus) {
            Toast.makeText(this, "failed to show the tweet detail", Toast.LENGTH_SHORT);
            finish();
        }

        setContentView(R.layout.post_detail);
        TextView authorScreenNameView = (TextView) findViewById(R.id.post_author_screen_name);
        authorScreenNameView.setText(detailedStatus.getUser().getScreenName());
        TextView authorView = (TextView) findViewById(R.id.post_author_name);
        authorView.setText(detailedStatus.getUser().getName());

        LinearLayout userLL = (LinearLayout) findViewById(R.id.post_author_box);
        userLL.setOnClickListener(this);

        TextView contentView = (TextView) findViewById(R.id.post_content);
        contentView.setText(detailedStatus.getText());

        TextView dateView = (TextView) findViewById(R.id.post_date);
        dateView.setText(SIMPLE_DATE_FORMAT.format(detailedStatus.getCreatedAt()));

        Button replyButton = (Button) findViewById(R.id.reply_button);
        replyButton.setOnClickListener(this);

        Button likeButton = (Button) findViewById(R.id.like_post_button);
        likeButton.setOnClickListener(this);

        Button retweetButton = (Button) findViewById(R.id.retweet_post_button);
        retweetButton.setOnClickListener(this);

        if (detailedStatus.getUser().getId() == getMyId()) {
            Button deleteButton = (Button) findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.post_author_box) {
            User user = detailedStatus.getUser();
            Intent userIntent = new Intent(this, UserDetailActivity.class);
            userIntent.putExtra(UserDetailActivity.USER_KEY, user);
            startActivity(userIntent);
        } else if (v.getId() == R.id.reply_button) {
            EditText replyText = (EditText) findViewById(R.id.reply_text);
            String content = replyText.getText().toString();
            if (!"".equals(content)) {
                Thread th = new SendReplyThread(content);
                th.start();
            }
        } else if (v.getId() == R.id.retweet_post_button) {
            new RetweetPostThread().start();
        } else if (v.getId() == R.id.like_post_button) {
            new LikePostThread().start();
        } else if (v.getId() == R.id.delete_button) {
            new DeletePostThread().start();
        }
    }

    private class SendReplyThread extends Thread {
        private String content;

        SendReplyThread(String content) {
            this.content = content;
        }

        @Override public void run() {
            String fullContent = "@" + detailedStatus.getUser().getScreenName() + " " + content;
            StatusUpdate upd = new StatusUpdate(fullContent);
            try {
                twitter.updateStatus(upd.inReplyToStatusId(detailedStatus.getId()));
                Message msg = handler.obtainMessage(MSG_REPLY_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_REPLY_FAILED);
                handler.sendMessage(msg);

            }
        }
    }

    private final class RetweetPostThread extends Thread {
        @Override public void run() {
            try {
                twitter.retweetStatus(detailedStatus.getId());
                Message msg = handler.obtainMessage(MSG_RETWEET_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_RETWEET_FAILED);
            }
        }
    }

    private final class LikePostThread extends Thread {
        @Override public void run() {
            try {
                twitter.createFavorite(detailedStatus.getId());
                Message msg = handler.obtainMessage(MSG_LIKE_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_LIKE_FAILED);
            }
        }
    }

    private final class DeletePostThread extends Thread {
        @Override public void run() {
            try {
                twitter.destroyStatus(detailedStatus.getId());
                Message msg = handler.obtainMessage(MSG_DELETE_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_DELETE_FAILED);
                handler.sendMessage(msg);
            }
        }
    }

    private static final int MSG_STATUS_MINE = 1340;
    private static final int MSG_REPLY_SUCCESS = 1301;
    private static final int MSG_REPLY_FAILED = 1302;
    private static final int MSG_LIKE_SUCCESS = 1311;
    private static final int MSG_LIKE_FAILED = 1312;
    private static final int MSG_RETWEET_SUCCESS = 1321;
    private static final int MSG_RETWEET_FAILED = 1322;
    private static final int MSG_DELETE_SUCCESS = 1331;
    private static final int MSG_DELETE_FAILED = 1332;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override public boolean handleMessage (Message msg) {
            if (msg.what == MSG_STATUS_MINE) {
            } else if (msg.what == MSG_REPLY_SUCCESS) {
                Toast.makeText(ctx, "success :)", Toast.LENGTH_SHORT);
                EditText replyText = (EditText) findViewById(R.id.reply_text);
                replyText.setText("");
            } else if (msg.what == MSG_REPLY_FAILED) {
                Toast.makeText(ctx, "failed :(", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_LIKE_SUCCESS) {
                Toast.makeText(ctx, "success :)", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_LIKE_FAILED) {
                Toast.makeText(ctx, "failed :(", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_RETWEET_SUCCESS) {
                Toast.makeText(ctx, "success :)", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_RETWEET_FAILED) {
                Toast.makeText(ctx, "failed :(", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_DELETE_SUCCESS) {
                Toast.makeText(ctx, "successfully deleted ;)", Toast.LENGTH_LONG);
                finish();
            } else if (msg.what == MSG_DELETE_FAILED) {
                Toast.makeText(ctx, "failed in deleting", Toast.LENGTH_LONG);
            }
            return false;
        }
    });
}
