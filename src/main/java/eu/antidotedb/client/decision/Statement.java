package eu.antidotedb.client.decision;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * A class to implement statement structure in the S3 Access Control Procedure
 * extends the OLD API
 * @author Romain
 */
public class Statement {
    private int sid;
    private boolean effect;
    private String principalType;
    private String[] principals;//[type, principalID]
    private String[] action;
    private String[] ressources;
    private String conditionBlock;
    
    
    /*API
    JSON : 
    {
  "Version": "2012-10-17",
  "Id": "S3-Account-Permissions",
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
}
    */

    
    public Statement(String policyString){
        JsonObject value = Json.parse(policyString).asObject();
        /*private int sid;
    private boolean effect;
    private String principalType;
    private String[] principals;//[type, principalID]
    private String[] action;
    private String[] ressources;
    private String conditionBlock;*/
        this.sid = value.get("Sid").asInt();
        for (JsonValue itemprincipal: value.get("Principal").asArray()){
            /*https://github.com/ralfstx/minimal-json*/
        }
        JsonArray action=value.get("Action").asArray();
        //this.action = action;
        this.conditionBlock = value.get("Condition Block").asString();
        //TODO : Romain : 
    }
    
    public String toString(){
        //TODO : Romain : write it as a string with separators
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
