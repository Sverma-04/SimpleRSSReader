package com.example.saloni.simplerssreader;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by saloni on 08/11/17.
 */

public class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

    private String urlLink;

    private List<RssFeedModel> mFeedModelList;
    private String mFeedTitle;
    private String mFeedLink;
    private String mFeedDescription;

    @Override
    protected void onPreExecute() {
        MainActivity.mSwipeLayout.setRefreshing(true);
        urlLink = MainActivity.mEditText.getText().toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Boolean doInBackground(Void... voids) {
        if (TextUtils.isEmpty(urlLink))
            return false;

        try {
            if(!urlLink.startsWith("http://") && !urlLink.startsWith("https://"))
                urlLink = "http://" + urlLink;

            URL url = new URL(urlLink);
            try (InputStream inputStream = url.openConnection().getInputStream()) {
                mFeedModelList = parseFeed(inputStream);
            }
            return true;
        } catch (IOException e) {
           Log.e("TAG","error");
        } catch (XmlPullParserException e) {
            Log.e("TAG", "Error", e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        MainActivity.mSwipeLayout.setRefreshing(false);

        if (success) {
            MainActivity.mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
            MainActivity.mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
            MainActivity.mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
            // Fill RecyclerView
            MainActivity.mRecyclerView.setAdapter(new RssFeedListAdapter(mFeedModelList));
        } else {
//            Toast.makeText(MainActivity.this,"Enter a valid Rss feed url",
//                    Toast.LENGTH_LONG).show();
        }
    }

    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException,
            IOException {
        String title = null;
        String link = null;
        String description = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null)
                    continue;

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MyXmlParser", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                if (name.equalsIgnoreCase("title")) {
                    title = result;
                } else if (name.equalsIgnoreCase("link")) {
                    link = result;
                } else if (name.equalsIgnoreCase("description")) {
                    description = result;
                }

                if (title != null && link != null && description != null) {
                    if(isItem) {
                        RssFeedModel item = new RssFeedModel(title, link, description);
                        items.add(item);
                    }
                    else {
                        mFeedTitle = title;
                        mFeedLink = link;
                        mFeedDescription = description;
                    }

                    title = null;
                    link = null;
                    description = null;
                    isItem = false;
                }
            }

            return items;
        } finally {
            inputStream.close();
        }
    }
}
