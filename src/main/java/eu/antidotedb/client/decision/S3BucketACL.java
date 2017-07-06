package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * handler for S3 Bucket ACL
 * @author Romain
 */
public class S3BucketACL extends S3ACL{
    
    public S3BucketACL(Map<ByteString, Set<ByteString>> acl) {
        super(acl);
    }
    
    public S3BucketACL(HashMap<String, String> rights) {
        super(rights);
    }
    
    public static S3BucketACL readForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString userid){
        throw new UnsupportedOperationException("not ready yet");
        //Romain : TODO : the Key need to be linked to S3KeyLink anyhow
        //Policy policy = new Policy(bucket, SomeArbitraryKey, ValueCoder.utf8String);
        //return new S3ACL(policy.read(tx, userid));
    }
    
    public void assignForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString userid, String right){
        //Policy policy = new Policy(bucket, SomeArbitraryKey, ValueCoder.utf8String);
        //policy.assign(tx, userid, encodeRight(right));
        throw new UnsupportedOperationException("not ready yet");
    }
    
}
