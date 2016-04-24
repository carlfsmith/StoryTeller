package com.example.scene.db;

import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.provider.BaseColumns._ID;

/**
 * Created by Carl on 4/17/2016.
 */
public class Scene implements Serializable{

    private String content; //user story scene text
    private List<Integer> parents;
    private List<Integer> children;
    private int id; //stores ID from database

    private static final long serialVersionUID = 6470090944622776147L;

    public Scene(){
        this.content = null;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.id = -1;
    }

    public Scene(String content){
        this.content = content;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.id = -1;
    }

    public Scene(String content, int id){
        this.content = content;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.id = id;
    }

    public Scene(String content, List<Integer> parents, List<Integer> children, int id){
        this.content = content;
        this.parents = parents;
        this.children = children;
        this.id = id;
    }

    public Scene(Scene s){
        this.content = s.getContent();
        this.parents = s.getParents();
        this.children = s.getChildren();
        this.id = s.getId();
    }

    public String getContent() {
        if(this.content == null){
            return "";
        }
        else{
            return content;
        }
    }

    public void set(Scene s){
        this.content = s.getContent();
        this.parents = s.getParents();
        this.children = s.getChildren();
        this.id = s.getId();
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Integer> getParents() {
        return parents;
    }

    public void addParent(Scene parent){
        if(parent != null) {
            Integer id = new Integer(parent.getId());
            this.parents.remove(id);
            this.parents.add(id);
        }
    }

    public void removeParent(Scene parent){
        if(parent != null) {
            Integer id = new Integer(parent.getId());
            this.parents.remove(id);
        }
    }

    public void setParents(List<Integer> parents) {
        this.parents = parents;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public void addChild(Scene child){
        if(child != null){
            Integer id = new Integer(child.getId());
            this.children.remove(id);
            this.children.add(id);
        }
    }

    public void removeChild(Scene child){
        if(child != null){
            Integer id = new Integer(child.getId());
            this.children.remove(id);
        }
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String parentsToString(){
        String str = null;
        if(this.parents.size() > 0){
            Iterator<Integer> it = this.parents.iterator();
            str = it.next().toString();
            while(it.hasNext()){
                str = str.concat("," + it.next().toString());
            }
        }
        else{
            str = "";
        }
        return str;
    }

    public String childrenToString(){
        String str = null;
        if(this.children.size() > 0){
            Iterator<Integer> it = this.children.iterator();
            str = it.next().toString();
            while(it.hasNext()){
                str = str.concat("," + it.next().toString());
            }
        }
        else{
            str = "";
        }
        return str;
    }

    public void setParentsFromString(String str){
        if(!str.isEmpty() && this.parents.isEmpty()) {
            String[] strArr = str.split(",");
            for (int i = 0; i < strArr.length; i++){
                this.parents.add(new Integer(strArr[i]));
            }
        }
        else if(!this.parents.isEmpty()){
            Log.d("SCENE", "Attempting to setParentsFromString but parents arrayList isn't empty");
        }
    }

    public void setChildrenFromString(String str){
        if(!str.isEmpty() && this.children.isEmpty()) {
            String[] strArr = str.split(",");
            for (int i = 0; i < strArr.length; i++){
                this.children.add(new Integer(strArr[i]));
            }
        }
        else if(!this.children.isEmpty()){
            Log.d("SCENE", "Attempting to setChildrenFromString but children arrayList isn't empty");
        }
    }
}
