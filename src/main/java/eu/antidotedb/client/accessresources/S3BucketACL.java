package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * handler for S3 Bucket ACL
 * @author romain-dumarais
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
     * @param tx
     * @param bucket
     * @param userid
     */
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        Collection<? extends ByteString> policyValues = tx.readACLHelper(true, bucket, null, userid);
        Set<ByteString> res = policyValues.stream().collect(Collectors.toSet());
        this.permissions.put(userid, res);
    }
    
    /**
     * assigns a right to a user without touching the others
     * @param tx
     * @param bucket
     * @param right
     * @param userid
     */
    public static void assignForUserStatic(S3InteractiveTransaction tx, ByteString bucket, ByteString userid, String right){
        tx.assignACLHelper(true, bucket, null, userid, encodeRight(right));
    }
    
    /**
     * assigns the current policy to a user without touching the others
     * @param tx
     * @param bucket
     * @param userid
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        tx.assignACLHelper(true, bucket, null, userid, this.permissions.get(userid));
    }
    
    /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not modify the rights of users that are not present in the local ACL object
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket){
        Set<ByteString> users = this.permissions.keySet();
        users.stream().forEach((user) -> {
            tx.assignACLHelper(true, bucket, null, user, this.permissions.get(user));
        });
    }
}
