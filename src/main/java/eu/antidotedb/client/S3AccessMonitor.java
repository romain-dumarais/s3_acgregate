package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.accessresources.S3ACL;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.decision.S3KeyLink;
import eu.antidotedb.client.messages.AntidoteRequest;
import eu.antidotedb.client.messages.AntidoteResponse;
import eu.antidotedb.client.transformer.Transformer;
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
    void assignACL(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL,ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions){
        if(isOpACLAllowed(downstream, connection, descriptor, true, isBucketACL, bucket, key)){ 
            //assignment
            ByteString securityBucket, aclKey;
            if(isBucketACL){
                securityBucket = keyLink.securityBucket(bucket);
                aclKey = keyLink.bucketACL(bucket, user);
            }
            else{
                if(key==null){throw new UnsupportedOperationException("ACL key can not be null");}
                securityBucket = keyLink.securityBucket(bucket);
                aclKey = keyLink.objectACL(key, user);
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
        }else{
            throw new AccessControlException("Permission assignment not allowed");
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
                return readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.bucketACL(bucket, user));
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
    private Collection<? extends ByteString> readACLUnchecked(SocketSender downstream, ByteString descriptor, ByteString securityBucket, ByteString aclKey){
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
    
    
    void assignPolicy(SocketSender downstream, Connection connection, ByteString descriptor, boolean isUserPolicy, ByteString key, ByteString policyValue){
        if(!isAssignPolicyAllowed(downstream, connection, descriptor, isUserPolicy, key)){
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
     * reads in a database a policy 
     * @param downstream handled by the transaction call
     * @param connection handled by the transaction call
     * @param descriptor handled by the transaction call
     * @param isUserPolicy if {@code true} returns a S3UserPolicy object, if {@code false} returns a S3BucketPolicy
     * @param key either the bucket key or the user ID
     * @return readPolicy
     */
    S3Policy readPolicy(SocketSender downstream, Connection connection, ByteString descriptor, boolean isUserPolicy, ByteString key){
        if(!isreadPolicyAllowed(downstream, connection, descriptor, isUserPolicy, key)){
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
    S3Policy readPolicyUnchecked(SocketSender downstream, ByteString descriptor, boolean isUserPolicy, ByteString securityBucket, ByteString policyKey){
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
    
    
    //--------------- Calls to DecisionProcedure -----------------
    //TODO : Romain : parse these calls inside the read/assign
    
    

    private boolean isOpACLAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean isAssign, boolean isBucketACL, ByteString bucket, ByteString key) {
        //get requested policies
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);
        Object userData = currentUserData(connection);
        S3BucketACL bucketACL = new S3BucketACL(currentUser,readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.bucketACL(bucket, currentUser)));
        //TODO : Romain : remove casts
        S3BucketPolicy bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, keyLink.securityBucket(bucket), keyLink.bucketPolicy());
        S3UserPolicy userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, keyLink.userBucket(domain), keyLink.userPolicy(key));
        
        if(isBucketACL){
            if(isAssign){
                return this.decisionprocedure.decideBucketACLAssign(domain, currentUser, userData, bucketACL, bucketPolicy, userPolicy);
            }else{
                return this.decisionprocedure.decideBucketACLRead(domain, currentUser, userData, bucketACL, bucketPolicy, userPolicy);
            }
        }else{
            S3ObjectACL objectACL = new S3ObjectACL(currentUser,readACLUnchecked(downstream, descriptor, bucket, key));
            if(isAssign){
                return this.decisionprocedure.decideObjectACLAssign(domain, currentUser, userData, objectACL, bucketACL, bucketPolicy, userPolicy);
            }else{
                return this.decisionprocedure.decideObjectACLRead( domain, currentUser, userData, objectACL, bucketACL, bucketPolicy, userPolicy);
            }
        }
    }

    private boolean isreadPolicyAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean userPolicy, ByteString key) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

    private boolean isAssignPolicyAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean userPolicy, ByteString key) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    

    @Override
    public Transformer newTransformer(Transformer downstream, Connection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain : intercept database calls
    }
}
