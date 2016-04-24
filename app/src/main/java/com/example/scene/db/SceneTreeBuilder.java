package com.example.scene.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Adapter;
import android.widget.AdapterView;

import com.example.carl.storyteller.ExpandListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.provider.BaseColumns._ID;

/**
 * Created by Carl on 4/17/2016.
 */
public class SceneTreeBuilder {

    private SceneDBHelper dbHelper;
    private SQLiteDatabase db;
    private Scene root;
    private Scene newSceneOptionItem;
    private final static String TAG = "TREE_BUILDER";

    public SceneTreeBuilder(SceneDBHelper dbHelper, SQLiteDatabase db){
        this.dbHelper = dbHelper;
        this.db = db;
        this.newSceneOptionItem = new Scene();
        //setting root
        this.root = new Scene();
        Cursor cursor = dbHelper.search(db, 1, null, null, null);
        //if cursor is empty, create root
        if(cursor.getCount() == 0){
            this.root.setId(this.dbHelper.insert(
                    this.db,
                    this.root.getContent(),
                    this.root.parentsToString(),
                    this.root.childrenToString()
            ));
        }//get root from DB then set
        else{
            cursor.moveToFirst();
            this.root.setId(
                    cursor.getInt(cursor.getColumnIndexOrThrow(SceneContact.Columns._ID))
            );
            this.root.setContent(
                    cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.SCENE))
            );
            this.root.setParentsFromString(
                    cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.PARENTS))
            );
            this.root.setChildrenFromString(
                    cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.CHILDREN))
            );
        }
        cursor.close();

        this.dBTEST();
    }

    public void load(ExpandListAdapter eLAdapter){
        /*
        * This method is used to set the first header and subHeader lists for viewing. Headers are
        * taken by simply getting the leftmost children from the database (lowest children array
        * index being the "leftmost"). Subheaders are determined by header siblings, so when we
        * start, we'll get the root at SQL ID = 1 and then query for its children, store the
        * leftmost as a header and the rest as associated subheaders. Repeat process with each new
        * header's children.
         */

        Cursor cursor;
        //this array will change size as we decend the "tree" -------------------------Consider converting to stack if time allows
        List<Integer> childrenToCheck = new ArrayList<>(this.root.getChildren());
        System.out.println("%%%%%%%%Children IDs to Check from Root: " + childrenToCheck.toString());

        //query for children
        while(childrenToCheck.size() > 0){
            Log.d(TAG, "----children to check: "+childrenToCheck.size()+" IDs:"+childrenToCheck.toString());
            cursor = this.dbHelper.searchForIDs(this.db, childrenToCheck);
            List<Integer> tempChildren = new ArrayList<>();
            List<Scene> targetSubHeader = new ArrayList<>();
            Boolean firstChild = true;
            if (cursor != null) {
                cursor.moveToFirst();
                Log.d(TAG, "Loading: total tasks = " + cursor.getCount());
                //populate lists
                while (!cursor.isAfterLast()) {
                    //get child scene
                    Log.d(TAG, "Loading task " + (cursor.getInt(cursor.getColumnIndexOrThrow(_ID))-1));
                    Scene scene = new Scene();
                    scene.setId(
                            cursor.getInt(cursor.getColumnIndexOrThrow(SceneContact.Columns._ID))
                    );
                    scene.setContent(
                            cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.SCENE))
                    );
                    scene.setParentsFromString(
                            cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.PARENTS))
                    );
                    scene.setChildrenFromString(
                            cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.CHILDREN))
                    );
                    Log.d(TAG, "      "+scene.getContent()+":"+scene.childrenToString());
                    //store the first scene as a header and save next group of children to load
                    if(firstChild){
                        tempChildren.addAll(scene.getChildren());
                        eLAdapter.addToGroup(scene);
                        firstChild = false;
                    }
                    else{   //store siblings as subHeaders
                        targetSubHeader.add(scene);
                    }
                    cursor.moveToNext();
                }
                cursor.close();

                //update childrenToCheck
                childrenToCheck.clear();
                childrenToCheck.addAll(tempChildren);
                tempChildren.clear();

                //new scene option to subList
                if(targetSubHeader.size() > 0){
                    targetSubHeader.add(0, this.newSceneOptionItem);
                }
                else{
                    targetSubHeader.add(this.newSceneOptionItem);
                }

                //hash header and subHeaders
                eLAdapter.setHeadToChildren(eLAdapter.getGroupCount() - 1, targetSubHeader);
            }
        }



        for(int i = 0; i < eLAdapter.getGroupCount(); i++){
            System.out.println(">>>>>>>>>>>>>>>>>>elem:"+(i+1)+
                    "  id:"+eLAdapter.getGroup(i).getId()+
                    "  content:"+eLAdapter.getGroup(i).getContent()+
                    "  parents:"+eLAdapter.getGroup(i).parentsToString()+
                    "  children:"+eLAdapter.getGroup(i).childrenToString());
        }

    }

    public void insertHeader(ExpandListAdapter eLAdapter, int position, Scene scene){
        /*
        * inserting a new header means adding a child to the header and migrating the header's child
        * to newHeader -or less if one is missing. The headers list stores all of the header scenes
        * displayed in the expandable list and the int position is the target location in the array
        * to insert the scene. Also, because the way the UI is designed, every new header needs
        * to have an associated subHeader list.
         */
        List<Scene> headers = eLAdapter.getGroups();

        Scene parent;
        Scene newHeader;
        List<Scene> newSubList = new ArrayList<>();
        Scene migratingChild = null;

        //add scene to array
        if(headers.size() > position){
            //there is an index after pos to insert to
            headers.add(position, scene);
        }
        else{//there's no index after pos to return to
            headers.add(scene);
        }
        //set header
        newHeader = headers.get(position);
        //set header's associated subList
        newSubList.add(this.newSceneOptionItem);
        eLAdapter.setHeadToChildren(position, newSubList);

        //if the item has a parent
        if(position - 1 >= 0){
            parent = headers.get(position - 1);
        }
        else{
            parent = this.root;
        }
        /*
        * Check if there is an element after the one at int position in header array.
        * This is done to determine the child scene displayed in the expandable list after the
        * parent header
        */
        if(headers.size() > position + 1){
            migratingChild = headers.get(position + 1);
        }

        //update new header with parent
        newHeader.addParent(parent);

        //handle possible child
        if(migratingChild != null){
            //remove old references
            parent.removeChild(migratingChild);
            migratingChild.removeParent(parent);
            //migrate old child
            newHeader.addChild(migratingChild);
        }

        //add new scene to database, add generated ID to scene
        newHeader.setId(this.dbHelper.insert(
                this.db, newHeader.getContent(),
                newHeader.parentsToString(),
                newHeader.childrenToString()
        ));

        //add newheader as child of parent using its new ID
        parent.addChild(newHeader);

        //add newheader as parent to migratingChild using its new ID
        if(migratingChild != null){
            migratingChild.addParent(newHeader);
        }

        //update parent
        this.dbHelper.updateById(
                this.db,
                parent.getId(),
                null, parent.parentsToString(),
                parent.childrenToString()
        );
        //update migraged child, if any
        if(migratingChild != null) {
            this.dbHelper.updateById(
                    this.db, migratingChild.getId(),
                    null, migratingChild.parentsToString(),
                    migratingChild.childrenToString()
            );
        }

        dBTEST();
    }

    public void insertSubHeader(ExpandListAdapter eLAdapter, int headPos, Scene scene){
        /*
        * inserting a new header means adding a child to the header and migrating the header's child
        * to newHeader -or less if one is missing. The headers list stores all of the header scenes
        * displayed in the expandable list and the insert position is automatically 1 because the
        * option dialog to create a new sub-header is at index 0. headPos is the sibling we're
        * creating the subHead for
         */
        List<Scene> headers = eLAdapter.getGroups();
        List<Scene> subHeaders = eLAdapter.getChildren(headPos);
        Scene parent;
        Scene newSubHeader;

        //if we have more subheaders than just the create new scene entry
        if(subHeaders.size() > 1){
            //add the scene at index 1 and set newSubHeader
            subHeaders.add(1, scene);
            newSubHeader = subHeaders.get(1);
        }
        else{//add the new scene to the end of the subHeaders list and set newSubHeader
            subHeaders.add(scene);
            newSubHeader = subHeaders.get(1);
        }

        //copy over scene
        newSubHeader.set(scene);

        //if the sibling at headPos has a parent at headPos - 1
        if(headPos - 1 >= 0){
            //set parent
            parent = headers.get(headPos - 1);
        }
        else{//otherwise the parent is the root
            parent = this.root;
        }

        //update new subHeader with parent
        newSubHeader.addParent(parent);

        //add new scene to database, add generated ID to scene
        newSubHeader.setId(this.dbHelper.insert(
                this.db, newSubHeader.getContent(),
                newSubHeader.parentsToString(),
                newSubHeader.childrenToString()
        ));

        //add newheader as child of parent using its new ID
        parent.addChild(newSubHeader);

        //update the parent
        this.dbHelper.updateById(
                this.db,
                parent.getId(),
                null, parent.parentsToString(),
                parent.childrenToString()
        );
    }

    //USED FOR TESTING-----------------------------
    public void dBTEST(){
        //this method queries database for all rows, then displays information
        //FOR TESTING---------------------------------------------------
        Cursor cursor = dbHelper.search(db, -1, null, null, null);
        List<Scene> blah = new ArrayList<>();
        //move cursor items to array
        if (cursor != null) {
            cursor.moveToFirst();
            Log.d(TAG, "Loading: total tasks = " + cursor.getCount());
            //populate array
            while (!cursor.isAfterLast()) {
                Scene scene2 = new Scene();
                scene2.setId(
                        cursor.getInt(cursor.getColumnIndexOrThrow(SceneContact.Columns._ID))
                );
                scene2.setContent(
                        cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.SCENE))
                );
                scene2.setParentsFromString(
                        cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.PARENTS))
                );
                scene2.setChildrenFromString(
                        cursor.getString(cursor.getColumnIndexOrThrow(SceneContact.Columns.CHILDREN))
                );
                blah.add(scene2);
                Log.d(TAG, "Loading task " + cursor.getInt(cursor.getColumnIndexOrThrow(_ID)));
                cursor.moveToNext();
            }
            cursor.close();

            for(int i = 0; i < blah.size(); i++){
                System.out.println("------------elem:"+(i+1)+"  id:"+blah.get(i).getId()+"  content:"+blah.get(i).getContent()+"  parents:"+blah.get(i).parentsToString()+"  children:"+blah.get(i).childrenToString());
            }
            //FOR TESTING---------------------------------------------------
        }
    }

    public void delete(Scene scene){

    }

    public void updateEList(List<String> headers, List<List<String>> subHeaders,
                            HashMap<String, List<String>> assignSu, Scene newHead)
    {
        /*
        * to update the headers and subHeaders we will trim the header array at newHead's sibling
        * and trim all relative subHeader items. The new header sub-array items will be determined
        * by selecting left-most child scenes and the relative subHeaders will be determined by each
        * header's siblings.
        * */
    }

    public void editScene(Scene toEdit, String content){
        //reference scene and set trimmed text (remove leading and trailing whitespace)
        toEdit.setContent(content.trim());
        //store in database (only store change to content)
        this.dbHelper.updateById(this.db, toEdit.getId(), toEdit.getContent(), null, null);
    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
