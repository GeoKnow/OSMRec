
package gr.athenainnovation.imis.OSMContainer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Class containing information about the OSM ways.
 * 
 * @author imis-nkarag
 */

public class OSMWay {
    
    private String id;
    private String lang;
    private String action; //e.g modify
    private String visible; 
    private String timestamp;
    private String uid;
    private String user;
    private String version;
    private String changeset;    
    private int classID;
    private Set<Integer> classIDs;   
    private final List<String> nodeReferences = new ArrayList(); //node references  //made final
    private final List<Geometry> nodeGeometries = new ArrayList(); //nodeGeometries   //made final
    private Coordinate[] coordinateList;    
    private final Map<String, String> tags = new HashMap<>();      
    private Geometry geometry;
    private ArrayList vector;
    
    //way attributes getters 
    public String getID(){
        return id;
    }
    
    public String getlang(){
        return lang;
    }

    public String getAction(){
        return action;
    }
    
    public String getVisible(){
        return visible;
    }  
    
    public List<Geometry> getNodeGeometries(){
        return nodeGeometries;
    }
    
    public Coordinate[] getCoordinateList(){       
        coordinateList =  (Coordinate[]) nodeGeometries.toArray();
        return coordinateList;
    }
    
    public Geometry getGeometry(){
        return geometry;
    }
    
    public String getTimestamp(){
        return timestamp;
    }
    
    public String getUid(){
        return uid;
    }    
    
    public String getUser(){
        return user;
    }
 
    public String getVersion(){
        return version;
    }    

    public String getChangeset(){
        return changeset;
    }    
    
    public List<String> getNodeReferences(){
        return nodeReferences;
    }
    
    public int getNumberOfNodes(){
        return this.nodeReferences.size();
    }
    
    public Map<String, String> getTagKeyValue(){
        return tags;
    }
    
    public int getClassID(){
        return this.classID;
    }
    
    public Set<Integer> getClassIDs(){
        return this.classIDs;
    }
    
    public ArrayList<Integer> getVector(){
        return vector;
    }
    
    public void setVector(ArrayList<Integer> vector){
        this.vector = vector;
    }
    
    //way attributes setters
    public void setID(String id){
        this.id = id;
    }
    
    public void setLang(String lang){
        this.lang = lang;
    }
    
    public void setAction(String action){
        this.action = action;
    }
    
    public void setVisible(String visible){
        this.visible = visible;
    }
    
    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }
    
    public void setUid(String uid){
        this.uid = uid;
    }    
    
    public void setUser(String user){
        this.user = user;
    }

    public void setVersion(String version){
        this.version = version;
    }    
 
    public void setChangeset(String changeset){
        this.changeset = changeset;
    } 
    
    public void setTagKeyValue(String tagKey, String tagValue){
        this.tags.put(tagKey, tagValue);
    }
    
    public void addNodeReference(String nodeReference){
        nodeReferences.add(nodeReference);
    }
    
    public void addNodeGeometry(Geometry geometry){
        nodeGeometries.add(geometry);
    }
    
    public void setGeometry(Geometry geometry){       
        this.geometry = geometry;
    }  
    
    public void setClassID(int classID){
        this.classID = classID;
    }

    public void setClassIDs(Set<Integer> classIDs){
        this.classIDs = classIDs;
    }    

}
