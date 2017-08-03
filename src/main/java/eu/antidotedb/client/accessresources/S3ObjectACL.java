package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.POLICY;
import eu.antidotedb.client.S3InteractiveTransaction;
import static eu.antidotedb.client.accessresources.S3Operation.READOBJECTACL;
import static eu.antidotedb.client.accessresources.S3Operation.WRITEOBJECTACL;
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
     * reads the right for a user in the database. Other users rights are not updated.
     * @param tx
     * @param bucket
     * @param key
     * @param userid
     */
    public void readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        AntidotePB.ApbBoundObject object = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(key).setType(POLICY).build();
        Collection<? extends ByteString> policyValues = tx.readACLHelper(userid, object, READOBJECTACL);
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
        AntidotePB.ApbBoundObject object = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(key).setType(POLICY).build();
        tx.assignACLHelper(userid, object, WRITEOBJECTACL, encodeRight(right));
    }
    
     /**
     * assigns a the current policy to a user. Does not modify the other users rights.
     * @param tx
     * @param bucket
     * @param key
     * @param userid
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        AntidotePB.ApbBoundObject object = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(key).setType(POLICY).build();
        tx.assignACLHelper(userid, object, WRITEOBJECTACL, this.permissions.get(userid));
    }
    
     /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not modify the rights of users that are not present in the local ACL object
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      * @param targetObject key of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket, ByteString targetObject){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString targetUser:users){
            AntidotePB.ApbBoundObject object = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(targetObject).setType(POLICY).build();
            tx.assignACLHelper(targetUser, object, WRITEOBJECTACL, this.permissions.get(targetUser));
        }
    }

}
