package lite.demo;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lite.demo.data.Db;
import lite.widget.R;

/**
 * @author ylscat
 *         Date: 2017-01-23 10:06
 */

public class PullLayout extends Activity implements lite.widget.pull.PullLayout.OnRefreshListener {
    private static final int PAGE_SIZE = 100;

    private SimpleAdapter mAdapter;
    private ArrayList<Map<String, String>> mList;
    private lite.widget.pull.PullLayout mPtr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_layout);

        mList = new ArrayList<>();
        mAdapter = new SimpleAdapter(this, mList, R.layout.item,
                new String[]{"name", "host", "diameter"},
                new int[]{R.id.name, R.id.host, R.id.diameter});
        ListView lv = (ListView) findViewById(R.id.list);
        lv.setAdapter(mAdapter);
        mPtr = (lite.widget.pull.PullLayout) lv.getParent();

        mPtr.setOnRefreshListener(this);
    }

    @Override
    public void onPull(boolean header) {
        new AsyncTask<Integer, Object, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
//                try {
//                    Thread.sleep(1000);
//                }
//                catch (InterruptedException ignore) {};
                int start = params[0];
                int count = mList.size();
                if(start > count)
                    return null;

                SQLiteDatabase db = Db.getDatabase(PullLayout.this);
                Cursor c = db.query("moons",
                        new String[]{"name", "host", "diameter"},
                        null,
                        null,
                        null,
                        null,
                        "diameter desc",
                        String.format("%d,%d", start, PAGE_SIZE));
                int n = c.getCount();
                if(count > start) {
                    if(start == 0)
                        mList.clear();
                    else
                        for(int i = count - 1; i >= start; i--)
                            mList.remove(i);
                }

                while (c.moveToNext()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("name", c.getString(0));
                    map.put("host", c.getString(1));
                    map.put("diameter", c.getString(2));
                    mList.add(map);
                }
                c.close();

                return n;
            }

            @Override
            protected void onPostExecute(Integer result) {
                mAdapter.notifyDataSetChanged();
                mPtr.stopLoading();
                if(result != null && result < PAGE_SIZE)
                    mPtr.setFooterPullable(false);
                else
                    mPtr.setFooterPullable(true);
            }
        }.execute(header ? 0 : mList.size());
    }
}
