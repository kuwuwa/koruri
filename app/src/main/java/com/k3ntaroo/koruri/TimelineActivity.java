package com.k3ntaroo.koruri;

import android.content.Context;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineActivity extends KoruriTwitterActivity implements View.OnClickListener {
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
                if (view == null) {
                    LayoutInflater li = LayoutInflater.from(getContext());
                    view = li.inflate(R.layout.post, par, false);
                }

                TextView authorText = (TextView)view.findViewById(R.id.post_author);
                TextView contentText = (TextView)view.findViewById(R.id.post_content);
                TextView dateText = (TextView)view.findViewById(R.id.post_date);

                Status st = getItem(pos);
                String authorStr = st.getUser().getScreenName() + "(" + st.getUser().getName() + ")";
                authorText.setText(authorStr);
                contentText.setText(st.getText());

                String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
                String dateStr = new SimpleDateFormat(DATE_PATTERN).format(st.getCreatedAt());
                Log.d(TAG, dateStr);
                dateText.setText(dateStr);
                return view;
            }
        };
        final ListView lv = (ListView) findViewById(R.id.timeline);
        lv.setAdapter(tlAdapter);
        updateHomeTimeline();

        // tweet
        final Button tweetButton = (Button) findViewById(R.id.timeline_tweet_button);
        tweetButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.timeline_tweet_button) {
            final EditText tweetText = (EditText) findViewById(R.id.timeline_tweet_text);
            Thread th = new UpdateStatusThread(tweetText.getText().toString());
            th.start();
        }
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
                Message msg = handler.obtainMessage(UPD_STATUS, RESULT_OK);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                // failed to update status
            }
        }
    }

    private Context ctx = this;

    private final static int MSG_TL = 1115;
    private final static int UPD_STATUS = 1116;
    private Handler handler = new Handler(new Handler.Callback() {

        @Override public boolean handleMessage (Message msg) {
            if (msg.what == MSG_TL) {
                ResponseList<Status> newStList = (ResponseList<Status>) msg.obj;

                RateLimitStatus limitStatus = newStList.getRateLimitStatus();
                limitStatus.getLimit();

                String newTitle =
                        "UPDATE(" + limitStatus.getRemaining() + "/" + limitStatus.getLimit() + ")";
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
                if (result == RESULT_OK) {
                    final EditText tweetText = (EditText) findViewById(R.id.timeline_tweet_text);
                    Toast.makeText(
                            ctx,
                            getString(R.string.tweet_sent_succesfully),
                            Toast.LENGTH_SHORT).show();

                    tweetText.setText(""); // clear
                }
            }
            return false;
        }
    });

}

