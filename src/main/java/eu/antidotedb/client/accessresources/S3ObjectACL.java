package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * handler for S3 Object ACL
 * @author romain-dumarais
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
     * builder for Access Monitor
     * @param user
     * @param policyValues 
     */
    public S3ObjectACL(ByteString user, Collection<? extends ByteString> policyValues){
        super(user, policyValues);
    }
    
    /**
     * reads the right for a user in the database. Other users rights are not updated.
     * @param tx
     * @param bucket
     * @param key
     * @param userid
     */
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        //Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        //this.setRight(userid.toStringUtf8(), decodeRight(policy.read(tx,userid)));
        Collection<? extends ByteString> policyValues = tx.readACLHelper(false, bucket, userid, key);
        Set<ByteString> res = policyValues.stream().collect(Collectors.toSet());
        this.permissions.put(userid, res);
    }

    /**
     * assigns a certain right to a user. Does not modify the other users rights.
     * @param tx
     * @param bucket
     * @param key
     * @param userid
     * @param right
     */
    public static void assignForUserStatic(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid, String right){
        tx.assignACLHelper(false, bucket, key, userid, encodeRight(right));
    }
    
     /**
     * assigns a the current policy to a user. Does not modify the other users rights.
     * @param tx
     * @param bucket
     * @param key
     * @param userid
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        tx.assignACLHelper(false, bucket, key, userid, this.permissions.get(userid));
    }
    
     /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not modify the rights of users that are not present in the local ACL object
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      * @param key key of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket, ByteString key){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            tx.assignACLHelper(false, bucket, key, user, this.permissions.get(user));
            
        }
    }

}
