package eu.antidotedb.client;

import eu.antidotedb.client.S3ACL;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * handler for S3 Bucket ACL
 * @author Romain
 */
public class S3BucketACL extends S3ACL{
    
    public S3BucketACL(){
        super();
    }
    
    public S3BucketACL(HashMap<String, String> rights) {
        super(rights);
    }
    
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        throw new UnsupportedOperationException("not ready yet");
        //Romain : TODO : the Key need to be linked to S3KeyLink anyhow
        //Policy policy = new Policy(bucket, SomeArbitraryKey, ValueCoder.utf8String);
        //return new S3ACL(policy.read(tx, userid));
    }
    
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid, String right){
        Set<ByteString> encodedPermissions = encodeRight(right).stream().collect(Collectors.toSet());
        tx.bucketACLAssignHelper(userid, bucket, encodedPermissions);
        throw new UnsupportedOperationException("not ready yet");
    }
    
    /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not reduce nor modify any rights of unconsidered users
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      * @param key key of the target object
      */
    public void assign(SecuredInteractiveTransaction tx, ByteString bucket){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            //Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
            //policy.assign(tx, user, this.permissions.get(user));
        }
    }
}
