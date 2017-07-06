package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;

/**
• Computes the Security Bucket from a origin Bucket name
• Computes the Data Bucket from a origin Bucket name
• Computes the ACL key from the object key
• Computes the user bucket key from the domain key
 * @author Romain
 * TODO : everything
 */
public class S3KeyLink {
    
    public ByteString securityBucket(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString dataBucket(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString userBucket(ByteString domain){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString objectACL(ByteString objectKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString bucketACL(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString bucketPolicy(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString userPolicy(ByteString user){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}