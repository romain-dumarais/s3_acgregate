package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.accessresources.S3AccessResource;
import eu.antidotedb.client.accessresources.S3Operation;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * this class performes the access decisions in an function-oriented way : 
 * is the user the domain root ? Is the user known in this domain ? 
 * Is there any explicit deny ? Any explicit allow ?
 * If needed, requests a group Policy
 * @author romain-dumarais
 * TODO : Romain : add groups
 * TODO : Romain : check initialization
 */
public class S3DecisionProcedure /*implements DecisionProcedure*/ {
    
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
     * @param accessResources
     * @return isRequestAllowed
     */
    public boolean decideRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String, ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, READOBJECT, targetObject, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
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
     * @param accessResources
     * @return isRequestAllowed
     * TODO : Romain : ApbOperation
     */
    public boolean decideUpdate(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, WRITEOBJECT, targetObject, null);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){
                return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    
    //--------------------------------
    //      ACL Management
    //--------------------------------
    
    //--------- Bucket ACL ----------
    
    public boolean decideBucketACLRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, READBUCKETACL, targetObject, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    public boolean decideBucketACLAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, WRITEBUCKETACL, targetObject, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    //--------- Object ACL ----------
    
    public boolean decideObjectACLRead(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, READOBJECTACL, targetObject, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    public boolean decideObjectACLAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetObject, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, WRITEOBJECTACL, targetObject, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    //--------------------------------
    //      Policies Management
    //--------------------------------
    //TODO : Romain : add userData
    
    //--------- Bucket Policy ----------
    
    public boolean decideBucketPolicyRead(ByteString currentUser, AntidotePB.ApbBoundObject targetBucket, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, READBUCKETPOLICY, targetBucket, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    public boolean decideBucketPolicyAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetBucket, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(currentUser, ASSIGNBUCKETPOLICY, targetBucket, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    //--------- User Policy ----------
    
    public boolean decideUserPolicyRead(ByteString currentUser, AntidotePB.ApbBoundObject targetUser, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(null, READUSERPOLICY, targetUser, userData);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }
    
    public boolean decideUserPolicyAssign(ByteString currentUser, AntidotePB.ApbBoundObject targetUser, Map<String,ByteString> userData, List<S3AccessResource> accessResources){
        S3Request request = new S3Request(null, ASSIGNUSERPOLICY, targetUser, null);
        ByteString domain = userData.get("domain");
        
        //root transaction
        if(currentUser.equals(domain)){return true;}
        
        //check that groups, buckets and users have been created by the domain 
        //root authority
        for(S3AccessResource resource : accessResources){
            if(!S3KeyLink.isInitialized(resource,domain)){return false;}
        }
        
        for(S3AccessResource resource: accessResources){
            if(resource.explicitDeny(request)){return false;}
        }
        for(S3AccessResource resource: accessResources){
            if(resource.explicitAllow(request)){return true;}
        }
        return false;
    }

    public Map<String, AntidotePB.ApbBoundObject> s3requestedPolicies(ByteString currentUser, ByteString domain, AntidotePB.ApbBoundObject object, S3Operation operation) {
        HashMap<String, AntidotePB.ApbBoundObject> requestedPolicies = new HashMap<>();
        switch(operation){
            case READOBJECTACL:
            case WRITEOBJECTACL:
            case READOBJECT:
            case WRITEOBJECT:
                requestedPolicies.put("objectACL", AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(object.getBucket()))
                .setKey(S3KeyLink.objectACL(object.getKey(),currentUser))
                .setType(AntidotePB.CRDT_type.POLICY)
                .build());
            case WRITEBUCKETACL:
            case READBUCKETACL:
                requestedPolicies.put("bucketACL", AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(object.getBucket()))
                .setKey(S3KeyLink.bucketACL(currentUser))
                .setType(AntidotePB.CRDT_type.POLICY)
                .build());
            case ASSIGNBUCKETPOLICY:
            case READBUCKETPOLICY:
                requestedPolicies.put("bucketPolicy", AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(object.getBucket()))
                .setKey(S3KeyLink.bucketPolicy())
                .setType(AntidotePB.CRDT_type.MVREG)
                .build());
            case READUSERPOLICY:
            case ASSIGNUSERPOLICY:
                requestedPolicies.put("userPolicy", AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.userBucket(domain))
                .setKey(S3KeyLink.userPolicy(currentUser))
                .setType(AntidotePB.CRDT_type.MVREG)
                .build());
                break;
        }
        return requestedPolicies;
    }

    
}