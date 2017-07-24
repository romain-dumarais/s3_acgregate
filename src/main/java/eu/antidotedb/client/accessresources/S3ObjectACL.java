package eu.antidotedb.client.accessresources;

import eu.antidotedb.client.accessresources.S3ACL;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * handler for S3 Object ACL
 * @author Romain
 */
public class S3ObjectACL extends S3ACL{
    
    //TODO : Romain : all-users reader
    
    public S3ObjectACL(){
        super();
    }
    
    public S3ObjectACL(HashMap<String, String> rights) {
        super(rights);
    }
    
    /**
     * reads the right for a user in the database. Other users rights are not updated.
     */
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        //Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        //this.setRight(userid.toStringUtf8(), decodeRight(policy.read(tx,userid)));
        Collection<? extends ByteString> policyValues = tx.readObjectACLHelper(bucket, userid, key);
        Set<ByteString> res = policyValues.stream().collect(Collectors.toSet());
        this.permissions.put(userid, res);
    }

    /**
     * assigns a certain right to a user. Does not modify the other users rights.
     */
    public static void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid, String right){
        tx.objectACLAssignHelper(bucket, key, userid, encodeRight(right));
    }
    
     /**
     * assigns a the current policy to a user. Does not modify the other users rights.
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        tx.objectACLAssignHelper(bucket, key, userid, this.permissions.get(userid));
    }
    
     /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not reduce nor modify any rights of unconsidered users
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      * @param key key of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket, ByteString key){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            tx.objectACLAssignHelper(bucket, key, user, this.permissions.get(user));
            
        }
    }
}
