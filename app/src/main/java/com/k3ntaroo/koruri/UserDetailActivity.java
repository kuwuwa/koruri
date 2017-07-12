package com.k3ntaroo.koruri;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

public class UserDetailActivity extends KoruriTwitterActivity {
    private final static String className = UserDetailActivity.class.getName();
    public final static String USER_KEY = className + "#user";

    private final Context ctx = this;

    private User user = null;

    private ArrayAdapter<Status> twAdapter;
    private List<Status> statusList = new ArrayList<>();

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

        // user's status
        ListView lv = (ListView) findViewById(R.id.user_statuses);
        twAdapter = new ArrayAdapter<Status>(this, 0, statusList) {
            @Override
            public
            @NonNull
            View getView(int pos, @Nullable View view, ViewGroup par) {
                if (null == view) {
                    LayoutInflater li = LayoutInflater.from(getContext());
                    view = li.inflate(R.layout.post_in_list, par, false);
                }

                Status st = getItem(pos);

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
        lv.setAdapter(twAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (twitter.getAuthorization().isEnabled() && statusList.size() == 0) {
            Log.d(className, "user statuses");
            new UserStatusesThread().start();
        }
    }

    private class UserStatusesThread extends Thread {
        @Override public void run() {
            try {
                ResponseList<Status> stt = twitter.getUserTimeline(user.getId());
                Message msg = hdl.obtainMessage(MSG_USER_STATUSES_SUCCESS, stt);
                hdl.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = hdl.obtainMessage(MSG_USER_STATUSES_FAILED);
                hdl.sendMessage(msg);
            }
        }
    }

    private final static int MSG_USER_STATUSES_SUCCESS = 1410;
    private final static int MSG_USER_STATUSES_FAILED = 1411;
    private final Handler hdl = new Handler(new Handler.Callback() {
        private final static int ONE_MILLISEC_PER_SEC = 1000;
        @Override public boolean handleMessage(Message msg) {
            if (msg.what == MSG_USER_STATUSES_SUCCESS) {
                ResponseList<Status> newStList = (ResponseList<Status>) msg.obj;

                RateLimitStatus limitStatus = newStList.getRateLimitStatus();

                long nowTime = Calendar.getInstance().getTimeInMillis();
                long delay = ONE_MILLISEC_PER_SEC * limitStatus.getSecondsUntilReset();
                String resetDateStr = SIMPLE_DATE_FORMAT.format(new Date(nowTime + delay));


                for (Status st : newStList) {
                    Log.d(className, st.getUser().getScreenName() + ":" + st.getText());
                }

                if (statusList == null || statusList.size() == 0) {
                    Log.d(className, "empty statuslist");
                    twAdapter.addAll(newStList);
                } else {
                    final Status ls = newStList.get(newStList.size() - 1);
                    final long lsTime = ls.getCreatedAt().getTime();
                    boolean add = false;
                    for (int j = 0; j < twAdapter.getCount(); ++j) {
                        if (!add && lsTime > twAdapter.getItem(j).getCreatedAt().getTime()) {
                            add = true;
                        }
                        if (add) {
                            newStList.add(twAdapter.getItem(j));
                        }
                    }
                    twAdapter.clear();
                    twAdapter.addAll(newStList);
                }
            } else if (msg.what == MSG_USER_STATUSES_FAILED) {
                Toast.makeText(ctx, "failed to get the user's statuses",
                        Toast.LENGTH_LONG);
            }
            return false;
        }
    });
}
