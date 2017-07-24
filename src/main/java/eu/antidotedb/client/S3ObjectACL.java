package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Set;

/**
 * handler for S3 Object ACL
 * @author Romain
 */
public class S3ObjectACL extends S3ACL{
    
    //TODO : static reader constructor
    //TODO : all-users reader
    
    public S3ObjectACL(){
        super();
    }
    
    public S3ObjectACL(HashMap<String, String> rights) {
        super(rights);
    }
    
    /**
     * reads the right for a user in the database. Other users rights are not updated.
     */
    public void readForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        super.permissions.put(userid, policy.read(tx, userid));
    }

    /**
     * assigns a certain right to a user. Does not modify the other users rights.
     */
    public static void assignForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid, String right){
        Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        policy.assign(tx, userid, encodeRight(right));
    }
     /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not reduce nor modify any rights of unconsidered users
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      * @param key key of the target object
      */
    public void assign(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
            policy.assign(tx, user, this.permissions.get(user));
        }
    }
}
