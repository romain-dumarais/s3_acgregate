package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.S3KeyLink;
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
        Set<ByteString> content = tx.readBucketACLHelper(bucket, userid).read(tx, userid);
        this.setRight(userid.toStringUtf8(), decodeRight(content));
    }
    
    /**
     * assigns a right to a user without touchng the others
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid, String right){
        Set<ByteString> encodedPermissions = encodeRight(right).stream().collect(Collectors.toSet());
        tx.bucketACLAssignHelper(userid, bucket, encodedPermissions);
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
