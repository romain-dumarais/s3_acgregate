package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * NEW API
 * @author Romain
 */
public class S3AccessControl implements DecisionProcedure {
    private final ByteString userBucket = ByteString.copyFromUtf8(".usersPoliciesBucket");

    @Override
    public boolean decideUpdate(AntidotePB.ApbBoundObject object, AntidotePB.ApbUpdateOperation op, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decideRead(AntidotePB.ApbBoundObject object, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decidePolicyAssign(ByteString bucket, ByteString key, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decidePolicyRead(ByteString bucket, ByteString key, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the Map of ACLs and Policies needed for accessing an object
     * @param bucket original bucket of the object that is read
     * @param key object key
     * @return requestedPolicies Map of the requested policies to Download
     */
    @Override
    public Map<String, ObjectInBucket> requestedPolicies(ByteString bucket, ByteString key) {
        TreeMap<String, ObjectInBucket> requestedPolicies = new TreeMap<>();
        ByteString aclBucket = getAclBucket(bucket);
        requestedPolicies.put("objectACL", new ObjectInBucket(aclBucket, key));
        requestedPolicies.put("bucketACL", new ObjectInBucket(bucket, ByteString.copyFromUtf8("policy")));
        requestedPolicies.put("bucketPolicy", new ObjectInBucket(bucket, key));
        requestedPolicies.put("userPolicy", new ObjectInBucket(this.userBucket, key));
        return requestedPolicies;
    }
    
    /**
     * binds a bucket to its ACL bucket
     * @param bucket bucket to link to its ACL bucket
     * @return the Key of the ACL bucket
     */
    private ByteString getAclBucket(ByteString bucket) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
}
