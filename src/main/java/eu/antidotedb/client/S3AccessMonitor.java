package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
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
    private final Map<Connection, Object> userDataMapping = new HashMap<>();
    private final Map<Connection,ByteString> domainMapping = new HashMap();
    
    public S3AccessMonitor() {
        super(null);
    }
    
    //-----------------------------------------------------------
    //              Transaction properties managemenet
    //-----------------------------------------------------------
    
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
        this.userDataMapping.put(connection, userData);
    }

    @Override
    void unsetUserData(Connection connection) {
        userDataMapping.remove(connection);
    }
    
    void setDomain(Connection connection, ByteString domain) {
        domainMapping.put(connection,domain);
    }
    
    void unsetDomain(Connection connection) {
        domainMapping.remove(connection);
    }
    
    //--------------- GETTERS -----------------
    
    private ByteString currentUser(Connection connection) {
        return userMapping.get(connection);
    }
    
    private ByteString currentDomain(Connection connection){
        return domainMapping.get(connection);
    }

    private Object currentUserData(Connection connection) {
        return userDataMapping.get(connection);
    }
    
    
    
    
    //-----------------------------------------------------------
    //              Access Resources Management
    //-----------------------------------------------------------
    
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
    void assignACL(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL,ByteString bucket, ByteString targetObject, ByteString targetUser, Collection<ByteString> permissions){
        if(!isOpACLAllowed(downstream, connection, descriptor, true, isBucketACL, bucket, targetObject)){ 
            throw new AccessControlException("ACL assignment not allowed");
        }else{
            //assignment
            ByteString securityBucket, aclKey;
            if(isBucketACL){
                securityBucket = keyLink.securityBucket(bucket);
                aclKey = keyLink.bucketACL(targetUser);
            }
            else{
                if(targetObject==null){throw new UnsupportedOperationException("ACL key can not be null");}
                securityBucket = keyLink.securityBucket(bucket);
                aclKey = keyLink.objectACL(targetObject, targetUser);
            }
            
            AntidotePB.ApbUpdateObjects aclUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addUpdates(AntidotePB.ApbUpdateOp.newBuilder()
                            .setBoundobject(AntidotePB.ApbBoundObject.newBuilder()
                                    .setBucket(securityBucket)
                                    .setKey(aclKey)
                                    .setType(AntidotePB.CRDT_type.POLICY))
                            .setOperation(AntidotePB.ApbUpdateOperation.newBuilder()
                                    .setPolicyop(AntidotePB.ApbPolicyUpdate.newBuilder()
                                            .addAllPermissions(permissions))))
                    .build();
            AntidoteRequest.MsgUpdateObjects request = AntidoteRequest.of(aclUpdateOp);
            //this bypasses transformers of the connection
            AntidoteResponse.Handler<AntidotePB.ApbOperationResp> responseExtractor = request.readResponseExtractor();
            AntidoteResponse response = request.accept(downstream);
            if (responseExtractor == null) {throw new IllegalStateException("Could not get response extractor for policy assign request");}
            if (response == null) {throw new AntidoteException("Missing response for " + request);}

            AntidotePB.ApbOperationResp operationResp = response.accept(responseExtractor);
            if (!operationResp.getSuccess()) {
                throw new AntidoteException("Could not perform S3 ACL update (error code: " + operationResp.getErrorcode() + ")");
            }
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
    Collection<? extends ByteString> readACL(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL, ByteString bucket, ByteString key, ByteString user){
        if(!isOpACLAllowed(downstream, connection, descriptor, false,isBucketACL, bucket, key)){
            throw new AccessControlException("ACL read is not allowed");
        }else{
            if(isBucketACL){
                return readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.bucketACL(user));
            }
            else{
                if(key==null){throw new UnsupportedOperationException("ACL key can not be null");}
                return readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.objectACL(key, user));
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
    private Collection<ByteString> readACLUnchecked(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, ByteString securityBucket, ByteString aclKey){
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(securityBucket)
                    .setKey(aclKey)
                    .setType(AntidotePB.CRDT_type.POLICY));

        AntidotePB.ApbReadObjectsResp policyResp = downstream.handle(readRequest
                    .build()).accept(new AntidoteResponse.MsgReadObjectsResp.Extractor());
        return policyResp.getObjects(0).getPolicy().getPermissionsList();
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
    void assignPolicy(SocketSender downstream, Connection connection, ByteString descriptor, boolean isUserPolicy, ByteString key, ByteString policyValue){
        if(!isOpPolicyAllowed(downstream, connection, descriptor, true, isUserPolicy, key)){
            throw new AccessControlException("Policy assign not allowed");
        }else{
            ByteString policyBucket, policyKey;
            if(isUserPolicy){
                policyBucket = keyLink.userBucket(currentDomain(connection));
                policyKey = keyLink.userPolicy(key);
            }
            else{
                policyBucket = keyLink.securityBucket(key);
                policyKey = keyLink.bucketPolicy();
            }
            //policy assignment
            AntidotePB.ApbUpdateObjects s3policyUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addUpdates(AntidotePB.ApbUpdateOp.newBuilder()
                        .setBoundobject(AntidotePB.ApbBoundObject.newBuilder()
                            .setBucket(policyBucket)
                            .setKey(policyKey)
                            .setType(AntidotePB.CRDT_type.MVREG))
                        .setOperation(AntidotePB.ApbUpdateOperation.newBuilder()
                            .setRegop(AntidotePB.ApbRegUpdate.newBuilder()
                            .setValue(policyValue))))
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
    S3Policy readPolicy(SocketSender downstream, Connection connection, ByteString descriptor, boolean isUserPolicy, ByteString key){
        if(!isOpPolicyAllowed(downstream, connection, descriptor, false, isUserPolicy, key)){
            throw new AccessControlException("Policy read not allowed");
        }else{
            if(isUserPolicy){
                return readPolicyUnchecked(downstream, descriptor, isUserPolicy, keyLink.userBucket(currentDomain(connection)), keyLink.userPolicy(key));
            }else{
                return readPolicyUnchecked(downstream, descriptor, isUserPolicy, keyLink.securityBucket(key),keyLink.bucketPolicy());
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
    S3Policy readPolicyUnchecked(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, boolean isUserPolicy, ByteString securityBucket, ByteString policyKey){
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(securityBucket)
                    .setKey(policyKey)
                    .setType(AntidotePB.CRDT_type.MVREG));

        AntidotePB.ApbReadObjectsResp policyResp = downstream.handle(readRequest
                    .build()).accept(new AntidoteResponse.MsgReadObjectsResp.Extractor());
        List<ByteString> concurrentPolicies = policyResp.getObjects(0).getMvreg().getValuesList();
        
        List<S3Policy> policiesList; policiesList = new ArrayList<>();
        for(ByteString stringPolicy:concurrentPolicies){
                    S3Policy policy;
                    if(isUserPolicy){policy = new S3UserPolicy();
                    }else{policy = new S3BucketPolicy();}
                    policy.decode(stringPolicy.toStringUtf8());
                    policiesList.add(policy);
                }
        if(policiesList.isEmpty()){
            throw new AccessControlException("this policy has not been initialized");
        }
        return policyMergerHelper(policiesList, isUserPolicy);
    }
    
    /**
     * helper to merge concurrent updates for Policy objects
     * @param policies set of concurrent objects
     * @param isUserPolicy flag for return Policy type
     * @return minimalPolicy the policy object with the intersection of the groups and statements
     */
    public S3Policy policyMergerHelper(List<S3Policy> policies, boolean isUserPolicy){
        //TODO : Romain : make this private after tests
        switch(policies.size()){
            case(0):
                throw new UnsupportedOperationException("empty list");
            case(1):
                return policies.get(0);
            default:
                //TODO : Romain : not optimal
                S3Policy minimalPolicy;
                if(isUserPolicy){minimalPolicy = new S3UserPolicy();
                }else{minimalPolicy = new S3BucketPolicy();}
                List<ByteString> groupList = policies.get(0).getGroups();
                List<S3Statement> statementsList = policies.get(0).getStatements();
                for(ByteString group : groupList){
                    boolean isIntersection=true;
                    for(S3Policy policy : policies){
                        isIntersection = isIntersection && policy.containsGroup(group);
                    }
                    if(isIntersection){minimalPolicy.addGroup(group);}
                }
                for(S3Statement statement : statementsList){
                    boolean isIntersection=true;
                    for(S3Policy policy : policies){
                        isIntersection = isIntersection && policy.containsStatement(statement);
                    }
                    if(isIntersection){minimalPolicy.addStatement(statement);}
                }
                return minimalPolicy;
        }
    }
    
    
    
    //-----------------------------------------------------------
    //              Calls to DecisionProcedure
    //-----------------------------------------------------------
    //TODO : Romain : use enum instead of boolean flags
    
    /**
     * get the needed access resources and pass calls the access decision 
     * @param downstream transformer for access resources reading
     * @param connection 
     * @param descriptor transaction descriptor
     * @param isUpdate boolean flag {@code true} if the result is "isUpdateObjectAllowed", {@code false} if the result is "isReadObjectAllowed"
     * @param targetBucket key of the targeted Bucket
     * @param targetObject key of the targeted Object
     * @return isOpObjectAllowed
     */
    private boolean isOpObjectAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, boolean isUpdate, ByteString targetBucket, ByteString targetObject) {
        //get AccessResources
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        Object userData = currentUserData(connection); //TODO : Romain : use userData
        if(domain.equals(currentUser)){return true;}//root credentials
        Collection<ByteString> bucketACL = readACLUnchecked(downstream, descriptor, keyLink.securityBucket(targetBucket), keyLink.bucketACL(currentUser));
        Collection<ByteString> objectACL = readACLUnchecked(downstream, descriptor, keyLink.securityBucket(targetBucket), keyLink.objectACL(targetObject, currentUser));
        S3UserPolicy userPolicy; S3BucketPolicy bucketPolicy;
        //TODO : Romain : remove casts
        try{
            userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, keyLink.userBucket(domain), keyLink.userPolicy(currentUser));
        }catch(AccessControlException e){
            throw new AccessControlException("the user does not exist in the database");
        }
        try{
        bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, keyLink.securityBucket(targetBucket), keyLink.bucketPolicy());
        }catch(AccessControlException e){
            throw new AccessControlException("the bucket does not exist or is not allowed");
        }
        //call decision Procedure
        if(isUpdate){
            return this.decisionprocedure.decideObjectWrite(domain, currentUser, userData, targetBucket, targetObject, objectACL, bucketACL, bucketPolicy, userPolicy);
        }else{
            return this.decisionprocedure.decideObjectRead(domain, currentUser, userData, targetBucket, targetObject, objectACL, bucketACL, bucketPolicy, userPolicy);
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
    private boolean isOpACLAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, boolean isAssign, boolean isBucketACL, ByteString bucket, ByteString targetObject) {
        //get requested policies
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        Object userData = currentUserData(connection); //TODO : Romain : use userData
        if(domain.equals(currentUser)){return true;}//root credentials
        Collection<ByteString> bucketACL = readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.bucketACL(currentUser));
        S3UserPolicy userPolicy; S3BucketPolicy bucketPolicy;
        //TODO : Romain : remove casts
        try{
            userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, keyLink.userBucket(domain), keyLink.userPolicy(currentUser));
        }catch(AccessControlException e){
            throw new AccessControlException("the user does not exist in the database");
        }
        try{
            bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, keyLink.securityBucket(bucket), keyLink.bucketPolicy());
        }catch(AccessControlException e){
            throw new AccessControlException("the bucket does not exist or is not allowed");
        }
        
        if(isBucketACL){
            if(isAssign){
                return this.decisionprocedure.decideBucketACLAssign(currentUser, bucket, bucketACL, bucketPolicy, userPolicy);
            }else{
                return this.decisionprocedure.decideBucketACLRead(currentUser, bucket, bucketACL, bucketPolicy, userPolicy);
            }
        }else{
            Collection<ByteString> objectACL = readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.objectACL(targetObject, currentUser));
            if(isAssign){
                return this.decisionprocedure.decideObjectACLAssign(currentUser, bucket, targetObject, objectACL, bucketACL, bucketPolicy, userPolicy);
            }else{
                return this.decisionprocedure.decideObjectACLRead(currentUser, bucket, targetObject, objectACL, bucketACL, bucketPolicy, userPolicy);
            }
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
    private boolean isOpPolicyAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean isAssign, boolean isUserPolicy, ByteString key) {
        //get requested policies
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        Object userData = currentUserData(connection);//TODO : Romain : use userData
        if(domain.equals(currentUser)){return true;}//root credentials
        //TODO : Romain : remove casts
        S3BucketPolicy bucketPolicy;
        S3UserPolicy userPolicy;
        //start of the process
        try{
            userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, keyLink.userBucket(domain), keyLink.userPolicy(currentUser));
        }catch(AccessControlException e){
            throw new AccessControlException("the user does not exist in the database");
        }
        
        if(isUserPolicy){
            if(isAssign){
                return this.decisionprocedure.decideUserPolicyAssign(key,userPolicy);
            }else{
                return this.decisionprocedure.decideUserPolicyRead(key, userPolicy);
            }
        }else{
            try{
                bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, keyLink.securityBucket(key), keyLink.bucketPolicy());
            }catch(AccessControlException e){
                throw new AccessControlException("current Bucket does not exist in the database or is not allowed");
            }
            if(isAssign){
                return this.decisionprocedure.decideBucketPolicyAssign(currentUser, key, bucketPolicy, userPolicy);
            }else{
                return this.decisionprocedure.decideBucketPolicyRead(currentUser, key, bucketPolicy, userPolicy);
            }
        }
    }

    
    
    
    //-----------------------------------------------------------
    //              interception of database calls
    //-----------------------------------------------------------

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
                    ByteString origBucket = boundObject.getBucket();
                    if(!isOpObjectAllowed(getDownstream(), connection, transactionDescriptor, false, origBucket, boundObject.getKey())){
                        throw new AccessControlException("User not allowed to read object");
                    }
                    
                    reqBuilder.addBoundobjects(boundObject.toBuilder().setBucket(keyLink.dataBucket(origBucket)));
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
                    ByteString origBucket = updateOp.getBoundobject().getBucket();
                    if(!isOpObjectAllowed(getDownstream(), connection, transactionDescriptor, true, origBucket, updateOp.getBoundobject().getKey())){
                        throw new AccessControlException("User not allowed to write object");
                    }
                    reqBuilder.addUpdates(updateOp.toBuilder().setBoundobject(updateOp.getBoundobject().toBuilder().setBucket(keyLink.dataBucket(origBucket))));
                }
                return getDownstream().handle(reqBuilder.build());
            }
            
            //TODO : Romain : handle Static transactions
           
        };
    }
}
