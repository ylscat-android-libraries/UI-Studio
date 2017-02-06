package lite.demo.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ylscat
 *         Date: 2017-02-06 10:12
 */

public class Db {
    private static SQLiteDatabase sDb;

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static SQLiteDatabase getDatabase(Context context) {
        if(sDb != null)
            return sDb;
        final String FILE_NAME = "moons.db";
        AssetManager am = context.getAssets();
        File file = context.getDatabasePath(FILE_NAME);
        if(!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = am.open(FILE_NAME);
            os = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            for(int len = is.read(buf); len > 0; len = is.read(buf)) {
                os.write(buf);
            }
        }
        catch (IOException ignore) {}
        finally {
            if(is != null)
                try {
                    is.close();
                } catch (IOException ignore) {}
            if(os != null)
                try {
                    os.close();
                } catch (IOException ignore) {}
        }

        sDb = SQLiteDatabase.openDatabase(file.getPath(),
                null, SQLiteDatabase.OPEN_READWRITE);
        return sDb;
    }
}
