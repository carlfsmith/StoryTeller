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
import android.widget.TextView;

import com.example.scene.db.Scene;
import com.example.scene.db.SceneDBHelper;
import com.example.scene.db.SceneTreeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener{

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

    private void createPopup(View popup, DialogInterface.OnClickListener listener){
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle);
        AlertDialog.Builder aDBuilder = new AlertDialog.Builder(ctw);
        aDBuilder.setView(popup);
        //can't do this as switch option because AlertDialog has a seperate activity
        aDBuilder.setPositiveButton("Done", listener);
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
                        int headPos = ExpandListAdapter.lastClickedHeadPos + 1;
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        System.out.println("HEAD POSITION: " + headPos);
                        //insert new scene into array and database
                        treeBuilder.insertHeader(expandListAdapter, headPos, scene);
                        updateUI();
                    }
                });

                break;
            case R.id.new_subhead_btn:
                //Add new subHeader
                //change popup title from default
                TextView textView2 = (TextView)popup.findViewById(R.id.textView);
                textView2.setText("New Branch");
                //create popup with unique positive button
                createPopup(popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into index 1 under headPos
                        int headPos = ExpandListAdapter.lastClickedHeadPos;
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        System.out.println("HEAD POSITION: " + headPos);
                        //insert new scene into array and database
                        treeBuilder.insertSubHeader(expandListAdapter, headPos, scene);
                        updateUI();
                    }
                });

                //switch over to new subHead scene branch or no?----------------------------------------------------------<<<<<

                break;

            case R.id.appHeader:
                //Add new header underneath appHeader
                //because appHead is not part of the header array, set below to -1
                ExpandListAdapter.lastClickedHeadPos = -1;
                //create popup with same button as first case
                createPopup(popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //we're inserting into the header AFTER the one our options are under
                        int headPos = ExpandListAdapter.lastClickedHeadPos + 1;
                        //create new scene with trimmed user input
                        Scene scene = new Scene(eText.getText().toString().trim());
                        System.out.println("HEAD POSITION: " + headPos);
                        //insert new scene into array and database
                        treeBuilder.insertHeader(expandListAdapter, headPos, scene);
                        updateUI();
                    }
                });

                break;
        }
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
        //edit scene entry
        final Scene s = (Scene)parent.getItemAtPosition(position);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup, null);
        final EditText eText = (EditText)popup.findViewById(R.id.popup_add_text);
        eText.setText(s.getContent());
        //change popup title from default
        TextView textView = (TextView)popup.findViewById(R.id.textView);
        textView.setText("Edit Scene");
        //create popup with unique positive button
        createPopup(popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                System.out.println("HEAD POSITION: " + position);
                //edit scene in array and database
                treeBuilder.editScene(s, eText.getText().toString());
                updateUI();
            }
        });

        //did not consume the following onClick callback (prevent doing two types of clicks sequentially)
        return true;
    }
}
