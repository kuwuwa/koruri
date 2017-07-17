package com.k3ntaroo.koruri;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import twitter4j.ResponseList;
import twitter4j.Status;

public class StatusesHandlerCallback implements Handler.Callback {
    public final static int MSG_LATEST_SUCCESS = 1180;
    public final static int MSG_LATEST_FAILED = 1181;
    public final static int MSG_EXT_SUCCESS = 1190;
    public final static int MSG_EXT_FAILED = 1191;

    private final Context ctx;
    private final ArrayAdapter<Status> adapter;

    StatusesHandlerCallback(Context ctx, ArrayAdapter<Status> adapter) {
        this.ctx = ctx;
        this.adapter = adapter;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_LATEST_SUCCESS) {
            final ResponseList<Status> latest = (ResponseList<Status>) msg.obj;

            final Status lastStatus = latest.get(latest.size() - 1);
            final long lastStatusTime = lastStatus.getCreatedAt().getTime();

            if (adapter.getCount() == 0 ||
                    lastStatusTime > adapter.getItem(0).getCreatedAt().getTime()) {
                latest.add(new ContinueItem(lastStatus.getId()-1));
            }

            boolean add = false;
            for (int pos = 0; pos < adapter.getCount(); ++pos) {
                if (!add && lastStatusTime > adapter.getItem(pos).getCreatedAt().getTime())
                    add = true;

                if (add) latest.add(adapter.getItem(pos));
            }
            adapter.clear();
            adapter.addAll(latest);
        } else if (msg.what == MSG_LATEST_FAILED) {
            Toast.makeText(ctx, "failed to get the statuses",
                    Toast.LENGTH_LONG).show();
        } else if (msg.what == MSG_EXT_SUCCESS) {
            final int pos = msg.arg1;
            ResponseList<Status> extras = (ResponseList<Status>) msg.obj;

            adapter.remove(adapter.getItem(pos));

            final Status lastStatus = extras.get(extras.size() - 1);
            int idx = adapter.getCount();
            for (int p = pos; p < adapter.getCount(); ++p) {
                Status st = adapter.getItem(p);
                if (st.getCreatedAt().getTime() < lastStatus.getCreatedAt().getTime()) {
                    idx = p; break;
                }
            }

            for (int c = 0; c < idx-pos; ++c)
                adapter.remove(adapter.getItem(pos));

            for (int c = extras.size()-1; c >= 0; --c)
                adapter.insert(extras.get(c), pos);

            if (idx == pos) {
                final int nextContPos = pos + extras.size();
                final long lastId = adapter.getItem(nextContPos-1).getId();
                adapter.insert(new ContinueItem(lastId-1), nextContPos);
            }
        } else if (msg.what == MSG_EXT_FAILED) {
            Toast.makeText(ctx, "failed to get the extra statuses",
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }
}
