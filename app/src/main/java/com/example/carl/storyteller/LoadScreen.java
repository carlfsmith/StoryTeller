package com.example.carl.storyteller;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.scene.db.SceneContact;
import com.example.scene.db.SceneDBHelper;

import java.util.ArrayList;

/**
 * Provides the load screen view and connects the user to the MainActivity (where their story is
 * displayed).
 */
public class LoadScreen extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        View.OnClickListener{

    private EditText editText;
    private Spinner spinner;

    private SceneDBHelper sceneDBHelper = null;
    private SQLiteDatabase sqlDB = null;

    private ArrayList<String> tables = null;

    private String tableToLoad;

    private static final String TAG = "LoadScreen";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load_screen);

        //initialize database
        sceneDBHelper = new SceneDBHelper(this);
        sqlDB = sceneDBHelper.getWritableDatabase();

        //get table names
        tables = new ArrayList<>();

        //see onResume
    }

    public void loadTables(){
        Cursor cursor = sceneDBHelper.getTables(sqlDB);
        //if there are tables in database
        if(cursor != null){
            cursor.moveToFirst();
            //populate list with table names
            while(!cursor.isAfterLast()){
                String tName = cursor.getString(cursor.getColumnIndex("name"));
                //get substring without quotes
                tName = tName.replaceAll("\'", "");
                tName = tName.replaceAll("_", " ");
                log("got table name: " + tName);
                tables.add(tName);
                cursor.moveToNext();
            }
            cursor.close();
        }
        else{
            log("no tables to load");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!sqlDB.isOpen()){
            sqlDB = sceneDBHelper.getWritableDatabase();
        }

        tables.clear();
        loadTables();
        //close database
        sqlDB.close();

        //set edittext and spinner
        editText = (EditText)findViewById(R.id.load_entertext);
        editText.requestFocus();
        editText.setOnClickListener(this);
        spinner = (Spinner)findViewById(R.id.load_spinner);
        spinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tables);
        spinner.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sqlDB.isOpen())
            sqlDB.close();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //get user selection from spinner
        this.tableToLoad = (String)parent.getItemAtPosition(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.load_newstory_btn:
                String tableName = this.editText.getText().toString().toLowerCase().trim();
                tableName = tableName.replaceAll(" ", "_");
                tableName = tableName.replaceAll("\'", "");
                tableName = "\'"+tableName+"\'";
                log("Create new story with name: " + tableName);
                //check if user input text is valid
                if(tableName.length() > 0){
                    //check if tableName is not in our list of tablenames
                    if(!tables.contains(tableName)){
                        //set tableName
                        SceneContact.TABLE = tableName;
                        //the new activity will generate the new table
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    else{//if it is tell user
                        Toast.makeText(this, "Story name is being used",
                                Toast.LENGTH_LONG).show();
                    }
                    SceneContact.TABLE = tableName;
                }
                else{//ask user for new entry
                    Toast.makeText(this, "Please enter a story name",
                            Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.load_continuestory_btn:
                if(tableToLoad != null && tableToLoad.length() > 0){
                    log("Continue story " + tableToLoad);
                    tableToLoad = tableToLoad.replaceAll(" ", "_");
                    tableToLoad = tableToLoad.replaceAll("\'", "");
                    tableToLoad = "\'"+tableToLoad+"\'";
                    SceneContact.TABLE = tableToLoad;
                    startActivity(new Intent(this, MainActivity.class));
                }//there's nothing to select
                else{
                    Toast.makeText(this, "There is no story to load",
                            Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
