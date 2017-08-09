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
import java.util.Objects;

/**
 * A class to implement statement structure in the S3 Access Control Procedure
 * note that resource can be either a bucket, a CRDT type, or set of object keys
 * @author romain-dumarais
 */
public class S3Statement {
    private final boolean effect;
    private final List<String> principals;
    private final List<S3Operation> actions; //TODO : Romain : use an ENUM type
    private final List<String> resourcesList;
    private final ByteString resourcebucket;
    private final String conditionBlock;

    /**
     * creates a Statement object with
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions the type of actions performed
     * @param resourceBucket bucket of the following resources 
     * @param resources list of objects for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<S3Operation> actions, ByteString resourceBucket, List<String> resources, String conditionBlock){
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
     * @param actions the type of actions performed
     * @param bucketKey name of the bucket for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<S3Operation> actions, ByteString bucketKey, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resourcesList=Arrays.asList("*");
        this.resourcebucket=bucketKey;
    }
    
    /**
     * 
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions the type of actions performed
     * @param resourcetype type of objects for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<S3Operation> actions, AntidotePB.CRDT_type resourcetype, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resourcesList=Arrays.asList(resourcetype.toString());
        this.resourcebucket=null;
        throw new UnsupportedOperationException("resourcetype not parsed yet");//TODO : Romain
    }

    /**
     * encode Statement as a JSON object
     * @return jsonStatement
     */
    public JsonObject encode(){
        JsonArray principalsJson = Json.array();
        principals.stream().forEach((princip) -> {principalsJson.add(princip);});
        
        JsonArray actionsJson = Json.array();
        actions.stream().forEach((action) -> {actionsJson.add(action.toString());});
        
        JsonArray resourcesObjectsJson = Json.array();
        if(resourcesList!=null){resourcesList.stream().forEach((resource) -> {resourcesObjectsJson.add(resource);});}
        
        JsonObject resourcesJson = Json.object();
        if(resourcebucket!=null){resourcesJson.add("bucket", resourcebucket.toStringUtf8());}
        resourcesJson.add("objects",resourcesObjectsJson);
        //resourcesJson.add("resourceType", "");//TODO : Romain
        
        JsonObject statementJson  = Json.object();
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
        List<S3Operation> actions = new ArrayList<>();
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
        for(JsonValue action : actionsJson) {actions.add(S3Operation.valueOf(action.asString()));}
        if(!resourcesListJson.isEmpty()){
        for(JsonValue resource : resourcesListJson) {resourcesList.add(resource.asString());}
        }
        if(!resourcesList.isEmpty()){
            return new S3Statement(effect, principals, actions, resourcebucket, resourcesList, conditionBlock);
        }else{
            return new S3Statement(effect, principals, actions, resourcebucket, conditionBlock);
        }
    }
    
    @Override
    public boolean equals(Object o){
        if(!o.getClass().equals(this.getClass())){return false;}
        else{boolean isEqual;
        S3Statement remoteStatement = (S3Statement) o;
        isEqual = this.effect==remoteStatement.getEffect()  
                && this.resourcebucket.equals(remoteStatement.getResourceBucket()) && this.actions.equals(remoteStatement.getActions())
                && this.principals.equals(remoteStatement.getPrincipals()) && this.resourcesList.equals(remoteStatement.getResources());
        if(this.conditionBlock==null){
            isEqual = isEqual && (remoteStatement.getConditionBlock()==null);
        }else{
            isEqual = isEqual && this.conditionBlock.equals(remoteStatement.getConditionBlock());
        }/*
        if(this.resourcesList.isEmpty()){
            isEqual = isEqual && ostatement.getResources().isEmpty();
        }else{
            isEqual = isEqual && this.resourcesList.equals(ostatement.getResources());
        }*/
        return isEqual;}
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.effect ? 1 : 0);
        hash = 29 * hash + Objects.hashCode(this.principals);
        hash = 29 * hash + Objects.hashCode(this.actions);
        hash = 29 * hash + Objects.hashCode(this.resourcesList);
        hash = 29 * hash + Objects.hashCode(this.resourcebucket);
        hash = 29 * hash + Objects.hashCode(this.conditionBlock);
        return hash;
    }
    
    //getters
    public boolean getEffect(){
        return this.effect;
    }
    public List<String> getPrincipals(){
        return this.principals;
    }
    public List<S3Operation> getActions(){
        return this.actions;
    }
    public List<String> getResources(){
        return this.resourcesList;
    }
    /**
     * @return resourceBucket may be empty
     */
    public ByteString getResourceBucket(){
        return this.resourcebucket;
    }
    /**
     * @return conditionBlock may be null
     */
    public String getConditionBlock(){
        return this.conditionBlock;
    }
}
