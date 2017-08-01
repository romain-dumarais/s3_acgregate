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
 * The code structure is voluntarily redundant to be clearer
 * @author romain-dumarais
 * TODO : Romain : add groups
 * TODO : Romain : add userData
 */
public class S3DecisionProcedure {
    
    //--------------------------------
    //      Object Management
    //--------------------------------
    
    /**
     * decision Process for a read request. It creates a request object and reads
     * the four access resources to check if the access is explicitly denied. 
     * If not, it reads again the four access resources to check if the access is 
     * explicitly allowed. If not, denies the access by default.
     * @param domain
     * @param currentUser ID of the user performing the request
     * @param userData arbitrary Data for application-level operations and informations
     * @param targetBucket key of the requested bucket
     * @param targetObject key of the requested object
     * @param objectACL
     * @param bucketACL
     * @param bucketPolicy
     * @param userPolicy
     * @return isRequestAllowed
     */
    public boolean decideObjectRead(ByteString domain, ByteString currentUser, Object userData, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "READOBJECT", targetBucket, targetObject, userData);
        System.out.println("-- read : user : "+currentUser.toStringUtf8()+" | currentObject : "+targetObject.toStringUtf8());
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"read")){ 
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"read")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"read")){
            return true;
        }
        if(S3ACL.explicitAllow(objectACL,"read")){
            return true;
        }
        return false;
    }
    
    /**
     * decision Process for a write request. It creates a request object and reads
     * the four access resources to check if the access is explicitly denied. 
     * If not, it reads again the four access resources to check if the access is 
     * explicitly allowed. If not, denies the access by default.
     * @param domain
     * @param currentUser
     * @param userData
     * @param targetBucket
     * @param targetObject
     * @param objectACL
     * @param bucketACL
     * @param bucketPolicy
     * @param userPolicy
     * @return isRequestAllowed
     */
    public boolean decideObjectWrite(ByteString domain, ByteString currentUser, Object userData, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "WRITEOBJECT", targetBucket, targetObject, null);
        System.out.println("-- write : user : "+currentUser.toStringUtf8()+" | currentObject : "+targetObject.toStringUtf8());
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"write")){ 
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"write")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"write")){
            return true;
        }
        if(S3ACL.explicitAllow(objectACL,"write")){
            return true;
        }
        return false;
    }
    
    
    //--------------------------------
    //      ACL Management
    //--------------------------------
    //TODO : Romain : userData
    
    //--------- Bucket ACL ----------
    
    public boolean decideBucketACLRead(ByteString currentUser, ByteString targetBucket, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "READBUCKETACL", targetBucket, null, null);
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"readACL")){
            return true;
        }
        return false;
    }
    
    public boolean decideBucketACLAssign(ByteString currentUser, ByteString targetBucket, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "WRITEBUCKETACL", targetBucket, null, null);
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"writeACL")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"writeACL")){
            return true;
        }
        return false;
    }
    
    //--------- Object ACL ----------
    
    public boolean decideObjectACLRead(ByteString currentUser, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "READOBJECTACL", targetBucket, targetObject, null);
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){ 
            //TODO : Romain : document this choice
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"readACL")){
            //TODO : Romain : document this choice
            return true;
        }
        if(S3ACL.explicitAllow(objectACL,"readACL")){
            return true;
        }
        return false;
    }
    
    public boolean decideObjectACLAssign(ByteString currentUser, ByteString targetBucket, ByteString targetObject, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "WRITEOBJECTACL", targetBucket, targetObject, null);
        System.out.println("**** request : "+currentUser.toStringUtf8());
        System.out.println("object ACL write : objectACL : "+objectACL.toString());
        System.out.println("object ACL write : bucketACL : "+bucketACL.toString());
        System.out.println("object ACL write : targetObject : "+targetObject.toStringUtf8()+" | target Bucket : "+targetBucket.toStringUtf8());
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(S3ACL.explicitDeny(bucketACL,"readACL")){ 
            //TODO : Romain : document this choice
            return false;
        }
        if(S3ACL.explicitDeny(objectACL,"readACL")){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        if(S3ACL.explicitAllow(bucketACL,"readACL")){
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
    //TODO : Romain : add userData
    
    //--------- Bucket Policy ----------
    
    public boolean decideBucketPolicyRead(ByteString currentUser, ByteString targetBucket, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "READBUCKETPOLICY", targetBucket, null, null);
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
    public boolean decideBucketPolicyAssign(ByteString currentUser, ByteString targetBucket, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, "ASSIGNBUCKETPOLICY", targetBucket, null, null);
        if(userPolicy.explicitDeny(request)){
            return false;
        }
        if(bucketPolicy.explicitDeny(request)){
            return false;
        }
        if(userPolicy.explicitAllow(request)){
            return true;
        }
        if(bucketPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
    //--------- User Policy ----------
    
    public boolean decideUserPolicyRead(ByteString targetUser,S3UserPolicy currentUserPolicy){
        S3Request request = new S3Request(null, "READUSERPOLICY", null, targetUser, null);
        if(currentUserPolicy.explicitDeny(request)){
            return false;
        }
        if(currentUserPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
    public boolean decideUserPolicyAssign(ByteString targetUser, S3UserPolicy currentUserPolicy){
        S3Request request = new S3Request(null, "ASSIGNUSERPOLICY", null, targetUser, null);
        if(currentUserPolicy.explicitDeny(request)){
            return false;
        }
        if(currentUserPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
}