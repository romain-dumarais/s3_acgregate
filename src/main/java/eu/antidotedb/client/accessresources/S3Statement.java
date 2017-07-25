package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import java.util.Arrays;
import java.util.List;

/**
 * A class to implement statement structure in the S3 Access Control Procedure
 * note that resource can be either a bucket, a CRDT type, or set of object keys
 * @author Romain
 */
public class S3Statement {
    private final boolean effect;
    private final List<String> principals;
    private final List<String> actions;
    private final List<String> resources;
    private final ByteString resourcebucket;
    private final String conditionBlock;
    
    
    /*API
  "Statement": [{
    "Sid": "1",
    "Effect": "Allow",
    "Principal": {"AWS": ["arn:aws:iam::ACCOUNT-ID-WITHOUT-HYPHENS:root"]}, 
    "Action": "s3:*",
    "Resource": [
      "arn:aws:s3:::mybucket",
      "arn:aws:s3:::mybucket/*"
    ]
  }]
    */

    /**
     * creates a Statement object with
     * @param effect {@code true} if the operation should be allowed, {@code false} otherwise
     * @param principals set of users and/or groups that requests the action
     * @param actions TODO : Romain
     * @param resources list of objects for which this statement is effective
     * @param conditionBlock optional condition on userData
     */
    public S3Statement(boolean effect, List<String> principals, List<String> actions, List<String> resources, String conditionBlock){
        this.actions=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.resources=resources;
        this.resourcebucket=null;
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
        this.resources=null;
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
        this.resources=Arrays.asList(resourcetype.toString());
        this.resourcebucket=null;
        throw new UnsupportedOperationException("resourcetype not parsed yet");//TODO : Romain
    }

    public S3Statement() {
        throw new UnsupportedOperationException("Not supported yet.");//TODO : Romain : do I need this ?
    }
    
    public String encode(){
        JsonArray principalsJson = Json.array();
        principals.stream().forEach((princip) -> {principalsJson.add(princip);});
        
        JsonArray actionsJson = Json.array();
        actions.stream().forEach((action) -> {actionsJson.add(action);});
        
        JsonArray resourcesObjectsJson = Json.array();
        resources.stream().forEach((resource) -> {resourcesObjectsJson.add(resource);});
        
        JsonObject resourcesJson = Json.object();
        resourcesJson.add("bucket", resourcebucket.toStringUtf8());
        resourcesJson.add("objects",resourcesObjectsJson);
        resourcesJson.add("resourceType", resourcesObjectsJson);
        JsonObject statementJson  = Json.object();
        statementJson.add("Sid", 1);//TODO : Romain
        statementJson.add("Effect", effect);
        statementJson.add("Principal", principalsJson);
        statementJson.add("Action", actionsJson);
        statementJson.add("Resource", resourcebucket.toStringUtf8()); //TODO : Romain
        statementJson.add("ConditionBlock",conditionBlock);
        return statementJson.toString();
    }
    public static S3Statement decode(String statementJson){
        //TODO : Romain : decode JSON object as statement
        throw new UnsupportedOperationException("not implemented yet");
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
        return this.resources;
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
