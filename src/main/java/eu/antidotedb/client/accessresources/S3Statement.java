package eu.antidotedb.client.accessresources;

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
    private final List<String> action;
    private final List<String> ressources;
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
        this.action=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.ressources=resources;
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
        this.action=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.ressources=null;
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
        this.action=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principals=principals;
        this.ressources=Arrays.asList(resourcetype.toString());
        this.resourcebucket=null;
    }

    public S3Statement() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    ByteString encode(){
        //TODO : Romain : encode Statement as JSON object
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
