package com.example.scene.db;

import android.provider.BaseColumns;

public class SceneContact {
    public static final String DB_NAME = "com.example.scene.db.sceneStorage";
    public static final int DB_VERSION = 3;
    public static String TABLE = "scenes";

    public static abstract class Columns {
        public static final String _ID = BaseColumns._ID;
        public static final String SCENE = "scene";
        public static final String PARENTS = "parents";
        public static final String CHILDREN = "children";
    }
}
