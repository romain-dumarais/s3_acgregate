package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3ACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import java.util.Collection;

/**
 * this class performes the access decisions in an function-oriented way : 
 * is the user the domain root ? Is the user known in this domain ? 
 * Is there any explicit deny ? Any explicit allow ?
 * If needed, requests a group Policy
 * @author romain-dumarais
 * TODO : Romain : add groups
 * TODO : Romain : refactor
 */
public class S3DecisionProcedure {
    
    //--------------------------------
    //      Object Management
    //--------------------------------
    
    public boolean decideObjectRead(ByteString domain, ByteString currentUser, Object userData, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean decideObjectWrite(ByteString domain, ByteString currentUser, Object userData, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    //--------------------------------
    //      ACL Management
    //--------------------------------
    //TODO : Romain : userData
    
    //--------- Bucket ACL ----------
    
    public boolean decideBucketACLRead(ByteString currentUser, ByteString targetBucket, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("READBUCKETACL", targetBucket)){
            return false;
        }
        if(bucketPolicy.explicitDeny("READBUCKETACL", currentUser)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow("READBUCKETACL", targetBucket)){
            return true;
        }
        if(bucketPolicy.explicitAllow("READBUCKETACL", currentUser)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"readACL")){
            return true;
        }
        return false;
    }
    
    public boolean decideBucketACLAssign(ByteString currentUser, ByteString targetBucket, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("WRITEBUCKETACL", targetBucket)){
            return false;
        }
        if(bucketPolicy.explicitDeny("WRITEBUCKETACL", currentUser)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"writeACL")){
            return false;
        }
        if(userPolicy.explicitAllow("WRITEBUCKETACL", targetBucket)){
            return true;
        }
        if(bucketPolicy.explicitAllow("WRITEBUCKETACL", currentUser)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"writeACL")){
            return true;
        }
        return false;
    }
    
    //--------- Object ACL ----------
    
    public boolean decideObjectACLRead(ByteString currentUser, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("READOBJECTACL", targetBucket, targetObject)){
            return false;
        }
        if(bucketPolicy.explicitDeny("READOBJECTACL", currentUser, targetObject)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){ 
            //TODO : Romain : document this choice
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow("READOBJECTACL", targetBucket, targetObject)){
            return true;
        }
        if(bucketPolicy.explicitAllow("READOBJECTACL", currentUser, targetObject)){
            return true;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){
            //TODO : Romain : document this choice
            return true;
        }
        if(S3ACL.explicitAllow(objectACL,"readACL")){
            return true;
        }
        return false;
    }
    
    public boolean decideObjectACLAssign(ByteString currentUser, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("READOBJECTACL", targetBucket, targetObject)){
            return false;
        }
        if(bucketPolicy.explicitDeny("READOBJECTACL", currentUser, targetObject)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){ 
            //TODO : Romain : document this choice
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow("READOBJECTACL", targetBucket, targetObject)){
            return true;
        }
        if(bucketPolicy.explicitAllow("READOBJECTACL", currentUser, targetObject)){
            return true;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){
            //TODO : Romain : document this choice
            return true;
        }
        if(S3ACL.explicitAllow(objectACL,"readACL")){
            return true;
        }
        return false;
    }
    
    //--------------------------------
    //      Policies Management
    //--------------------------------
    //TODO : Romain : change decide calls in AccessMonitor
    
    //--------- Bucket Policy ----------
    
    public boolean decideBucketPolicyRead(ByteString currentUser, ByteString targetBucket, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("READBUCKETPOLICY", targetBucket)){
            return false;
        }
        if(bucketPolicy.explicitDeny("READBUCKETPOLICY", currentUser)){
            return false;
        }
        if(userPolicy.explicitAllow("READBUCKETPOLICY", targetBucket)){
            return true;
        }
        if(bucketPolicy.explicitAllow("READBUCKETPOLICY", currentUser)){
            return true;
        }
        return false;
    }
    
    public boolean decideBucketPolicyAssign(ByteString currentUser, ByteString targetBucket, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        if(userPolicy.explicitDeny("ASSIGNBUCKETPOLICY", targetBucket)){
            return false;
        }
        if(bucketPolicy.explicitDeny("ASSIGNBUCKETPOLICY", currentUser)){
            return false;
        }
        if(userPolicy.explicitAllow("ASSIGNBUCKETPOLICY", targetBucket)){
            return true;
        }
        if(bucketPolicy.explicitAllow("ASSIGNBUCKETPOLICY", currentUser)){
            return true;
        }
        return false;
    }
    
    //--------- User Policy ----------
    
    public boolean decideUserPolicyRead(ByteString targetUser,S3UserPolicy currentUserPolicy){
        if(currentUserPolicy.explicitDeny("READUSERPOLICY", targetUser)){
            return false;
        }
        if(currentUserPolicy.explicitAllow("READUSERPOLICY", targetUser)){
            return true;
        }
        return false;
    }
    
    public boolean decideUserPolicyAssign(ByteString targetUser, S3UserPolicy currentUserPolicy){
        if(currentUserPolicy.explicitDeny("ASSIGNUSERPOLICY",targetUser)){
            return false;
        }
        if(currentUserPolicy.explicitAllow("ASSIGNUSERPOLICY",targetUser)){
            return true;
        }
        return false;
    }
    
}