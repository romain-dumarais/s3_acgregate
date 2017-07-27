package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import java.util.Collection;
import java.util.Map;

/**
 * this class performes the access decisions in an function-oriented way
 * @author romain-dumarais
 * TODO : Romain : add groups
 */
public class S3DecisionProcedure implements DecisionProcedure {
    
    //--------------------------------
    //      Object Management
    //--------------------------------
    
    public boolean decideObjectRead(ByteString domain, ByteString currentUser, Object userData, S3ObjectACL objectACL, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideObjectAssign(ByteString domain, ByteString currentUser, Object userData, S3ObjectACL objectACL, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    //--------------------------------
    //      ACL Management
    //--------------------------------
    
    public boolean decideBucketACLRead(ByteString domain, ByteString currentUser, Object userData, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideBucketACLAssign(ByteString domain, ByteString currentUser, Object userData, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideObjectACLRead(ByteString domain, ByteString currentUser, Object userData, S3ObjectACL objectACL, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideObjectACLAssign(ByteString domain, ByteString currentUser, Object userData, S3ObjectACL objectACL, S3BucketACL bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    //--------------------------------
    //      Policies Management
    //--------------------------------
    
    public boolean decideBucketPolicyRead(ByteString domain, ByteString currentUser, Object userData, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideBucketPolicyAssign(ByteString domain, ByteString currentUser, Object userData, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideUserPolicyRead(ByteString domain, ByteString currentUser, Object userData, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideUserPolicyAssign(ByteString domain, ByteString currentUser, Object userData, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    //------------------------
    //          OLD API
    //------------------------
    @Override
    public boolean decideUpdate(AntidotePB.ApbBoundObject object, AntidotePB.ApbUpdateOperation op, Object userData, Map<String, Collection<ByteString>> policies) {
        throw new UnsupportedOperationException("OLD API.");
    }
    @Override
    public boolean decideRead(AntidotePB.ApbBoundObject object, Object userData, Map<String, Collection<ByteString>> policies) {
        throw new UnsupportedOperationException("OLD API.");
    }
    @Override
    public boolean decidePolicyAssign(ByteString bucket, ByteString key, Collection<ByteString> oldPolicy, Collection<ByteString> newPolicy, Object userData, Map<String, Collection<ByteString>> policies) {
        throw new UnsupportedOperationException("OLD API.");
    }
    @Override
    public boolean decidePolicyRead(ByteString bucket, ByteString key, Object userData, Map<String, Collection<ByteString>> policies) {
        throw new UnsupportedOperationException("OLD API.");
    }
    @Override
    public Map<String, ObjectInBucket> requestedPolicies(ByteString bucket, ByteString key) {
        throw new UnsupportedOperationException("OLD API.");
    }
}