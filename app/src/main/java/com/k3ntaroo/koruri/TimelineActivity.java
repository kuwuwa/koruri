package com.k3ntaroo.koruri;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class TimelineActivity extends KoruriTwitterActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = TimelineActivity.class.getName();

    private final String STATUS_LIST_KEY = TAG + "." + "STATUS_LIST";

    private ArrayAdapter<Status> tlAdapter;
    private List<Status> statusList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timeline);

        // timeline
        tlAdapter = new ArrayAdapter<Status>(this, 0, statusList) {
            @Override
            public @NonNull View getView (int pos, @Nullable View view, ViewGroup par) {
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

        final ListView lv = (ListView) findViewById(R.id.post_in_list);
        lv.setAdapter(tlAdapter);
        lv.setOnItemClickListener(this);

        // tweet
        final Button tweetButton = (Button) findViewById(R.id.timeline_tweet_button);
        tweetButton.setOnClickListener(this);

        if (null == twitter.getConfiguration().getOAuthAccessToken()) {
            Log.d(TAG, "not authorized!");
            // should be restarted after authorization
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (statusList.size() == 0) {
            updateHomeTimeline();
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, Integer.toString(v.getId()));
        if (v.getId() == R.id.timeline_tweet_button) {
            final EditText tweetText = (EditText) findViewById(R.id.timeline_tweet_text);
            Thread th = new UpdateStatusThread(tweetText.getText().toString());
            th.start();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        // a post in list is clicked
        Status status = statusList.get(pos);
        Intent detailIntent = new Intent(this, TweetDetailActivity.class);
        detailIntent.putExtra(TweetDetailActivity.STATUS_KEY, status);
        startActivity(detailIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.timeline_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.update_timeline) {
            updateHomeTimeline();
        } else if(item.getItemId() == R.id.logout) {
            logout();
        }
        return true;
    }

    //// timeline
    private void updateHomeTimeline () {
        Thread th = new GetHomeTimelineThread();
        th.start();
    }


    private class GetHomeTimelineThread extends Thread {
        @Override public void run() {
            try {
                ResponseList<Status> tl = twitter.getHomeTimeline();
                Message msg = handler.obtainMessage(MSG_TL, tl);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                // xx
            }
        }
    }

    // tweet
    private class UpdateStatusThread extends Thread {
        private final String text;

        public UpdateStatusThread(String text) {
            this.text = text;
        }

        @Override public void run() {
            try {
                twitter.updateStatus(this.text);
                Message msg = handler.obtainMessage(UPD_STATUS, UPD_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(UPD_STATUS, UPD_FAIL);
                handler.sendMessage(msg);
            }
        }
    }

    private Context ctx = this;

    private final static int MSG_TL = 1210;
    private final static int UPD_STATUS = 1220;
    private final static int UPD_SUCCESS = 1211;
    private final static int UPD_FAIL = 1212;
    private Handler handler = new Handler(new Handler.Callback() {
        private final static int ONE_MILLISEC_PER_SEC = 1000;

        @Override public boolean handleMessage (Message msg) {
            if (msg.what == MSG_TL) {
                ResponseList<Status> newStList = (ResponseList<Status>) msg.obj;

                RateLimitStatus limitStatus = newStList.getRateLimitStatus();

                long nowTime = Calendar.getInstance().getTimeInMillis();
                long delay = ONE_MILLISEC_PER_SEC * limitStatus.getSecondsUntilReset();
                String resetDateStr = SIMPLE_DATE_FORMAT.format(new Date(nowTime + delay));

                String newTitle =
                        "UPDATE(" + limitStatus.getRemaining() + "/" + limitStatus.getLimit() + ")"
                        + "(reset: " + resetDateStr.substring(11) + ")";
                ActionMenuItemView updMenu = (ActionMenuItemView) findViewById(R.id.update_timeline);
                updMenu.setText(newTitle);

                for (Status st : newStList) {
                    Log.d(TAG, st.getUser().getScreenName() + ":" + st.getText());
                }

                if (statusList == null || statusList.size() == 0) {
                    tlAdapter.addAll(newStList);
                } else {
                    Status prevFirstStatus = tlAdapter.getItem(0);
                    int cutIdx = newStList.size() - 1;
                    for (int j = 0; j < newStList.size(); ++j) {
                        if (prevFirstStatus.getId() == newStList.get(j).getId()) {
                            cutIdx = j;
                            break;
                        }
                    }
                    for (int j = newStList.size() - 1; j >= 0; --j) {
                        tlAdapter.insert(newStList.get(j), 0);
                    }
                }
            } else if (msg.what == UPD_STATUS) {
                int result = (int) msg.obj;
                if (result == UPD_SUCCESS) {
                    final EditText tweetText = (EditText) findViewById(R.id.timeline_tweet_text);
                    Toast.makeText(
                            ctx,
                            getString(R.string.tweet_sent_succesfully),
                            Toast.LENGTH_SHORT).show();

                    tweetText.setText(""); // clear
                } else if (result == UPD_FAIL) {
                    Toast.makeText(
                            ctx,
                            getString(R.string.tweet_sent_unsuccesfully),
                            Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        }
    });
}

