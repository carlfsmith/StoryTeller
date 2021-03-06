package com.example.carl.storyteller;

import android.content.DialogInterface;
import android.database.Cursor;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scene.db.Scene;
import com.example.scene.db.SceneContact;
import com.example.scene.db.SceneDBHelper;
import com.example.scene.db.SceneTreeBuilder;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener,
        ExpandableListView.OnChildClickListener, View.OnClickListener, TextView.OnLongClickListener{

    private ExpandableListView expandableListView;
    private static ExpandListAdapter expandListAdapter;
    private List<Scene> headerArr; //stores header text
    private HashMap<Scene, List<Scene>> headToSub;    //relates header to subHeader

    private SceneDBHelper sceneDBHelper;
    private SQLiteDatabase sqlDB;
    private static SceneTreeBuilder treeBuilder;

    private AlertDialog dialog;

    TextView headerText = null;
    String title = null;

    private final static String TAG = "MAIN_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        //initialize database
        sceneDBHelper = new SceneDBHelper(MainActivity.this);
        sqlDB = sceneDBHelper.getWritableDatabase();
        sceneDBHelper.createTableIfNotExists(sqlDB);
        log("im in!: " + SceneContact.TABLE);

        //initialize lists
        headerArr = new ArrayList<>();
        headToSub = new HashMap<>();

        //initialize exp list view and related objects
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        expandableListView.setOnItemLongClickListener(this);
        expandableListView.setOnChildClickListener(this);
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.main_header, expandableListView, false);
        expandableListView.addHeaderView(header, null, false);
        headerText = (TextView) findViewById(R.id.appHeader);
        headerText.setOnLongClickListener(this);
        ////initLists();
        expandListAdapter = new ExpandListAdapter(this, headerArr, headToSub, expandableListView);
        //set expandable list adapter
        expandableListView.setAdapter(expandListAdapter);

        //initialize treeBuilder
        treeBuilder = new SceneTreeBuilder(sceneDBHelper, sqlDB);
        //-1, -1 signifies that we'll be using the root scene as a relative root (it's not part of our list)
        treeBuilder.load(expandListAdapter, -1, -1);

        updateUI();

        //set bundle contents
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            this.title = extras.getString("storyName");
            headerText.setText(this.title);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("lastGroupToExpand", expandListAdapter.getLastGroupToExpand());
        log("SAVING STATE...");
    }


    @Override
    protected void onStop() {
        super.onStop();
        this.sqlDB.close();
        this.sceneDBHelper.close();
        log("stopping MainActivity...");
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

    private void createPopup(View popup, DialogInterface.OnClickListener listener, final Boolean condition){
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle);
        AlertDialog.Builder aDBuilder = new AlertDialog.Builder(ctw);
        aDBuilder.setView(popup);
        //can't do this as switch option because AlertDialog has a seperate activity
        aDBuilder.setPositiveButton("Done", listener);
        aDBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //collapse eListView
                if(!condition)
                    expandListAdapter.collapseLastGroup();
            }
        });
        aDBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //collapse eListView
                if(!condition)
                    expandListAdapter.collapseLastGroup();
            }
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
    }

    private void log(String s){
        Log.d(TAG, s);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        //get reference to subHeader and load new subList
        treeBuilder.load(expandListAdapter, groupPosition, childPosition);
        expandListAdapter.collapseLastGroup();
        updateUI();
        return false;
    }

    @Override
    public void onClick(View v) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup, null);
        //get the holder of the user entered text
        final EditText eText = (EditText)popup.findViewById(R.id.popup_add_text);

        switch(v.getId()){
            case R.id.new_header_btn:
                //Add new header
                //create popup with unique positive button
                createPopup(popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into the header AFTER the one our options are under
                        int headPos = expandListAdapter.getLastGroupToExpand() + 1;
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        //insert new scene into array and database
                        treeBuilder.insertHeader(expandListAdapter, headPos, scene);
                        //collapse eListView
                        expandListAdapter.collapseLastGroup();
                        updateUI();
                    }
                }, false);

                break;
            case R.id.new_subhead_btn:
                //Add new subHeader
                //change popup title from default
                ((TextView)popup.findViewById(R.id.popup_textview)).setText("New Branch");
                //create popup with unique positive button
                createPopup(popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into index 1 under headPos
                        int headPos = expandListAdapter.getLastGroupToExpand();
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        //insert new scene into array and database
                        treeBuilder.insertSubHeader(expandListAdapter, headPos, scene);
                        //load subHeader scene
                        int subPos = expandListAdapter.getChildrenCount(headPos) - 1;
                        treeBuilder.load(expandListAdapter, headPos, subPos);
                        //collapse eListView
                        expandListAdapter.collapseLastGroup();
                        updateUI();
                    }
                }, false);

                //switch over to new subHead scene branch or no?----------------------------------------------------------<<<<<

                break;

            case R.id.appHeader:
                //Add new header underneath appHeader
                //because appHead is not part of the header array, set below to -1
                expandListAdapter.setLastGroupToExpand(-1);
                //create popup with same button as first case
                createPopup(popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into the header AFTER the one our options are under
                        int headPos = expandListAdapter.getLastGroupToExpand() + 1;
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        //insert new scene into array and database
                        treeBuilder.insertHeader(expandListAdapter, headPos, scene);
                        updateUI();
                    }
                }, false);

                break;

            case R.id.deleteStoryBtn:
                sceneDBHelper.deleteTable(sqlDB, SceneContact.TABLE);
                finish();

                break;

            case R.id.move_up_btn:
                //move the current header and subheaders above the header above it (index-1)
                final int headPos = expandListAdapter.getLastGroupToExpand();
                treeBuilder.moveUp(expandListAdapter, headPos);
                //when headPos is greater than 0, collapse
                if(headPos > 0)
                    expandListAdapter.collapseLastGroup();
                expandListAdapter.expandGroup(headPos - 1);
                updateUI();

                //scroll to new position
                expandableListView.clearFocus();
                final int height = expandableListView.getHeight() / 2;
                expandableListView.post( new Runnable() {
                    @Override
                    public void run() {
                        expandableListView.smoothScrollToPositionFromTop(headPos+1, height);
                    }
                });
                break;

            case R.id.move_down_btn:
                //move the current header and subheaders below the header below it.
                //..which is the same as moving the header below up
                final int headPosn = expandListAdapter.getLastGroupToExpand() + 1;
                treeBuilder.moveUp(expandListAdapter, headPosn);
                //only collapse when headPos has an index smaller than the list size
                if(headPosn < expandListAdapter.getGroupCount())
                    expandableListView.collapseGroup(headPosn - 1);
                expandListAdapter.expandGroup(headPosn);
                updateUI();

                //scroll to new position
                expandableListView.clearFocus();
                final int heightt = expandableListView.getHeight() / 2;

                expandableListView.post( new Runnable() {
                    @Override
                    public void run() {
                        //expandableListView.smoothScrollToPosition(headPosn);
                        expandableListView.smoothScrollToPositionFromTop(headPosn + 2, heightt);
                    }
                });
                break;
        }
    }

    public static SceneTreeBuilder getTreeBuilder(){
        return treeBuilder;
    }

    public static ExpandListAdapter getExpandListAdapter(){
        return expandListAdapter;
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
        //edit scene entry
        final Scene s = (Scene)parent.getItemAtPosition(position);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup, null);
        //remove unnecessary cloud UI elements

        final EditText eText = (EditText)popup.findViewById(R.id.popup_add_text);
        eText.setText(s.getContent());
        //change popup title from default
        TextView textView = (TextView)popup.findViewById(R.id.popup_textview);
        textView.setText("Edit Scene");
        //create popup with unique positive button
        createPopup(popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //edit scene in array and database
                treeBuilder.editScene(s, eText.getText().toString());
                updateUI();
            }
        }, false);

        //do not consume the following onClick callback (prevent doing two types of clicks sequentially)
        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup, null);
        //hide unnecessary cloud UI features.
        ((EditText)popup.findViewById(R.id.popup_edit_text)).setVisibility(EditText.GONE);
        ((ImageButton)popup.findViewById(R.id.cloud_btn)).setVisibility(ImageButton.GONE);

        final EditText eText = (EditText)popup.findViewById(R.id.popup_add_text);
        eText.setText(this.title);
        //change popup title from default
        TextView textView = (TextView)popup.findViewById(R.id.popup_textview);
        textView.setText("Change Story's Name");
        //create popup with unique positive button
        createPopup(popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogg, int id) {
                //to be overridden
            }
        }, true);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tablename = scrub(eText.getText().toString());
                String finalTableName = dBScrub(tablename);
                //check if name is unique
                if(isInTables(finalTableName)){
                    //tell user that name selected is invalid
                    Toast.makeText(MainActivity.this, "Story name is already being used",
                            Toast.LENGTH_LONG).show();
                }
                else{
                    title = tablename;
                    sceneDBHelper.renameTable(sqlDB, finalTableName);
                    SceneContact.TABLE = finalTableName;
                    headerText.setText(tablename);
                    updateUI();
                    dialog.dismiss();
                    log("GOOD TO GO WITH: " + title);
                }
            }
        });

        //do not consume the following onClick callback (prevent doing two types of clicks sequentially)
        return true;
    }

    private String scrub(String dirty){
        dirty = dirty.toLowerCase();
        String clean = dirty.replaceAll("\'", "");
        clean = clean.replaceAll("_", " ");
        clean = WordUtils.capitalize(clean);
        return clean;
    }

    private String dBScrub(String dirty){
        dirty = dirty.toLowerCase();
        String clean = dirty.replaceAll(" ", "_");
        clean = "\'"+clean+"\'";
        return clean;
    }

    public Boolean isInTables(String tableName){
        Cursor cursor = sceneDBHelper.getTables(sqlDB);
        //if there are tables in database
        if(cursor != null){
            cursor.moveToFirst();
            //populate list with table names
            while(!cursor.isAfterLast()){
                String tName = "'"+cursor.getString(cursor.getColumnIndex("name"))+"'";
                log("are they equal?: " + tName + "  " + tableName);
                if(tName.equals(tableName)){
                    log("they're equal");
                    return true;
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        else{
            log("no tables to load");
        }

        return false;
    }
}
