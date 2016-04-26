package com.example.scene.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Carl F. Smith on 4/18/2016.
 *
 * SceneDBHeelper assists in injecting data into the database.
 *
 */
public class SceneDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "DB_LIST";

    public SceneDBHelper(Context context){
        super(context, SceneContact.DB_NAME, null, SceneContact.DB_VERSION);
    }

    // when no database exists in disk and the helper class needs to create a new one.
    @Override
    public void onCreate(SQLiteDatabase db){
    }

    public void init(SQLiteDatabase db){
        Log.d(TAG, SceneContact.Columns.CREATE_TABLE);
        db.execSQL(SceneContact.Columns.CREATE_TABLE);
        Log.d(TAG, "database created.");
    }

    public void clearTable(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS " + SceneContact.TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqlDB, int i, int i2){
        sqlDB.execSQL("DROP TABLE IF EXISTS " + SceneContact.TABLE);
        onCreate(sqlDB);
    }

    //used to insert row into the database table
    public int insert(SQLiteDatabase db, String sceneText, String parents, String children){
        ContentValues values = new ContentValues();
        values.put(SceneContact.Columns.SCENE, sceneText);
        values.put(SceneContact.Columns.PARENTS, parents);
        values.put(SceneContact.Columns.CHILDREN, children);
        long id = db.insert(SceneContact.TABLE, null, values);
        return (int)id;
    }

    //used to wipe all rows from the database table
    public void clear(SQLiteDatabase db){
        db.execSQL("DELETE FROM " + SceneContact.TABLE);
    }

    public void delete(SQLiteDatabase db, int id){
        String ID = SceneContact.Columns._ID;
        db.delete(SceneContact.TABLE, ID+"="+id,null);
    }

    //used to get results of table query
    public Cursor search(SQLiteDatabase db, int id, String content, String parents, String children){
        Cursor cursor = null;

        if(id < 1 && content == null && parents == null && children == null){
            cursor = db.query(SceneContact.TABLE,
                    null, null, null, null, null, null);
        }
        else if(id >= 1 && content == null && parents == null && children == null){
            String selection = SceneContact.Columns._ID + " = " + id;
            cursor = db.query(SceneContact.TABLE, null, selection, null, null, null, null);
        }
        return cursor;
    }

    //used to get all entrees with provided IDs
    public Cursor searchForIDs(SQLiteDatabase db, List<Integer> ids){
        String selection;
        String idTag = SceneContact.Columns._ID;

        Iterator<Integer> it = ids.iterator();
        //build selection string
        int idNum = it.next();
        selection = idTag + "=" + idNum;
        while(it.hasNext()){
            idNum = it.next();
            selection = selection + " OR " + idTag + "=" + idNum;
        }

        return db.query(SceneContact.TABLE, null, selection, null, null, null, null);
    }

    //used to update the database table
    public void updateById(SQLiteDatabase db, int id, String sceneText, String parents, String children){
        if(id > 0){
            ContentValues cValues = new ContentValues();
            if(sceneText != null){
                cValues.put(SceneContact.Columns.SCENE, sceneText);
            }
            if(parents != null){
                cValues.put(SceneContact.Columns.PARENTS, parents);
            }
            if(children != null){
                cValues.put(SceneContact.Columns.CHILDREN, children);
            }

            db.update(SceneContact.TABLE, cValues, SceneContact.Columns._ID + "=" + id, null);
        }
    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
