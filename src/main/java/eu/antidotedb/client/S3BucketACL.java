package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import java.util.Collection;
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
    
    /**
     * reads the rights for a user in the database and update it locally. Other
     * users rights not updated
     */
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        Collection<? extends ByteString> policyValues = tx.readBucketACLHelper(bucket, userid);
        Set<ByteString> res = policyValues.stream().collect(Collectors.toSet());
        this.permissions.put(userid, res);
    }
    
    /**
     * assigns a right to a user without touching the others
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid, String right){
        tx.bucketACLAssignHelper(bucket, userid, encodeRight(right));
    }
    
    /**
     * assigns the current policy to a user without touching the others
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        tx.bucketACLAssignHelper(bucket, userid, this.permissions.get(userid));
    }
    
    /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not reduce nor modify any rights of unconsidered users
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket){
        Set<ByteString> users = this.permissions.keySet();
        users.stream().forEach((user) -> {
            tx.bucketACLAssignHelper(user, bucket, this.permissions.get(user));
        });
    }
}
