package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.Permissions;
import eu.antidotedb.client.accessresources.S3AccessResource;
import eu.antidotedb.client.accessresources.S3Policy;
import java.util.Arrays;

/**
 * link between metadata and data. 
 * Throws exceptions if the targetBucket is a metadata bucket
 * @author romain-dumarais
 * TODO : use link in existing Antidote FS projects
 */
public final class S3KeyLink {
    
    
    //----------------------------------
    //          Key Mapping
    //----------------------------------
    
    /*
    public ByteString domainFlag(ByteString bucketKey, ByteString objectKey){
        throw new UnsupportedOperationException("not implemented yet");
    }*/
    
    public static ByteString securityBucket(ByteString bucketKey){
        if(bucketKey.toStringUtf8().startsWith("_")){
            throw new AccessControlException("not a valid bucket name");
        }
        return ByteString.copyFromUtf8("_acl_").concat(bucketKey);
    }
    
    public static ByteString dataBucket(ByteString bucketKey){
        if(bucketKey.toStringUtf8().startsWith("_")){
            throw new AccessControlException("not a valid bucket name");
        }
        return ByteString.copyFromUtf8("_data_").concat(bucketKey);
    }
    
    public static ByteString userBucket(ByteString domain){
        return ByteString.copyFromUtf8("_user_").concat(domain);
    }
    
    public static ByteString objectACL(ByteString objectKey, ByteString currentUser){
        return objectKey.concat(ByteString.copyFromUtf8("_")).concat(currentUser);
    }
    
    public static ByteString bucketACL(ByteString currentUser){
        return ByteString.copyFromUtf8("_bucket_acl_").concat(currentUser);
    }
    
    public static ByteString bucketPolicy(){
        return ByteString.copyFromUtf8("_bucket_policy");
    }
    
    public static ByteString userPolicy(ByteString user){
        return user.concat(ByteString.copyFromUtf8("_policy"));
    }
    
    public static boolean isInitialized(S3AccessResource resource, ByteString domain) {
        if(resource instanceof S3Policy){
            S3Policy policy = (S3Policy) resource;
            return policy.getGroups().contains(domainFlag(domain));
        }
        return resource instanceof Permissions;
    }
    
    public static ByteString domainFlag(ByteString domain){
        return ByteString.copyFromUtf8("_domain_").concat(domain);
    }
}