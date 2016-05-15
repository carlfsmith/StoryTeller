package com.example.carl.storyteller;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.scene.db.Scene;
import com.example.scene.db.SceneTreeBuilder;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Carl F. Smith on 4/17/2016.
 *
 * Expands the listAdapter to handle changing views on group or child-view calls
 *
 */
public class ExpandListAdapter extends BaseExpandableListAdapter {
    private List<Scene> headers;
    private HashMap<Scene, List<Scene>> assignSub;
    private Context context;
    private LayoutInflater inflater;
    private ExpandableListView expandableListView;

    private int lastGroupToExpand;

    public static int lastClickedSubHeadPos;

    public static int REGULAR = 0;
    public static int NEW = 1;

    private final static String TAG = "EXPANDABLELIST_ADAPTER";

    class ViewHolder {
        TextView textView;
        View divider;
        ImageButton button;
    }

    ExpandListAdapter(Context context, List<Scene> headers, HashMap<Scene, List<Scene>> assignSub, ExpandableListView eLView){
        this.context = context;
        this.headers = headers;
        this.assignSub = assignSub;
        this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.expandableListView = eLView;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        //expand a group and collapse old group
        if(groupPosition != lastGroupToExpand){
            expandableListView.collapseGroup(lastGroupToExpand);
        }

        super.onGroupExpanded(groupPosition);
        lastGroupToExpand = groupPosition;
    }

    public int getLastGroupToExpand(){
        return this.lastGroupToExpand;
    }

    public void setLastGroupToExpand(int groupPosition){
        this.lastGroupToExpand = groupPosition;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        //flips an internal switch to alert the adapter that a new view is coming into the list
        int id = this.getChild(groupPosition,childPosition).getId();
        if(id != -1){
            return this.REGULAR;
        }
        else{
            return this.NEW;
        }
    }

    @Override
    public int getChildTypeCount() {
        //number of types of views
        return 2;
    }

    //used to select layout type
    @Override
    public int getGroupType(int groupPosition) {
        //flips an internal switch to alert the adapter that a new view is coming into the list
        if(getChildrenCount(groupPosition) > 1){
            return this.REGULAR;
        }
        else{
            return this.NEW;
        }
    }

    @Override
    public int getGroupTypeCount() {
        //number of types of views
        return 2;
    }

    @Override
    //parents size
    public int getGroupCount() {
        return this.headers.size();
    }

    @Override
    //get children size
    public int getChildrenCount(int groupPosition) {
        List<Scene> subH = this.assignSub.get(this.headers.get(groupPosition));
        //prevent crash on null list object when header has no subHeaders
        if(subH != null) {
            return subH.size();
        }
        else{
            return 0;
        }
    }

    public List<Scene> getGroups(){
        return this.headers;
    }

    @Override
    //get header text
    public Scene getGroup(int groupPosition) {
        return this.headers.get(groupPosition);
    }

    public List<Scene> getChildren(int groupPosition) {
        return this.assignSub.get(this.headers.get(groupPosition));
    }

    @Override
    //get subheader text at header
    public Scene getChild(int groupPosition, int childPosition) {
        return this.assignSub.get(this.headers.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Scene scene = getGroup(groupPosition);
        int toInflate;
        ViewHolder holder;

        toInflate = R.layout.header_display;
        //get our view if null and then inflate
        if(convertView == null){
            convertView = inflater.inflate(toInflate, null);
            holder = new ViewHolder();
            holder.textView = (TextView)convertView.findViewById(R.id.header_entry);
            holder.divider = convertView.findViewById(R.id.header_divider);
            holder.button = (ImageButton)convertView.findViewById(R.id.delete_btn);
            //it's ugly putting this here instead of in MainActivity's onClick, but I need the row number for the button in each list item
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //add group delete button
                    SceneTreeBuilder treeBuilder = MainActivity.getTreeBuilder();
                    ExpandListAdapter expandListAdapter = MainActivity.getExpandListAdapter();
                    treeBuilder.deleteHeader(expandListAdapter, groupPosition);
                    notifyDataSetChanged();
                }
            });
            convertView.setTag(holder);
        }
        else{ //get the textView we stored in tag for faster access to textView
            holder = (ViewHolder) convertView.getTag();
        }

        //bind data
        holder.textView.setText(scene.getContent());

        //adjust divider sizes
        if(this.headers.get(groupPosition) != null){
            //show small header divider when there are no subHeaders (1st doesn't count)
            if(this.getChildren(groupPosition).size() <= 1){
                holder.divider.getLayoutParams().height = getDp(context.getResources().getDimension(R.dimen.divider_empty_height));
                holder.divider.setBackgroundColor(context.getResources().getColor(R.color.headerEmpty));
            }
            else{//show large header divider when there are subHeaders
                holder.divider.getLayoutParams().height = getDp(context.getResources().getDimension(R.dimen.divider_filled_height));
                holder.divider.setBackgroundColor(context.getResources().getColor(R.color.headerFull));
            }
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Scene scene = getChild(groupPosition, childPosition);
        int toInflate;
        ViewHolder holder;

        //set add inset scene option view if -1 scene flag is set (scene has no real database ID)
        if(scene.getId() == -1){

            toInflate = R.layout.option_header;
            //get our view if null and then inflate
            if(convertView == null){
                convertView = inflater.inflate(toInflate, null);
            }
        }
        else{//otherwise we're going to inflate the default view
            toInflate = R.layout.subheader_display;
            //get our view if null and then inflate
            if(convertView == null){
                convertView = inflater.inflate(toInflate, null);
                holder = new ViewHolder();
                holder.textView = (TextView)convertView.findViewById(R.id.subheader_entry);
                holder.divider = null;
                convertView.setTag(holder);
            }
            else{ //get the textView we stored in tag for faster access to textView
                holder = (ViewHolder) convertView.getTag();
            }
            //bind data
            holder.textView.setText(scene.getContent());
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private int getDp(float px){
        return (int)(px / context.getResources().getDisplayMetrics().density);
    }

    public void addToGroup(Scene group){
        this.headers.add(group);
    }

    public void setHeadToChildren(int groupPosition, List<Scene> children){
        this.assignSub.put(getGroup(groupPosition), children);
    }

    public void collapseLastGroup(){
        expandableListView.collapseGroup(lastGroupToExpand);
    }

    public void expandGroup(int group){
        if(group > -1 && group < getGroupCount())
            expandableListView.expandGroup(group);
    }

    public void swapGroupWithChild(int groupPosition, int childPosition){
        Scene subScene = getChild(groupPosition, childPosition);
        Scene header = getGroup(groupPosition);
        List<Scene> subScenes = getChildren(groupPosition);
        //remove hash
        assignSub.remove(header);
        //remove subScene from subLists and inject header
        subScenes.remove(childPosition);
        subScenes.add(childPosition, header);
        //remove header from array and insert subscene
        this.headers.remove(groupPosition);
        this.headers.add(groupPosition, subScene);
        //hash
        assignSub.put(subScene, subScenes);
    }

    public void trimSubList(int startPos){
        //start at the last item and end at startPos
        for(int i = headers.size() - 1; i >= startPos; i--){
            assignSub.remove(getGroup(i));
            headers.remove(i);
        }
    }

    public void removeGroup(int groupPosition){
        assignSub.remove(getGroup(groupPosition));
        headers.remove(groupPosition);
    }

    public void removeChild(int groupPosition, int childPosition){
        getChildren(groupPosition).remove(childPosition);
    }

    public void swapGroups(int group1Position, int group2Position){
        Scene scene1 = getGroup(group1Position);
        Scene scene2 = getGroup(group2Position);
        headers.remove(group1Position);
        this.headers.add(group1Position, scene2);
        headers.remove(group2Position);
        this.headers.add(group2Position, scene1);
    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
