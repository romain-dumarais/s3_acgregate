package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Policy;

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
        if(bucketKey.toStringUtf8().startsWith("_")){
            throw new AccessControlException("not a valid bucket name");
        }
        return ByteString.copyFromUtf8("_ACL_").concat(bucketKey);
    }
    
    public ByteString dataBucket(ByteString bucketKey){
        if(bucketKey.toStringUtf8().startsWith("_")){
            throw new AccessControlException("not a valid bucket name");
        }
        return ByteString.copyFromUtf8("_data_").concat(bucketKey);
    }
    
    public ByteString userBucket(ByteString domain){
        return ByteString.copyFromUtf8("_user_").concat(domain);
    }
    
    public ByteString objectACL(ByteString objectKey, ByteString user){
        return objectKey.concat(ByteString.copyFromUtf8("_")).concat(user);
    }
    
    public ByteString bucketACL(ByteString user){
        return ByteString.copyFromUtf8("_bucket_acl_").concat(user);
    }
    
    public ByteString bucketPolicy(){
        return ByteString.copyFromUtf8("_bucket_policy");
    }
    
    public ByteString userPolicy(ByteString user){
        return user.concat(ByteString.copyFromUtf8("_policy"));
    }
    
    public static boolean isInitialized(S3Policy policy, ByteString domain) {
        return policy.getGroups().contains(domain);
    }
    
}