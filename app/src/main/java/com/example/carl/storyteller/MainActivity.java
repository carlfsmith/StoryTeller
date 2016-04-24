package com.example.carl.storyteller;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;

import com.example.scene.db.Scene;
import com.example.scene.db.SceneDBHelper;
import com.example.scene.db.SceneTreeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemLongClickListener, ExpandableListView.OnGroupClickListener,
        ExpandableListView.OnChildClickListener{

    private ExpandableListView expandableListView;
    private ExpandListAdapter expandListAdapter;
    private List<Scene> headerArr; //stores header text
    private List<List<Scene>> allSubHeaders;   //stores subHeader arrays    -----------------------------CONSIDER REMOVING
    private HashMap<Scene, List<Scene>> headToSub;    //relates header to subHeader

    private SceneDBHelper sceneDBHelper;
    private SQLiteDatabase sqlDB;
    private SceneTreeBuilder treeBuilder;

    private AlertDialog dialog;

    private final static String TAG = "MAIN_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        //initialize database
        sceneDBHelper = new SceneDBHelper(MainActivity.this);
        sqlDB = sceneDBHelper.getWritableDatabase();
        sceneDBHelper.init(sqlDB);

        //FOR TESTING------------------------------
//        sceneDBHelper.clear(sqlDB);
        //FOR TESTING------------------------------

        //initialize lists
        headerArr = new ArrayList<>();
        allSubHeaders = new ArrayList<>();
        headToSub = new HashMap<>();

        //initialize exp list view and related objects
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        expandableListView.setOnItemLongClickListener(this);
        expandableListView.setOnGroupClickListener(this);
        expandableListView.setOnChildClickListener(this);
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.main_header, expandableListView, false);
        expandableListView.addHeaderView(header, null, false);
        ////initLists();
        expandListAdapter = new ExpandListAdapter(this, headerArr, headToSub, expandableListView);
        //set expandable list adapter
        expandableListView.setAdapter(expandListAdapter);

        //initialize treeBuilder
        treeBuilder = new SceneTreeBuilder(sceneDBHelper, sqlDB);
        treeBuilder.load(expandListAdapter);

        updateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.sqlDB.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI(){
        expandListAdapter.notifyDataSetChanged();
    }

    private void createHeader(LayoutInflater inflater, int id){
        //because appHead is not part of the header array, set below to -1
        if(id == R.id.appHeader){
            ExpandListAdapter.lastClickedHeadPos = -1;
        }

        View popup = inflater.inflate(R.layout.new_scene_popup, null);
        final EditText eText = (EditText)popup.findViewById(R.id.popup_add_text);
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle);
        AlertDialog.Builder aDBuilder = new AlertDialog.Builder(ctw);
        aDBuilder.setView(popup);
        //can't do this as switch option because AlertDialog has a seperate activity
        aDBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //we're inserting into the header AFTER the one our options are under
                int pos = ExpandListAdapter.lastClickedHeadPos + 1;
                //create new scene
                Scene scene = new Scene(eText.getText().toString());
                System.out.println("HEAD POSITION: " + pos);
                //insert new scene into array and database
                treeBuilder.insertHeader(expandListAdapter, pos, scene);
                updateUI();
            }
        });
        aDBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {}
        });
        dialog = aDBuilder.create();
        dialog.show();
        //stylize alert dialog button text
        Button posBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Button negBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setTextColor(Color.parseColor("#469C5A"));
        negBtn.setTextColor(Color.parseColor("#469C5A"));
        posBtn.setTextSize(20);
        negBtn.setTextSize(20);
        //collapse eListView
        expandListAdapter.collapseLastGroup();
    }

    private void log(String s){
        Log.d(TAG, s);
    }

    //set from XML
    public void onClicked(View v){
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        switch(v.getId()){
            case R.id.new_header_btn:
                //Add new header
                createHeader(inflater, R.id.new_header_btn);

                break;
            case R.id.new_subhead_btn:
                //Add new subHeader
                View popupAlt = inflater.inflate(R.layout.new_scene_popup, null);
                final EditText eText2 = (EditText)popupAlt.findViewById(R.id.popup_add_text);
                ContextThemeWrapper ctw2 = new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle);
                AlertDialog.Builder aDBuilder2 = new AlertDialog.Builder(ctw2);
                aDBuilder2.setView(popupAlt);
                //can't do this as switch option because AlertDialog has a seperate activity
                aDBuilder2.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into index 1 under headPos
                        int headPos = ExpandListAdapter.lastClickedHeadPos;
                        //create new scene
                        Scene scene = new Scene(eText2.getText().toString());
                        System.out.println("HEAD POSITION: " + headPos);
                        //insert new scene into array and database
                        treeBuilder.insertSubHeader(expandListAdapter, headPos, scene);
                        updateUI();
                    }
                });
                aDBuilder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {}
                });
                dialog = aDBuilder2.create();
                dialog.show();
                //stylize alert dialog button text
                Button posBtn2 = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                Button negBtn2 = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                posBtn2.setTextColor(Color.parseColor("#469C5A"));
                negBtn2.setTextColor(Color.parseColor("#469C5A"));
                posBtn2.setTextSize(20);
                negBtn2.setTextSize(20);

                //switch over to new subHead scene branch?

                //collapse eListView
                expandListAdapter.collapseLastGroup();

                break;

            case R.id.appHeader:
                //Add new header underneath appHeader
                createHeader(inflater, R.id.appHeader);

                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        log("onItemLongClick");
        //did not consume the following onClick callback (prevent doing two types of clicks sequentially)
        return true;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        log("onGroupClick");
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        log("onChildClick");
        return false;
    }
}
