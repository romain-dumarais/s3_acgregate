package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.accessresources.S3ACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import java.util.Collection;
import java.util.Map;

/**
 * this class performes the access decisions in an function-oriented way : 
 * is the user the domain root ? Is the user known in this domain ? 
 * Is there any explicit deny ? Any explicit allow ?
 * If needed, requests a group Policy
 * The code structure is voluntarily redundant to show a clear decision procedure 
 * @author romain-dumarais
 * TODO : Romain : add groups
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
     * @param currentUser ID of the user performing the request
     * @param userData arbitrary Data for application-level operations and informations
     * @param targetObject the requested object
     * @param objectACL
     * @param bucketACL
     * @param bucketPolicy
     * @param userPolicy
     * @return isRequestAllowed
     */
    public boolean decideObjectRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String, ByteString> userData, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, READOBJECT, targetObject.getBucket(), targetObject.getKey(), userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
     * @param currentUser
     * @param userData
     * @param targetObject
     * @param objectACL
     * @param bucketACL
     * @param bucketPolicy
     * @param userPolicy
     * @return isRequestAllowed
     */
    public boolean decideObjectWrite(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, WRITEOBJECT, targetObject.getBucket(), targetObject.getKey(), null);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    //--------- Bucket ACL ----------
    
    public boolean decideBucketACLRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, READBUCKETACL, targetObject.getBucket(), null, userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideBucketACLAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, WRITEBUCKETACL, targetObject.getBucket(), null, userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideObjectACLRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, READOBJECTACL, targetObject.getBucket(), targetObject.getKey(), userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideObjectACLAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, Collection<ByteString> objectACL, Collection<ByteString> bucketACL, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, WRITEOBJECTACL, targetObject.getBucket(), targetObject.getKey(), userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideBucketPolicyRead(ByteString currentUser, ByteString targetBucket, Map<String,ByteString> userData, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, READBUCKETPOLICY, targetBucket, null, userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideBucketPolicyAssign(ByteString currentUser, ByteString targetBucket, Map<String,ByteString> userData, S3BucketPolicy bucketPolicy, S3UserPolicy userPolicy){
        S3Request request = new S3Request(currentUser, ASSIGNBUCKETPOLICY, targetBucket, null, userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(userPolicy, domain) || userPolicy.explicitDeny(request)){
            return false;
        }
        if(!S3KeyLink.isInitialized(bucketPolicy, domain) || bucketPolicy.explicitDeny(request)){
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
    
    public boolean decideUserPolicyRead(ByteString targetUser,S3UserPolicy currentUserPolicy, Map<String,ByteString> userData){
        S3Request request = new S3Request(null, READUSERPOLICY, null, targetUser, userData);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(currentUserPolicy, domain) || currentUserPolicy.explicitDeny(request)){
            return false;
        }
        if(currentUserPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
    public boolean decideUserPolicyAssign(ByteString targetUser, S3UserPolicy currentUserPolicy, Map<String,ByteString> userData){
        S3Request request = new S3Request(null, ASSIGNUSERPOLICY, null, targetUser, null);
        ByteString domain = userData.get("domain");
        if(!S3KeyLink.isInitialized(currentUserPolicy, domain) || currentUserPolicy.explicitDeny(request)){
            return false;
        }
        if(currentUserPolicy.explicitAllow(request)){
            return true;
        }
        return false;
    }
    
}