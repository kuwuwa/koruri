package com.k3ntaroo.koruri;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

public class UserDetailActivity extends KoruriTwitterActivity
        implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String className = UserDetailActivity.class.getName();
    public final static String USER_KEY = className + "#user";

    private final Context ctx = this;

    private User user;
    private Relationship friendship;

    private ArrayAdapter<Status> twAdapter;
    private List<Status> statusList = new ArrayList<>();

    private Handler statusesHandler;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        user = (User) getIntent().getSerializableExtra(USER_KEY);
        if (null == user) {
            Toast.makeText(this, "couldn't show the user detail", Toast.LENGTH_LONG);
            finish();
        }

        setContentView(R.layout.user_detail);

        TextView authorScreenNameText = (TextView) findViewById(R.id.user_screen_name);
        authorScreenNameText.setText(user.getScreenName());

        TextView authorNameText = (TextView) findViewById(R.id.user_name);
        authorNameText.setText(user.getName());

        TextView authorBioText = (TextView) findViewById(R.id.user_bio);
        authorBioText.setText(user.getDescription());

        TextView numFollowingsText = (TextView) findViewById(R.id.user_num_followings);
        numFollowingsText.setText(Integer.toString(user.getFriendsCount()));

        TextView numFollowersText = (TextView) findViewById(R.id.user_num_followers);
        numFollowersText.setText(Integer.toString(user.getFollowersCount()));

        Button followButton = (Button) findViewById(R.id.user_follow);
        followButton.setOnClickListener(this);

        // user's status
        ListView lv = (ListView) findViewById(R.id.user_statuses);
        twAdapter = new ArrayAdapter<Status>(this, 0, statusList) {
            @Override
            public
            @NonNull
            View getView(int pos, @Nullable View view, ViewGroup par) {

                Status st = getItem(pos);
                LayoutInflater li = LayoutInflater.from(getContext());

                if (st.getId() == -2) {
                    view = li.inflate(R.layout.continue_in_list, par, false);
                    return view;
                }

                view = li.inflate(R.layout.post_in_list, par, false);

                TextView authorText = (TextView) view.findViewById(R.id.post_author);
                String authorStr = "@" + st.getUser().getScreenName();
                authorText.setText(authorStr);

                TextView contentText = (TextView) view.findViewById(R.id.post_content);
                contentText.setText(st.getText());

                TextView dateText = (TextView) view.findViewById(R.id.post_date);
                String dateStr = SIMPLE_DATE_FORMAT.format(st.getCreatedAt());
                dateText.setText(dateStr);

                TextView statusIdText = (TextView) view.findViewById(R.id.post_index);
                statusIdText.setText(Long.toString(pos));

                return view;
            }
        };
        lv.setOnItemClickListener(this);
        lv.setAdapter(twAdapter);

        statusesHandler = new Handler(new StatusesHandlerCallback(ctx, twAdapter));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (twitter.getAuthorization().isEnabled() && statusList.size() == 0) {
            new GetFriendshipThread().start();
            new UserStatusesThread().start();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.user_follow) {
            new UserFollowToggleThread(user.getId(), friendship.isSourceFollowingTarget())
                    .start();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        // a post in list is clicked
        Status status = statusList.get(pos);
        if (status.getId() != -2) { // go to the detail
            if (status.isRetweet()) status = status.getRetweetedStatus();
            Intent detailIntent = new Intent(this, TweetDetailActivity.class);
            detailIntent.putExtra(TweetDetailActivity.STATUS_KEY, status);
            startActivity(detailIntent);
            return;
        }

        // [status.getId() == -2] -> continue button
        long maxId = ((ContinueItem) status).maxId;
        new ExtraUserStatusesThread(maxId, pos).start();
    }

    private class UserStatusesThread extends Thread {
        @Override public void run() {
            try {
                ResponseList<Status> stt = twitter.getUserTimeline(user.getId());
                Message msg = statusesHandler
                        .obtainMessage(StatusesHandlerCallback.MSG_LATEST_SUCCESS, stt);
                statusesHandler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = statusesHandler
                        .obtainMessage(StatusesHandlerCallback.MSG_LATEST_FAILED);
                statusesHandler.sendMessage(msg);
            }
        }
    }
    private class ExtraUserStatusesThread extends Thread {
        public final long maxId;
        public final int pos;

        public ExtraUserStatusesThread(long maxId, int pos) {
            this.maxId = maxId;
            this.pos = pos;
        }

        @Override public void run() {
            Paging paging = new Paging().maxId(maxId).count(50);
            try {
                ResponseList<Status> statuses = twitter.getUserTimeline(user.getId(), paging);
                Message msg = statusesHandler
                        .obtainMessage(StatusesHandlerCallback.MSG_EXT_SUCCESS, pos, -1, statuses);
                statusesHandler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = statusesHandler
                        .obtainMessage(StatusesHandlerCallback.MSG_EXT_FAILED);
                statusesHandler.sendMessage(msg);
            }
        }
    }

    private class GetFriendshipThread extends Thread {
        @Override public void run() {
            try {
                friendship = twitter.showFriendship(getMyId(), user.getId());
                Message msg = hdl.obtainMessage(MSG_USER_FRIENDSHIP_SUCCESS, friendship);
                hdl.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = hdl.obtainMessage(MSG_USER_FRIENDSHIP_FAILED);
                hdl.sendMessage(msg);
            }
        }
    }

    private class UserFollowToggleThread extends Thread {
        private final long userId;
        private final boolean nowFollowing;

        UserFollowToggleThread(long userId, boolean nowFollowing) {
            this.userId = userId;
            this.nowFollowing = nowFollowing;
        }

        @Override public void run() {
            try {
                User updUser = nowFollowing ?
                        twitter.destroyFriendship(userId) : twitter.createFriendship(userId);
                Message msg = hdl.obtainMessage(MSG_USER_FOLLOW_SUCCESS, updUser);
                hdl.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = hdl.obtainMessage(MSG_USER_FOLLOW_FAILED);
                hdl.sendMessage(msg);
            }
        }
    }

    private final static int MSG_USER_FOLLOW_SUCCESS = 1420;
    private final static int MSG_USER_FOLLOW_FAILED = 1421;
    private final static int MSG_USER_FRIENDSHIP_SUCCESS = 1430;
    private final static int MSG_USER_FRIENDSHIP_FAILED = 1431;
    private final Handler hdl = new Handler(new Handler.Callback() {
        @Override public boolean handleMessage(Message msg) {
            if (msg.what == MSG_USER_FOLLOW_SUCCESS) {
                Toast.makeText(ctx, "follow the user successfully :)", Toast.LENGTH_LONG);
                new GetFriendshipThread().start();
            } else if (msg.what == MSG_USER_FOLLOW_FAILED) {
                Toast.makeText(ctx, "failed to follow the user :(", Toast.LENGTH_LONG);
            } else if (msg.what == MSG_USER_FRIENDSHIP_SUCCESS) {
                Relationship rel = (Relationship) msg.obj;
                friendship = rel;
                Button followButton = (Button) findViewById(R.id.user_follow);
                followButton.setText(rel.isSourceFollowingTarget() ? "unfollow" : "follow");
            } else if (msg.what == MSG_USER_FRIENDSHIP_FAILED) {
                // there is nothing to do
            }
            return false;
        }
    });
}
