package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.POLICY;
import eu.antidotedb.client.accessresources.Permissions;
import eu.antidotedb.client.accessresources.S3AccessResource;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3Operation;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.decision.S3KeyLink;
import eu.antidotedb.client.messages.AntidoteRequest;
import eu.antidotedb.client.messages.AntidoteResponse;
import eu.antidotedb.client.transformer.Transformer;
import eu.antidotedb.client.transformer.TransformerWithDownstream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class performs the Access Control main process. transformer to S3 Access Control smeantics
 * 
 * - Override the communication with the Protocol Buffer
 * - requests the different Access Ressources in the security Bucket and domain Bucket
 * - calls the DecisionProcedure for every request
 * the differences in the management of user/bucket/object ACL/policies are hard coded
 * @author romain-dumarais
 */
public class S3AccessMonitor extends AccessMonitor{
    private final S3DecisionProcedure decisionprocedure = new S3DecisionProcedure();
    private final S3KeyLink keyLink=new S3KeyLink();
    private final Map<Connection, ByteString> userMapping = new HashMap<>();
    private final Map<Connection, Map<String,ByteString>> userDataMapping = new HashMap<>();
    
    public S3AccessMonitor() {
        super(null);
    }
    
    
    
    //--------------------------------------------------------------------------
    //              Transaction properties managemenet
    //--------------------------------------------------------------------------
    
    @Override
    void setCurrentUser(Connection connection, ByteString userid) {
        userMapping.put(connection, userid);
    }

    @Override
    void unsetCurrentUser(Connection connection) {
        userMapping.remove(connection);
    }
    
    @Override
    public void setUserData(Connection connection, Object userData) {
        this.userDataMapping.put(connection, (Map<String, ByteString>) userData);
    }

    @Override
    void unsetUserData(Connection connection) {
        userDataMapping.remove(connection);
    }
    
    
    //--------------- GETTERS -----------------
    
    private ByteString currentUser(Connection connection) {
        return userMapping.get(connection);
    }
    
    private ByteString currentDomain(Connection connection){
        return userDataMapping.get(connection).get("domain");
    }

    private Map<String,ByteString> currentUserData(Connection connection) {
        return userDataMapping.get(connection);
    }
    
    
    
    
    //--------------------------------------------------------------------------
    //              Access Resources Management
    //--------------------------------------------------------------------------
    
    //--------------- ACLs -----------------
    
    /**
     * method called by the transaction helpers, to : check if the user has the right to assign ACL & assign ACL
     * @param downstream AntidoteRequest.Handler<AntidoteResponse> handled by the transaction call
     * @param connection handled by the transaction call
     * @param descriptor handled by the transaction call
     * @param isBucketACL boolean flag to handle eiher object or bucket ACL
     * @param bucket bucket of the resource
     * @param key is null if the bucket is set
     * @param user ID of the user for which the ACL is assigned 
     * @param permissions 
     */
    void assignACL(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, AntidotePB.ApbBoundObject targetObject, ByteString targetUser, Collection<ByteString> permissions){
        if(!isOpACLAllowed(downstream, connection, descriptor, operation, targetObject)){ 
            throw new AccessControlException("ACL assignment not allowed for user : "+currentUser(connection).toStringUtf8());
        }else{
            //assignment
            ByteString securityBucket, aclKey;
            switch(operation){
                case WRITEBUCKETACL:
                    securityBucket = S3KeyLink.securityBucket(targetObject.getBucket());
                    aclKey = S3KeyLink.bucketACL(targetUser);
                    break;
                case WRITEOBJECTACL:
                    if(targetObject==null){throw new UnsupportedOperationException("ACL key can not be null");}
                    securityBucket = S3KeyLink.securityBucket(targetObject.getBucket());
                    aclKey = S3KeyLink.objectACL(targetObject.getKey(), targetUser);
                    break;
                default:
                    throw new AccessControlException("operation not supported");
            }
            AntidotePB.ApbUpdateOperation update=AntidotePB.ApbUpdateOperation.newBuilder()
                    .setPolicyop(AntidotePB.ApbPolicyUpdate.newBuilder()
                    .addAllPermissions(permissions))
                    .build();
            AntidotePB.ApbBoundObject object = AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(securityBucket)
                    .setKey(aclKey)
                    .setType(AntidotePB.CRDT_type.POLICY)
                    .build();
            assignHelper(downstream, descriptor, object, update);
        }
    }
    
    /**
     * method called by the transaction helpers, to : check if the user has the right to read ACL & read ACL
     * @param downstream handled by the transaction call
     * @param connection handled by the transaction call
     * @param descriptor handled by the transaction call
     * @param isBucketACL flag to handle object or bucket ACL
     * @param bucket
     * @param key is null if the bucket flag is set
     * @param user ID of the user for which the ACL is read 
     * @return permissions
     */
    Collection<? extends ByteString> readACL(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, AntidotePB.ApbBoundObject targetObject, ByteString user){
        if(!isOpACLAllowed(downstream, connection, descriptor, operation, targetObject)){
            throw new AccessControlException("ACL read is not allowed");
        }else{
            AntidotePB.ApbBoundObject aclRef;
            switch(operation){
                case READBUCKETACL:
                    aclRef = AntidotePB.ApbBoundObject.newBuilder().setBucket(S3KeyLink.securityBucket(targetObject.getBucket())).setKey(S3KeyLink.bucketACL(user)).setType(POLICY).build();
                    return readHelper(downstream, descriptor, aclRef).getPolicy().getPermissionsList();
                case READOBJECTACL:
                    aclRef = AntidotePB.ApbBoundObject.newBuilder().setBucket(S3KeyLink.securityBucket(targetObject.getBucket())).setKey(S3KeyLink.objectACL(targetObject.getKey(), user)).setType(POLICY).build();
                    return readHelper(downstream, descriptor, aclRef).getPolicy().getPermissionsList();
                default:
                    throw new AccessControlException("operation not compatible");
            }
        }
    }
    
    /**
     * internal function to read remote ACL, either for a read ACL operation, either for an operation check
     * @param downstream handled by the transaction call
     * @param descriptor handled by the transaction call
     * @param securityBucket bucket in which the ACL is read
     * @param aclKey key of the ACL
     * @param user ID of the user for which the ACL is read 
     * @return 
     */
    private AntidotePB.ApbReadObjectResp readHelper(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, AntidotePB.ApbBoundObject targetRef){
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(targetRef);

        AntidotePB.ApbReadObjectsResp policyResp = downstream.handle(readRequest
                    .build()).accept(new AntidoteResponse.MsgReadObjectsResp.Extractor());
        return policyResp.getObjects(0);
    }
    
    
    //--------------- Policies -----------------
    
    /**
     * handle and performs the write request for a Policy in the database
     * @param downstream
     * @param connection
     * @param descriptor
     * @param isUserPolicy
     * @param key
     * @param policyValue 
     */
    void assignPolicy(SocketSender downstream, Connection connection, ByteString descriptor, S3Operation operation, ByteString key, S3Policy policyObject){
        if(!isOpPolicyAllowed(downstream, connection, descriptor, operation, key)){
            throw new AccessControlException("Policy assign not allowed");
        }else{
            //protect the read-only domain flag
            for(ByteString group:policyObject.getGroups()){
                if(group.toStringUtf8().startsWith("_domain_")){
                    throw new AccessControlException("group format not allowed");}
            }
            ByteString policyBucket, policyKey;
            switch(operation){
                case ASSIGNUSERPOLICY:
                    policyBucket = S3KeyLink.userBucket(currentDomain(connection));
                    policyKey = S3KeyLink.userPolicy(key);
                    break;
                case ASSIGNBUCKETPOLICY:
                    policyBucket = S3KeyLink.securityBucket(key);
                    policyKey = S3KeyLink.bucketPolicy();
                    break;
                default:
                    throw new AccessControlException("operation not supported");
            }
            
            //add the read-only domain flag
            List<ByteString> groupList = new ArrayList<>();
            groupList.add(S3KeyLink.domainFlag(currentDomain(connection)));
            groupList.addAll(policyObject.getGroups());
            ByteString policyValue = new S3Policy(groupList, policyObject.getStatements()).encode();;
                  
            //policy assignment
            AntidotePB.ApbBoundObject policy = AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(policyBucket)
                    .setKey(policyKey)
                    .setType(AntidotePB.CRDT_type.MVREG)
                    .build();
            AntidotePB.ApbUpdateOperation update = AntidotePB.ApbUpdateOperation.newBuilder()
                    .setRegop(AntidotePB.ApbRegUpdate.newBuilder()
                            .setValue(policyValue)).build();
            assignHelper(downstream, descriptor, policy, update);
        }
    }
    
    
    /**
     * function that performs the write operations in the database.
     * @param downstream
     * @param descriptor
     * @param object BoundObject to update
     * @param update update operation to perform
     */
    private void assignHelper(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, AntidotePB.ApbBoundObject object, AntidotePB.ApbUpdateOperation update){
        AntidotePB.ApbUpdateObjects s3policyUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addUpdates(AntidotePB.ApbUpdateOp.newBuilder()
                        .setBoundobject(object)
                        .setOperation(update))
                .build();
            AntidoteRequest.MsgUpdateObjects request = AntidoteRequest.of(s3policyUpdateOp);
            
            //Bypass connection transformers
            AntidoteResponse.Handler<AntidotePB.ApbOperationResp> responseExtractor = request.readResponseExtractor();
            AntidoteResponse response = request.accept(downstream);
            //handle errors
            if (responseExtractor == null) {
            throw new IllegalStateException("Could not get response extractor for policy assign request");
            }
            if (response == null) {
                throw new AntidoteException("Missing response for " + request);
            }
            AntidotePB.ApbOperationResp operationResp = response.accept(responseExtractor);
            if (!operationResp.getSuccess()) {
                throw new AntidoteException("Could not perform S3 policy update (error code: " + operationResp.getErrorcode() + ")");
            }
    }
    
    /**
     * handle the read request for a Policy in the database
     * @param downstream handled by the transaction call
     * @param connection handled by the transaction call
     * @param descriptor handled by the transaction call
     * @param isUserPolicy if {@code true} returns a S3UserPolicy object, if {@code false} returns a S3BucketPolicy
     * @param key either the bucket key or the user ID
     * @return readPolicy
     */
    S3Policy readPolicy(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, ByteString key){
        if(!isOpPolicyAllowed(downstream, connection, descriptor, operation, key)){
            throw new AccessControlException("Policy read not allowed");
        }else{
            S3Policy resultPolicy;
            switch(operation){
            case READUSERPOLICY:
                resultPolicy = policyMergerHelper(readHelper(downstream, 
                        descriptor, AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(S3KeyLink.userBucket(currentDomain(connection)))
                        .setKey(S3KeyLink.userPolicy(key))
                        .setType(AntidotePB.CRDT_type.MVREG)
                        .build()).getMvreg().getValuesList());
                resultPolicy.removeGroup(S3KeyLink.domainFlag(currentDomain(connection)));//make the read-only domain flag not readable for users
                return new S3UserPolicy(resultPolicy.getGroups(), resultPolicy.getStatements());
            case READBUCKETPOLICY:
                resultPolicy = policyMergerHelper(readHelper(downstream, 
                        descriptor, AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(S3KeyLink.securityBucket(key))
                        .setKey(S3KeyLink.bucketPolicy())
                        .setType(AntidotePB.CRDT_type.MVREG)
                        .build()).getMvreg().getValuesList());
                resultPolicy.removeGroup(S3KeyLink.domainFlag(currentDomain(connection)));//make the read-only domain flag not readable for users
                return new S3BucketPolicy(resultPolicy.getGroups(), resultPolicy.getStatements());
            default:
                throw new AccessControlException("unsupported operation");
            }
            
        }
    }

    /**
     * reads a Policy in the Database
     * @param downstream
     * @param descriptor
     * @param bucket
     * @param policyKey
     * @return concurrentPolicies collection of concurrent policies
     */
    S3Policy readPolicyUnchecked(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, ByteString securityBucket, ByteString policyKey){
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(securityBucket)
                    .setKey(policyKey)
                    .setType(AntidotePB.CRDT_type.MVREG));

        AntidotePB.ApbReadObjectsResp policyResp = downstream.handle(readRequest
                    .build()).accept(new AntidoteResponse.MsgReadObjectsResp.Extractor());
        List<ByteString> concurrentPolicies = policyResp.getObjects(0).getMvreg().getValuesList();
        
        
        return policyMergerHelper(concurrentPolicies);
    }
    
    /**
     * helper to merge concurrent updates for Policy objects
     * The current merge operation computes a policy with the union of the ALLOW
     * statements and the intersection of DENY statement to be as restrictive as
     * possible.
     * it is to notice that the minimal policy get the intersection of the groups
     * and does not ensure the restrictive character of the group assignment.
     * @param concurrentPolicies set of concurrent objects
     * @return minimalPolicy the policy object with the intersection of the groups and statements
     * TODO : Romain : change format to have separated negative & positive statements
     */
    public S3Policy policyMergerHelper(List<ByteString> concurrentPolicies){
        
        switch(concurrentPolicies.size()){
            case(0):
                return new S3BucketPolicy();//arbitrary choice
            case(1):
                return new S3Policy(concurrentPolicies.get(0));
            default:
                List<S3Policy> policies = new ArrayList<>();
                for(ByteString stringPolicy:concurrentPolicies){
                    policies.add(new S3Policy(stringPolicy));
                }
                S3Policy minimalPolicy = new S3Policy(new ArrayList<>(), new ArrayList<>());//empty Policy
                List<ByteString> groupList = policies.get(0).getGroups();
                List<S3Statement> statementsList = policies.get(0).getStatements();
                for(ByteString group : groupList){
                    boolean isIntersection=true;
                    for(S3Policy policy : policies){
                        isIntersection = isIntersection && policy.containsGroup(group);
                    }
                    if(isIntersection){minimalPolicy.addGroup(group);}
                }
                //Intersection of positive statements
                for(S3Statement statement : statementsList){
                    if(statement.getEffect()){
                        boolean isIntersection=true;
                        for(S3Policy policy : policies){
                            isIntersection = isIntersection && policy.containsStatement(statement);
                        }
                        if(isIntersection){minimalPolicy.addStatement(statement);}
                    }
                }
                //Union of negative statements
                for(S3Policy policy:policies){
                    for(S3Statement statement : policy.getStatements()){
                        if((!statement.getEffect()) && (!minimalPolicy.containsStatement(statement))){
                            minimalPolicy.addStatement(statement);}
                    }
                }
                return minimalPolicy;
        }
    }
    
    
    //--------------------------------------------------------------------------
    //              Calls to DecisionProcedure
    //--------------------------------------------------------------------------
    
    /**
     * helper to get remote access resources in the database from the decision 
     * procedure response
     * @param downstream transformer for download
     * @param descriptor Transaction descriptor
     * @param requestedPolicies map with the references of the policies
     * @return accessResources
     */
    private List<S3AccessResource> getResources(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, Map<String, AntidotePB.ApbBoundObject> requestedPolicies) {
        List<S3AccessResource> accessResources= new ArrayList<>();
        if(requestedPolicies.containsKey("userPolicy")){
            S3Policy userpolicy = policyMergerHelper(readHelper(downstream, descriptor, requestedPolicies.get("userPolicy")).getMvreg().getValuesList());
            //groups
            /*
            for(ByteString group : userpolicy.getGroups()){
                if(!group.toStringUtf8().startsWith("_")){
                    AntidotePB.ApbBoundObject policy = AntidotePB.ApbBoundObject.newBuilder()
                            .setBucket(S3KeyLink.userBucket(domain))
                            .setKey(S3KeyLink.userPolicy(group))
                            .setType(AntidotePB.CRDT_type.MVREG).build();
                    accessResources.add(policyMergerHelper(readHelper(downstream, descriptor, policy).getMvreg().getValuesList()));
                }
            }*/
            accessResources.add(userpolicy);
        }
        if(requestedPolicies.containsKey("bucketPolicy")){
            S3Policy bucketpolicy = policyMergerHelper(readHelper(downstream, descriptor, requestedPolicies.get("bucketPolicy")).getMvreg().getValuesList());
            //groups
            for(ByteString group : bucketpolicy.getGroups()){
                
            }
            accessResources.add(bucketpolicy);
        }
        if(requestedPolicies.containsKey("bucketACL")){
            accessResources.add(new Permissions(readHelper(downstream, descriptor, requestedPolicies.get("bucketACL")).getPolicy().getPermissionsList()));
        }
        if(requestedPolicies.containsKey("objectACL")){
            accessResources.add(new Permissions(readHelper(downstream, descriptor, requestedPolicies.get("objectACL")).getPolicy().getPermissionsList()));
        }
        return accessResources;
    }
    
    /**
     * get the needed access resources and pass calls the access decision 
     * @param downstream transformer for access resources reading
     * @param connection 
     * @param descriptor transaction descriptor
     * @param isUpdate boolean flag {@code true} if the result is "isUpdateObjectAllowed", {@code false} if the result is "isReadObjectAllowed"
     * @param targetBucket key of the targeted Bucket
     * @param targetObject key of the targeted Object in symbolic bucket
     * @return isOpObjectAllowed
     */
    private boolean isOpObjectAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, AntidotePB.ApbBoundObject targetObject) {
        //get AccessResources
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);

        //root credentials
        //if(domain.equals(currentUser)){return true;}
        
        Map<String, AntidotePB.ApbBoundObject> requestedPolicies = this.decisionprocedure.s3requestedPolicies(currentUser, domain, targetObject, operation);
        List<S3AccessResource> accessResources = getResources(downstream, descriptor, requestedPolicies);
        /*Collection<ByteString> bucketACL = readHelper(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.bucketACL(currentUser))
                .setType(POLICY)
                .build()).getPolicy().getPermissionsList();
        Collection<ByteString> objectACL = readHelper(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                .setType(POLICY)
                .build()).getPolicy().getPermissionsList();
        S3UserPolicy userPolicy; S3BucketPolicy bucketPolicy;
        userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));
        bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.securityBucket(targetObject.getBucket()), S3KeyLink.bucketPolicy());*/
        
        //call decision Procedure
        switch(operation){
        case WRITEOBJECT:
            return this.decisionprocedure.decideUpdate(currentUser, targetObject, currentUserData(connection), accessResources);
        case READOBJECT:
            //TODO : Romain : pass operation to decicion procedure
            return this.decisionprocedure.decideRead(currentUser, targetObject, currentUserData(connection), accessResources);
        default:
            throw new AccessControlException("unsupported operation");
        }
    }
    
    /**
     * get the needed access resources and pass calls the access decision 
     * @param downstream transformer for communication w/ protocol buffer
     * @param connection
     * @param descriptor transaction descriptor
     * @param isAssign boolean flag {@code true} if the operation is READ, {@code false} if ASSIGN
     * @param isBucketACL boolean flag {@code true} if the operation is on a bucketACL, {@code false} if on an objectACL
     * @param bucket requested bucket
     * @param key requested object
     * @return {@code true} if the operation is allowed, {@code false} if denied
     */
    private boolean isOpACLAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, AntidotePB.ApbBoundObject targetObject) {
        //get requested policies
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        
        //root credentials
        //if(domain.equals(currentUser)){return true;}
        
        /*Collection<ByteString> bucketACL = readHelper(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.bucketACL(currentUser))
                .setType(POLICY)
                .build()).getPolicy().getPermissionsList();
        Collection<ByteString> objectACL;
        S3UserPolicy userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));
        S3BucketPolicy bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.securityBucket(targetObject.getBucket()), S3KeyLink.bucketPolicy());*/
        Map<String, AntidotePB.ApbBoundObject> requestedResources = this.decisionprocedure.s3requestedPolicies(currentUser, domain, targetObject, operation);
        List<S3AccessResource> accessResources = getResources(downstream, descriptor, requestedResources);
        
        switch(operation){
            case READOBJECTACL:
                 /*objectACL = readHelper(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                         .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                         .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                         .setType(POLICY)
                         .build()).getPolicy().getPermissionsList();*/
                return this.decisionprocedure.decideObjectACLRead(currentUser, targetObject, currentUserData(connection), accessResources);
            case WRITEOBJECTACL:
                /*objectACL = readHelper(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                        .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                        .setType(POLICY)
                        .build()).getPolicy().getPermissionsList();*/
                return this.decisionprocedure.decideObjectACLAssign(currentUser, targetObject, currentUserData(connection), accessResources);
            case READBUCKETACL:
                return this.decisionprocedure.decideBucketACLRead(currentUser, targetObject, currentUserData(connection), accessResources);
            case WRITEBUCKETACL:
                return this.decisionprocedure.decideBucketACLAssign(currentUser, targetObject, currentUserData(connection), accessResources);
            default:
                return false;
        }
    }

    
    /**
     * get the needed access resources and pass calls the access decision 
     * @param downstream transformer for communication w/ protocol buffer
     * @param connection
     * @param descriptor transaction descriptor
     * @param isAssign boolean flag {@code true} if the operation is READ, {@code false} if ASSIGN
     * @param isUserPolicy boolean flag {@code true} if the operation is on a user Policy, {@code false} if on an bucket Policy
     * @param bucket requested bucket
     * @param key requested bucket or user key
     * @return {@code true} if the operation is allowed, {@code false} if denied
     */
    private boolean isOpPolicyAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, ByteString key) {
        //get requested policies
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        
        //root credentials
        //if(domain.equals(currentUser)){return true;}
        
        //TODO : Romain : remove casts
        
        /*S3BucketPolicy bucketPolicy;
        S3UserPolicy userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));*/
        AntidotePB.ApbBoundObject target;
        Map<String, AntidotePB.ApbBoundObject> requestedPolicies;
        List<S3AccessResource> accessResources;
        switch(operation){
            case ASSIGNUSERPOLICY:
                target= AntidotePB.ApbBoundObject.newBuilder().setKey(key)
                        .setBucket(ByteString.copyFromUtf8("userbucket"))
                        .setType(POLICY).build();
                requestedPolicies = this.decisionprocedure.s3requestedPolicies(currentUser, domain, target, operation);
                accessResources = getResources(downstream, descriptor, requestedPolicies);
                return this.decisionprocedure.decideUserPolicyAssign(currentUser, target, currentUserData(connection), accessResources);
            case READUSERPOLICY:
                target= AntidotePB.ApbBoundObject.newBuilder().setKey(key)
                        .setBucket(ByteString.copyFromUtf8("userbucket"))
                        .setType(POLICY).build();
                requestedPolicies = this.decisionprocedure.s3requestedPolicies(currentUser, domain, target, operation);
                accessResources = getResources(downstream, descriptor, requestedPolicies);
                return this.decisionprocedure.decideUserPolicyRead(currentUser, target, currentUserData(connection), accessResources);
            case ASSIGNBUCKETPOLICY:
                target= AntidotePB.ApbBoundObject.newBuilder().setKey(ByteString.copyFromUtf8("bucketPolicy"))
                        .setBucket(key).setType(POLICY).build();
                requestedPolicies = this.decisionprocedure.s3requestedPolicies(currentUser, domain, target, operation);
                accessResources = getResources(downstream, descriptor, requestedPolicies);
                //bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.securityBucket(key), S3KeyLink.bucketPolicy());
                return this.decisionprocedure.decideBucketPolicyAssign(currentUser, target, currentUserData(connection), accessResources);
            case READBUCKETPOLICY:
                target= AntidotePB.ApbBoundObject.newBuilder().setKey(ByteString.copyFromUtf8("bucketPolicy"))
                        .setBucket(key).setType(POLICY).build();
                requestedPolicies = this.decisionprocedure.s3requestedPolicies(currentUser, domain, target, operation);
                accessResources = getResources(downstream, descriptor, requestedPolicies);
                //bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, S3KeyLink.securityBucket(key), S3KeyLink.bucketPolicy());
                return this.decisionprocedure.decideBucketPolicyRead(currentUser, target, currentUserData(connection), accessResources);
            default:
                throw new AccessControlException("unsupported operation");
            }
    }

    
    
    
    //--------------------------------------------------------------------------
    //              interception of database calls
    //--------------------------------------------------------------------------

    /**
     * transformer to intercept the access request in the protocol buffer and 
     * overwrites them with decision procedure
     * @param downstream
     * @param connection
     */
    @Override
    public Transformer newTransformer(Transformer downstream, Connection connection) {
        return new TransformerWithDownstream(downstream) {
            
            /**
             * overwrites the read request sent in the database and throws an exception
             * if the access is denied
             * @param apbReadObjects read request
             */
            @Override
            public AntidoteResponse handle(AntidotePB.ApbReadObjects apbReadObjects) {
                AntidotePB.ApbReadObjects.Builder reqBuilder = apbReadObjects.toBuilder().clearBoundobjects();
                ByteString transactionDescriptor = apbReadObjects.getTransactionDescriptor();
                for (AntidotePB.ApbBoundObject boundObject : apbReadObjects.getBoundobjectsList()) {
                    if(!isOpObjectAllowed(getDownstream(), connection, transactionDescriptor, READOBJECT, boundObject)){
                        throw new AccessControlException("User not allowed to read object");
                    }
                    
                    reqBuilder.addBoundobjects(boundObject.toBuilder().setBucket(S3KeyLink.dataBucket(boundObject.getBucket())));
                }
                return getDownstream().handle(reqBuilder.build());
            }
            
            /**
             * overwrites the write request sent in the database and throws an exception
             * if the access is denied
             * @param apbUpdateObjects write request
             * @return updateObjectResponse
             */
            @Override
            public AntidoteResponse handle(AntidotePB.ApbUpdateObjects apbUpdateObjects) {
                AntidotePB.ApbUpdateObjects.Builder reqBuilder = apbUpdateObjects.toBuilder().clearUpdates();
                ByteString transactionDescriptor = apbUpdateObjects.getTransactionDescriptor();
                for (AntidotePB.ApbUpdateOp updateOp : apbUpdateObjects.getUpdatesList()) {
                    if(!isOpObjectAllowed(getDownstream(), connection, transactionDescriptor, WRITEOBJECT, updateOp.getBoundobject())){
                        throw new AccessControlException("User not allowed to write object");
                    }
                    reqBuilder.addUpdates(updateOp.toBuilder().setBoundobject(updateOp.getBoundobject().toBuilder().
                            setBucket(S3KeyLink.dataBucket(updateOp.getBoundobject().getBucket()))));
                }
                return getDownstream().handle(reqBuilder.build());
            }
            
        };
    }
    
    
    
    
    
    
    
    //--------------------------------------------------------------------------
    //              Resources Lifecycle
    //--------------------------------------------------------------------------
    

    /**
     * this is a helper to initialize Policies and the domain flag. It acts like 
     * a reset operation.
     * @param downstream
     * @param descriptor
     * @param isUser
     * @param domain
     * @param targetKey 
     */
    void init(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, boolean isUser, ByteString domain, ByteString targetKey) {
        ByteString policyBucket, policyKey, domainflag;
        if(isUser){policyBucket=S3KeyLink.userBucket(domain);
        policyKey=S3KeyLink.userPolicy(targetKey);
        S3UserPolicy flag=new S3UserPolicy(); flag.addGroup(S3KeyLink.domainFlag(domain));
        domainflag=flag.encode();}
        else{policyBucket=S3KeyLink.securityBucket(targetKey);
        policyKey=S3KeyLink.bucketPolicy();
        S3BucketPolicy flag=new S3BucketPolicy(); flag.addGroup(S3KeyLink.domainFlag(domain));
        domainflag=flag.encode();}

        //policy assignment
        AntidotePB.ApbUpdateObjects domainUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
                .setTransactionDescriptor(descriptor)
                .addUpdates(AntidotePB.ApbUpdateOp.newBuilder()
                    .setBoundobject(AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(policyBucket)
                        .setKey(policyKey)
                        .setType(AntidotePB.CRDT_type.MVREG))
                    .setOperation(AntidotePB.ApbUpdateOperation.newBuilder()
                        .setRegop(AntidotePB.ApbRegUpdate.newBuilder()
                        .setValue(domainflag))))
            .build();
        AntidoteRequest.MsgUpdateObjects request = AntidoteRequest.of(domainUpdateOp);

        //Bypass connection transformers
        AntidoteResponse.Handler<AntidotePB.ApbOperationResp> responseExtractor = request.readResponseExtractor();
        AntidoteResponse response = request.accept(downstream);
        //handle errors
        if (responseExtractor == null) {
        throw new IllegalStateException("Could not get response extractor for create resource");
        }
        if (response == null) {
            throw new AntidoteException("Missing response for " + request);
        }
        AntidotePB.ApbOperationResp operationResp = response.accept(responseExtractor);
        if (!operationResp.getSuccess()) {
            throw new AntidoteException("Could not create resource (error code: "
                    + operationResp.getErrorcode() + ")");
        }
    }

    void delete(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, boolean isUser, ByteString domain, ByteString targetKey) {
        ByteString policyBucket, policyKey;
        if(isUser){policyBucket=S3KeyLink.userBucket(domain);
        policyKey=S3KeyLink.userPolicy(targetKey);
        }
        else{policyBucket=S3KeyLink.securityBucket(targetKey);
        policyKey=S3KeyLink.bucketPolicy();
        }
        
        //policy assignment
        AntidotePB.ApbUpdateObjects domainUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
                .setTransactionDescriptor(descriptor)
                .addUpdates(AntidotePB.ApbUpdateOp.newBuilder()
                    .setBoundobject(AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(policyBucket)
                        .setKey(policyKey)
                        .setType(AntidotePB.CRDT_type.MVREG))
                    .setOperation(AntidotePB.ApbUpdateOperation.newBuilder()
                        .setRegop(AntidotePB.ApbRegUpdate.newBuilder()
                        .setValue(ByteString.EMPTY))))
            .build();
        AntidoteRequest.MsgUpdateObjects request = AntidoteRequest.of(domainUpdateOp);

        //Bypass connection transformers
        AntidoteResponse.Handler<AntidotePB.ApbOperationResp> responseExtractor = request.readResponseExtractor();
        AntidoteResponse response = request.accept(downstream);
        //handle errors
        if (responseExtractor == null) {throw new IllegalStateException("Could not get response extractor for create resource");}
        if (response == null) {throw new AntidoteException("Missing response for "
                + request);}
        AntidotePB.ApbOperationResp operationResp = response.accept(responseExtractor);
        if (!operationResp.getSuccess()) {throw new AntidoteException("Could not create resource (error code: "
                + operationResp.getErrorcode() + ")");}
    }

    
    
}
