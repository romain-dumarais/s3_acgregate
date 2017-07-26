package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to implement statement structure in the S3 Access Control Procedure
 * note that resource can be either a bucket, a CRDT type, or set of object keys
 * @author romain-dumarais
 */
public class S3Statement {
    private final boolean effect;
    private final List<String> principals;
    private final List<String> actions;
    private final List<String> resourcesList;
    private final ByteString resourcebucket;
    private final String conditionBlock;

    /**
     * creates a Statement object with
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions TODO : Romain
     * @param resourceBucket bucket of the following resources 
     * @param resources list of objects for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<String> actions, ByteString resourceBucket, List<String> resources, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resourcesList=resources;
        this.resourcebucket=resourceBucket;
    }
    
    /**
     * 
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions TODO : Romain
     * @param bucketKey name of the bucket for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<String> actions, ByteString bucketKey, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resourcesList=null;
        this.resourcebucket=bucketKey;
    }
    
    /**
     * 
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions TODO : Romain
     * @param resourcetype type of objects for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<String> actions, AntidotePB.CRDT_type resourcetype, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resourcesList=Arrays.asList(resourcetype.toString());
        this.resourcebucket=null;
        throw new UnsupportedOperationException("resourcetype not parsed yet");//TODO : Romain
    }

    public S3Statement() {
        throw new UnsupportedOperationException("Not supported yet.");//TODO : Romain : do I need this ?
    }
    
    public JsonObject encode(){
        JsonArray principalsJson = Json.array();
        principals.stream().forEach((princip) -> {principalsJson.add(princip);});
        
        JsonArray actionsJson = Json.array();
        actions.stream().forEach((action) -> {actionsJson.add(action);});
        
        JsonArray resourcesObjectsJson = Json.array();
        if(resourcesList!=null){resourcesList.stream().forEach((resource) -> {resourcesObjectsJson.add(resource);});}
        
        JsonObject resourcesJson = Json.object();
        if(resourcebucket!=null){resourcesJson.add("bucket", resourcebucket.toStringUtf8());}
        resourcesJson.add("objects",resourcesObjectsJson);
        //resourcesJson.add("resourceType", "");//TODO : Romain
        
        JsonObject statementJson  = Json.object();
        //statementJson.add("Sid", 1);//TODO : Romain
        statementJson.add("Effect", effect);
        statementJson.add("Principals", principalsJson);
        statementJson.add("Actions", actionsJson);
        statementJson.add("Resources", resourcesJson);
        if(!conditionBlock.equals("")){statementJson.add("ConditionBlock",conditionBlock);}
        return statementJson;
    }
    
    /**
     * parses a JSON object to a statement
     * @param statementJson
     * @return 
     */
    public static S3Statement decodeStatic(JsonObject statementJson){
        JsonObject value = statementJson;
        boolean effect;
        List<String> principals = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> resourcesList = new ArrayList<>();
        ByteString resourcebucket;
        String conditionBlock;
        
        effect = value.get("Effect").asBoolean();
        JsonArray principalsJson = value.get("Principals").asArray();
        JsonArray actionsJson = value.get("Actions").asArray();
        JsonObject resourcesJson = value.get("Resources").asObject();
        JsonArray resourcesListJson=new JsonArray();
        try{resourcesListJson = resourcesJson.get("objects").asArray();
        }catch(NullPointerException e){}
        resourcebucket = ByteString.copyFromUtf8(resourcesJson.get("bucket").asString());
        //TODO : Romain : add resourceType
        try{conditionBlock = value.get("ConditionBlock").asString();
        }catch(NullPointerException e){conditionBlock="";}
        //parse Json Arrays to list
        for (JsonValue princip : principalsJson) {principals.add(princip.asString());}
        for(JsonValue action : actionsJson) {actions.add(action.asString());}
        if(!resourcesListJson.isEmpty()){
        for(JsonValue resource : resourcesListJson) {resourcesList.add(resource.asString());}
        }
        if(!resourcesList.isEmpty()){
            return new S3Statement(effect, principals, actions, resourcebucket, resourcesList, conditionBlock);
        }else{
            return new S3Statement(effect, principals, actions, resourcebucket, conditionBlock);
        }
    }
    
    //getters
    public boolean getEffect(){
        return this.effect;
    }
    public List<String> getPrincipals(){
        return this.principals;
    }
    public List<String> getActions(){
        return this.actions;
    }
    public List<String> getResources(){
        return this.resourcesList;
    }
    /**
     * may be null
     */
    public ByteString getResourceBucket(){
        return this.resourcebucket;
    }
    /**
     * may be null
     */
    public String getConditionBlock(){
        return this.conditionBlock;
    }
}
