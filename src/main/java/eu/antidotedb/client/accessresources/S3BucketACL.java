package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.POLICY;
import eu.antidotedb.client.S3InteractiveTransaction;
import static eu.antidotedb.client.accessresources.S3Operation.WRITEBUCKETACL;
import static eu.antidotedb.client.accessresources.S3Operation.READBUCKETACL;
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
        //the key is not necessary, but it is required in the ProtoBuf
        AntidotePB.ApbBoundObject targetBucket = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(ByteString.copyFromUtf8("bucketACL")).setType(POLICY).build();
        Collection<? extends ByteString> policyValues = tx.readACLHelper(userid, targetBucket, READBUCKETACL);
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
        //the key is not necessary, but it is required in the ProtoBuf
        AntidotePB.ApbBoundObject targetBucket = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(ByteString.copyFromUtf8("bucketACL")).setType(POLICY).build();
        tx.assignACLHelper(userid, targetBucket, WRITEBUCKETACL, encodeRight(right));
    }
    
    /**
     * assigns the current policy to a user without touching the others
     * @param tx
     * @param bucket
     * @param userid
     */
    public void assignForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid){
        //the key is not necessary, but it is required in the ProtoBuf
        AntidotePB.ApbBoundObject targetBucket = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(ByteString.copyFromUtf8("bucketACL")).setType(POLICY).build();
        tx.assignACLHelper(userid, targetBucket, WRITEBUCKETACL, this.permissions.get(userid));
    }
    
    /**
      * assigns the current ACL Map to a remote objectACL.
      * This operation does not modify the rights of users that are not present in the local ACL object
      * @param tx transaction used for the assigh operation
      * @param bucket bucket of the target object
      */
    public void assign(S3InteractiveTransaction tx, ByteString bucket){
        Set<ByteString> users = this.permissions.keySet();
        //the key is not necessary, but it is required in the ProtoBuf
        AntidotePB.ApbBoundObject targetBucket = AntidotePB.ApbBoundObject.newBuilder().setBucket(bucket).setKey(ByteString.copyFromUtf8("bucketACL")).setType(POLICY).build();
        users.stream().forEach((user) -> {
            tx.assignACLHelper(user, targetBucket, WRITEBUCKETACL, this.permissions.get(user));
        });
    }
}
