package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3UserPolicy;

/**
 * handle requests & decision procedure
 * @author romain-dumarais
 */
public class S3Request {
    private final ByteString domain;
    private final ByteString currentUser;
    private final Object userData;
    private final ByteString objectKey;
    private final ByteString bucketKey;
    private final String operation;//TODO : Romain : use an enum type
    private S3ObjectACL objectACL;
    private S3BucketACL bucketACL;
    //TODO : Romain : lists for groups
    private S3BucketPolicy bucketpolicy; 
    private S3UserPolicy buserpolicy;
    
    
    public S3Request(ByteString domain, ByteString currentUser, Object userData, 
            ByteString objectKey, ByteString bucketKey, String operation){
        this.bucketKey=bucketKey;
        this.currentUser=currentUser;
        this.domain=domain;
        this.objectKey=objectKey;
        this.operation=operation;
        this.userData=userData;
    }
    
    /**
     * 
     * @return the access decision : {@code true} if ALLOW, {@code false} if deny
     */
    public boolean makeDecision(){
        if(currentUser.equals(domain)){
            return true; //transaction with root credentials
        }else{
            throw new UnsupportedOperationException("not implemented yet");
        }
    }
    
    public void addBucketACL(S3BucketACL bucketACL){
        this.bucketACL=bucketACL;
    }
    
    public void addObjectACL(S3ObjectACL objectACL){
        this.objectACL=objectACL;
    }
    
    public void addBucketPolicy(S3BucketPolicy bucketPolicy){
        //TODO : Romain : groups
        this.bucketpolicy=bucketPolicy;
    }
    
    public void addUserPolicy(S3UserPolicy userPolicy){
        //TODO : Romain : groups
        this.buserpolicy=userPolicy;
    }
}
