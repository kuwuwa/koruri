package com.k3ntaroo.koruri;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineActivity extends KoruriTwitterActivity {
    private final static String TAG = TimelineActivity.class.getName();

    private final String STATUS_LIST_KEY = TAG + "." + "STATUS_LIST";

    private ArrayAdapter<Status> tlAdapter;
    private List<Status> statusList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timeline);

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
                dateText.setText(dateStr);
                return view;
            }
        };
        ListView lv = (ListView) findViewById(R.id.timeline);
        lv.setAdapter(tlAdapter);
        updateHomeTimeline();
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

    private final static int MSG_TL = 1115;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override public boolean handleMessage (Message msg) {
            if (msg.what == MSG_TL) {
                ResponseList<Status> tl = (ResponseList<Status>) msg.obj;
                for (Status st : tl) {
                    Log.d(TAG, st.getUser().getScreenName() + ":" + st.getText());
                }

                if (statusList == null || statusList.size() == 0) {
                    tlAdapter.addAll(tl);
                } else {
                    for (int j = tl.size()-1; j >= 0; --j) {
                        tlAdapter.insert(tl.get(j), 0);
                    }
                }
            }
            return false;
        }
    });
}

