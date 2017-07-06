package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.S3ACL;
import eu.antidotedb.client.decision.S3Policy;

/**
 * Gets the requested Policies and return them either as S3ACL instances, either as S3Policy instance.
 * Get the Bucket domain flag
 * @author Romain
 */
public class S3AccessResources {
    
    public S3ACL getObjectACL(ByteString objectKey, ByteString bucketKeys){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public S3ACL getBucketACL(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public S3Policy getBucketPolicy(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public S3Policy getUserPolicy(ByteString userKey){
        //TODO : handle group Policy
        throw new UnsupportedOperationException("not implemented yet");
    }
}
