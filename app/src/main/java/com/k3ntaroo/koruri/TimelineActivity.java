package com.k3ntaroo.koruri;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import twitter4j.Paging;
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
                Status st = getItem(pos);
                if (st.getId() == -2) {
                    LayoutInflater li = LayoutInflater.from(getContext());
                    view = li.inflate(R.layout.continue_in_list, par, false);
                    return view;
                }

                LayoutInflater li = LayoutInflater.from(getContext());
                view = li.inflate(R.layout.post_in_list, par, false);


                TextView authorText = (TextView) view.findViewById(R.id.post_author);
                TextView contentText = (TextView) view.findViewById(R.id.post_content);
                TextView dateText = (TextView) view.findViewById(R.id.post_date);

                if (st.isFavorited()) {
                    Log.d(TAG, st.getUser().getScreenName() + ":" + st.getText());
                }
                final int textColor = Color.parseColor(st.isFavorited() ? "#AA4444" : "#000000");
                authorText.setTextColor(textColor);
                contentText.setTextColor(textColor);
                dateText.setTextColor(textColor);

                TextView statusIdText = (TextView) view.findViewById(R.id.post_index);
                statusIdText.setText(Long.toString(pos));

                if (st.isRetweet()) {
                    view.setBackgroundColor(Color.parseColor("#DDFFDD"));
                    Status rtStatus = st.getRetweetedStatus();

                    String authorStr = "@" + rtStatus.getUser().getScreenName() +
                            " (RT by @" + st.getUser().getScreenName() + ")";
                    authorText.setText(authorStr);

                    contentText.setText(rtStatus.getText());

                    String dateStr = SIMPLE_DATE_FORMAT.format(rtStatus.getCreatedAt());
                    dateText.setText(dateStr);
                } else {
                    view.setBackgroundColor(Color.parseColor("#FAFAFA"));
                    String authorStr = "@" + st.getUser().getScreenName();
                    authorText.setText(authorStr);

                    contentText.setText(st.getText());

                    String dateStr = SIMPLE_DATE_FORMAT.format(st.getCreatedAt());
                    dateText.setText(dateStr);
                }

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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (twitter.getAuthorization().isEnabled() && statusList.size() == 0) {
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
        if (status.getId() != -2) { // go to the detail
            if (status.isRetweet()) status = status.getRetweetedStatus();
            Intent detailIntent = new Intent(this, TweetDetailActivity.class);
            detailIntent.putExtra(TweetDetailActivity.STATUS_KEY, status);
            startActivity(detailIntent);
            return;
        }

        // [status.getId() == -2] -> continue
        long maxId = ((ContinueItem) status).maxId;
        new ExtraTweetThread(maxId, pos).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
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
                Message msg = handler.obtainMessage(MSG_TL_FAILED);
                handler.sendMessage(msg);
                // xx
            }
        }
    }

    private class ExtraTweetThread extends Thread {
        public final long maxId;
        public final int pos;

        public ExtraTweetThread(long maxId, int pos) {
            this.maxId = maxId;
            this.pos = pos;
        }

        @Override public void run() {
            Paging paging = new Paging().maxId(maxId).count(50);
            try {
                ResponseList<Status> statuses = twitter.getHomeTimeline(paging);
                Message msg = handler.obtainMessage(MSG_EXT_SUCCESS, pos, -1, statuses);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_EXT_FAILED);
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
                Message msg = handler.obtainMessage(MSG_UPD_SUCCESS);
                handler.sendMessage(msg);
            } catch (TwitterException e) {
                Message msg = handler.obtainMessage(MSG_UPD_FAILED);
                handler.sendMessage(msg);
            }
        }
    }

    private Context ctx = this;

    private final static int MSG_TL = 1210;
    private final static int MSG_TL_FAILED = 1211;
    private final static int MSG_UPD_SUCCESS = 1221;
    private final static int MSG_UPD_FAILED = 1222;
    private final static int MSG_EXT_SUCCESS = 1231;
    private final static int MSG_EXT_FAILED = 1232;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override public boolean handleMessage (Message msg) {
            if (msg.what == MSG_TL) {
                ResponseList<Status> newStList = (ResponseList<Status>) msg.obj;

                RateLimitStatus limitStatus = newStList.getRateLimitStatus();
                refreshUpdateMenu(limitStatus);

                for (Status st : newStList) {
                    Log.d(TAG, st.getUser().getScreenName() + ":" + st.getText());
                }

                if (statusList == null || statusList.size() == 0) {
                    Log.d(TAG, "empty statuslist");
                    tlAdapter.addAll(newStList);
                } else {
                    final Status ls = newStList.get(newStList.size() - 1);
                    final long lsTime = ls.getCreatedAt().getTime();
                    boolean add = false;
                    for (int j = 0; j < tlAdapter.getCount()-1; ++j) {
                        if (!add && lsTime > tlAdapter.getItem(j).getCreatedAt().getTime()) {
                            add = true;
                        }
                        if (add) {
                            newStList.add(tlAdapter.getItem(j));
                        }
                    }
                    tlAdapter.clear();
                    tlAdapter.addAll(newStList);
                }
                long lastId = tlAdapter.getItem(tlAdapter.getCount()-1).getId();
                tlAdapter.add(new ContinueItem(lastId-1));
            } else if (msg.what == MSG_TL_FAILED) {
                Toast.makeText(ctx, "failed in getting your home TL", Toast.LENGTH_SHORT);
            } else if (msg.what == MSG_UPD_SUCCESS) {
                    final EditText tweetText = (EditText) findViewById(R.id.timeline_tweet_text);
                    Toast.makeText(
                            ctx,
                            getString(R.string.tweet_sent_succesfully),
                            Toast.LENGTH_SHORT).show();

                    tweetText.setText(""); // clear
            } else if (msg.what == MSG_UPD_FAILED) {
                Toast.makeText(
                        ctx,
                        getString(R.string.tweet_sent_unsuccesfully),
                        Toast.LENGTH_SHORT).show();
            } else if (msg.what == MSG_EXT_SUCCESS) {
                int pos = msg.arg1;
                ResponseList<Status> extraStatuses = (ResponseList<Status>) msg.obj;

                refreshUpdateMenu(extraStatuses.getRateLimitStatus());

                { Status cs = tlAdapter.getItem(pos); tlAdapter.remove(cs); }

                Status lastExtSt = extraStatuses.get(extraStatuses.size()-1);
                int idx = tlAdapter.getCount();
                for (int j = pos; j < tlAdapter.getCount(); ++j) {
                    Status st = tlAdapter.getItem(j);
                    if (st.getCreatedAt().getTime() < lastExtSt.getCreatedAt().getTime()) {
                        idx = j;
                        break;
                    }
                }

                for (int c = 0; c < idx - pos; ++c) {
                    tlAdapter.remove(tlAdapter.getItem(pos));
                }

                for (int j = extraStatuses.size() - 1; j >= 0; --j) {
                    tlAdapter.insert(extraStatuses.get(j), pos);
                }

                if (idx == pos) {
                    int nextContPos = pos + extraStatuses.size();
                    long lastId = tlAdapter.getItem(nextContPos-1).getId();
                    tlAdapter.insert(new ContinueItem(lastId - 1), nextContPos);
                }
            } else if (msg.what == MSG_EXT_FAILED) {
                Toast.makeText(
                        ctx,
                        "failed to get extra tweets :(",
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    });

    private final static int ONE_MILLISEC_PER_SEC = 1000;

    private void refreshUpdateMenu(RateLimitStatus limitStatus) {
        long nowTime = Calendar.getInstance().getTimeInMillis();
        long delay = ONE_MILLISEC_PER_SEC * limitStatus.getSecondsUntilReset();
        String resetDateStr = SIMPLE_DATE_FORMAT.format(new Date(nowTime + delay));

        String newTitle =
                "UPDATE(" + limitStatus.getRemaining() + "/" + limitStatus.getLimit() + ")"
                        + "(reset: " + resetDateStr.substring(11) + ")";
        ActionMenuItemView updMenu = (ActionMenuItemView) findViewById(R.id.update_timeline);

        if (null != updMenu) {
            updMenu.setText(newTitle);
        }
    }
}

