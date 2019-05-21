package lab.graph.AsyncTask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.smartiot.FeedsModel;
import com.loopj.android.http.HttpGet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class JsonAsyncTask extends AsyncTask<String, String, List<FeedsModel>>{

    private Context mContext;
    private JsonAsyncTask.JsonListener mListener;

    public JsonAsyncTask(Context context, JsonAsyncTask.JsonListener listener) {
        mContext = context;
        mListener = listener;
    }

    protected void onPreExecute() {
        super.onPreExecute();
    }

    protected void onPostExecute(List<FeedsModel> feeds) {
        if (mListener == null) {
            return;
        }

        if (feeds.size() > 0) {

            mListener.onStreamResponse(feeds);
        } else {
            mListener.onStreamError(feeds);
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    protected List<FeedsModel> doInBackground(String... strings) {
        String url = strings[0];
        List<FeedsModel> feeds = new ArrayList<>();

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instream = entity.getContent();
                String firstResponse = convertStreamToString(instream);

                JSONObject object = new JSONObject(firstResponse);
                JSONArray feedsObj = object.optJSONArray("feeds");

                instream.close();

                for (int i = 0;i < feedsObj.length(); i++) {
                    try {
                        FeedsModel feed = new FeedsModel();

                        JSONObject obj = (JSONObject) feedsObj.get(i);

                        feed.created_at = obj.optString("created_at");
                        feed.entry_id = obj.optInt("entry_id");
                        feed.field1 = Integer.parseInt(obj.optString("field1"));
                        feed.field2 = Integer.parseInt(obj.optString("field2"));

                        feeds.add(feed);
                    } catch (Exception e) {
                        Log.e("Exception :", "Err " + e);
                    }
                }

                return feeds;
            }


        } catch (Exception e) {
            Log.e("Exception :", "Err " + e);
        }

        return null;
    }

    public interface JsonListener {
        void onStreamResponse(List<FeedsModel> feeds);

        void onStreamError(List<FeedsModel> feeds);
    }
}
