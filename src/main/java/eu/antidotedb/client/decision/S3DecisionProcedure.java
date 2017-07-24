package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * TODO : Romain : everything
 * @author Romain
 */
public class S3DecisionProcedure implements DecisionProcedure {
    private final ByteString userBucket;
    private final ByteString owner; 
    
    
    public S3DecisionProcedure(){
        throw new AccessControlException("unsupported operation");
    }
    
    public S3DecisionProcedure(String ownerKey){
        this.owner=ByteString.copyFromUtf8(ownerKey);
        this.userBucket=ByteString.copyFromUtf8("."+ownerKey+"_userBucket");
    }

    @Override
    public boolean decideUpdate(AntidotePB.ApbBoundObject object, AntidotePB.ApbUpdateOperation op, Object userData, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        //TODO : Romain : interpret the operation performed and check for this kind of object & operation
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decideRead(AntidotePB.ApbBoundObject object, Object userData, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decidePolicyAssign(ByteString bucket, ByteString key, Collection<ByteString> oldPolicy, Collection<ByteString> newPolicy, Object userData, Map<String, Collection<ByteString>> policies) {
        //TODO : Romain : todo
        //TODO : Romain : compare the two policies and check there is no removal of the ownership
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean decidePolicyRead(ByteString bucket, ByteString key, Object userData, Map<String, Collection<ByteString>> policies) {
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
        /*TreeMap<String, ObjectInBucket> requestedPolicies = new TreeMap<>();
        ByteString aclBucket = getAclBucket(bucket);
        requestedPolicies.put("objectACL", new ObjectInBucket(aclBucket, key));
        requestedPolicies.put("bucketACL", new ObjectInBucket(bucket, ByteString.copyFromUtf8("policy")));
        requestedPolicies.put("bucketPolicy", new ObjectInBucket(bucket, key));
        requestedPolicies.put("userPolicy", new ObjectInBucket(this.userBucket, key));
        return requestedPolicies;*/
        //TODO : Romain : todo
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
