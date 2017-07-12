package com.k3ntaroo.koruri;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by kentaro on 7/12/17.
 */

public class TweetDetailActivity extends KoruriTwitterActivity {
    private final static String className = TweetDetailActivity.class.getName();
    public final static String STATUS_ID_KEY = className + "#status_id";
    public final static String AUTHOR_KEY = className + "#author";
    public final static String CONTENT_KEY = className + "#content";
    public final static String DATE_KEY = className + "#date";

    private Long statusId = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        statusId = getIntent().getLongExtra(STATUS_ID_KEY, -1);
        String author = getIntent().getStringExtra(AUTHOR_KEY);
        String content = getIntent().getStringExtra(CONTENT_KEY);
        long date = getIntent().getLongExtra(DATE_KEY, -1);

        if (author == null || content == null || date == -1) {
            Toast.makeText(this, "failed to show the tweet detail", Toast.LENGTH_SHORT);
            finish();
        }

        setContentView(R.layout.post_detail);
        TextView authorView = (TextView) findViewById(R.id.post_author);
        authorView.setText(author);

        TextView contentView = (TextView) findViewById(R.id.post_content);
        contentView.setText(content);

        TextView dateView = (TextView) findViewById(R.id.post_date);
        dateView.setText(SIMPLE_DATE_FORMAT.format(date));
    }
}
