package lite.demo;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ylscat
 *         Date: 2017-02-06 09:28
 */

public class Main extends ListActivity implements AdapterView.OnItemClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView lv = getListView();
        lv.setAdapter(getAdapter());
        lv.setOnItemClickListener(this);
    }

    private ListAdapter getAdapter() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            ArrayList<Map<String, String>> list = new ArrayList<>();
            final String KEY_NAME = "name";
            final String KEY_INTENT = "intent";
            final String THIS = getClass().getName();
            for(ActivityInfo a : info.activities) {
                String intent = a.name;
                if(THIS.equals(intent))
                    continue;
                HashMap<String, String> map = new HashMap<>();
                int index = intent.lastIndexOf('.');
                String name = intent.substring(index + 1);
                map.put(KEY_NAME, name);
                map.put(KEY_INTENT, intent);
                list.add(map);
            }
            Collections.sort(list, new Comparator<Map<String, String>>() {
                @Override
                public int compare(Map<String, String> lhs, Map<String, String> rhs) {
                    return lhs.get(KEY_NAME).compareTo(rhs.get(KEY_NAME));
                }
            });
            return new SimpleAdapter(this, list, android.R.layout.simple_list_item_1,
                    new String[]{KEY_NAME},
                    new int[]{android.R.id.text1});
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Map<String, String> map = (Map<String, String>) parent.getItemAtPosition(position);
        String name = map.get("intent");
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), name);
        startActivity(intent);
    }
}
