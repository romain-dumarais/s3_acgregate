package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;

/**
 * link between metadata and data. 
 * Throws exceptions if the targetBucket is a metadata bucket
 * @author romain-dumarais
 * TODO : use link in existing Antidote FS projects
 */
public final class S3KeyLink {
    
    public ByteString domainFlag(ByteString bucketKey, ByteString objectKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString securityBucket(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString dataBucket(ByteString bucketKey){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString userBucket(ByteString domain){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString objectACL(ByteString objectKey, ByteString user){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString bucketACL(ByteString bucketKey, ByteString user){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString bucketPolicy(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public ByteString userPolicy(ByteString user){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}