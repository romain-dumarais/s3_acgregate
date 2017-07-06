package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.Policy;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.ValueCoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * handler for S3 Object ACL
 * @author Romain
 */
public class S3ObjectACL extends S3ACL{
    
    public S3ObjectACL(Map<ByteString, Set<ByteString>> acl) {
        super(acl);
    }
    
    public S3ObjectACL(HashMap<String, String> rights) {
        super(rights);
    }
        
    public static S3ObjectACL readForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid){
        Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        Map<ByteString, Set<ByteString>> acl = new HashMap<>();
        acl.put(userid, policy.read(tx, userid));
        return new S3ObjectACL(acl);
    }

    
    public void assignForUser(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key, ByteString userid, String right){
        Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
        policy.assign(tx, userid, encodeRight(right));
    }
    
    public void assign(SecuredInteractiveTransaction tx, ByteString bucket, ByteString key){
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            Policy policy = new Policy(bucket, key, ValueCoder.utf8String);
            policy.assign(tx, user, this.permissions.get(user));
        }
    }
}
