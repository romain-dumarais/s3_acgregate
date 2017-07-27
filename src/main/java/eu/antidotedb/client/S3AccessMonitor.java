package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class extends the Access Monitor transformer to S3 Access Control smeantics
 * the differences in the management of user/bucket/object ACL/policies are hard coded
 * @author romain-dumarais
 */
public class S3AccessMonitor extends AccessMonitor{
    private final S3KeyLink keyLink=new S3KeyLink();
    private final Map<Connection,ByteString> domainMapping = new HashMap();
    
    
    public S3AccessMonitor(S3DecisionProcedure proc) {
        super(proc);
    }
    
    private ByteString currentDomain(Connection connection){
        return domainMapping.get(connection);
    }
    
    void setDomain(Connection connection, ByteString domain) {
        domainMapping.put(connection,domain);
    }
    
    void unsetDomain(Connection connection) {
        domainMapping.remove(connection);
    }
    
    /*
    Override the communication with the Protocol Buffer
    requests the different Access Ressources in the security Bucket and domain Bucket
    prevent to write directly in the Security Bucket.*/
    
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
        if(isAssignACLAllowed(downstream, connection, descriptor, isBucketACL, bucket, key)){ 
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
        if(!isreadACLAllowed(downstream, connection, descriptor, isBucketACL, bucket, key)){
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
            ArrayList<S3Policy> policiesList = new ArrayList<>();
            if(isUserPolicy){
                Collection<? extends ByteString> concurrentPolicies = readPolicyUnchecked(downstream, descriptor, keyLink.userBucket(currentDomain(connection)), keyLink.userPolicy(key));
                for(ByteString stringPolicy:concurrentPolicies){
                    S3UserPolicy userPolicy = new S3UserPolicy();
                    userPolicy.decode(stringPolicy.toStringUtf8());
                    policiesList.add(userPolicy);
                }
            }else{
                Collection<? extends ByteString> concurrentPolicies = readPolicyUnchecked(downstream, descriptor, keyLink.securityBucket(key),keyLink.bucketPolicy());
                for(ByteString stringPolicy:concurrentPolicies){
                    S3BucketPolicy bucketPolicy = new S3BucketPolicy();
                    bucketPolicy.decode(stringPolicy.toStringUtf8());
                    policiesList.add(bucketPolicy);
                }
            }
            return policyMergerHelper(policiesList, isUserPolicy);
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
    Collection<? extends ByteString> readPolicyUnchecked(SocketSender downstream, ByteString descriptor, ByteString securityBucket, ByteString policyKey){
        //TODO : Romain : readPolicy
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(AntidotePB.ApbBoundObject.newBuilder()
                    .setBucket(securityBucket)
                    .setKey(policyKey)
                    .setType(AntidotePB.CRDT_type.MVREG));

        AntidotePB.ApbReadObjectsResp policyResp = downstream.handle(readRequest
                    .build()).accept(new AntidoteResponse.MsgReadObjectsResp.Extractor());
        return policyResp.getObjects(0).getMvreg().getValuesList();
    }
    
    /**
     * helper to merge concurrent updates for Policy objects
     * @param policies set of concurrent objects
     * @param isUserPolicy flag for return Policy type
     * @return minimalPolicy the policy object with the intersection of the groups and statements
     */
    public S3Policy policyMergerHelper(List<S3Policy> policies, boolean isUserPolicy){
        //TODO : Romain : switch to private
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
    
    @Override
    void assignPermissions(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString txid, ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions) {
        throw new UnsupportedOperationException("Not supported : old API"); //TODO : Romain
    }
    
    /*
    process the Decision algorithm : 
    is the user the domain root ? Is the user known in this domain ? Is
    there any explicit deny ? Any explicit allow ?
    If needed, requests a group Policy
    */

    private boolean isreadACLAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL, ByteString bucket, ByteString key) {
        //get requested policies
        boolean accessDecision=false;
        ByteString domain=currentDomain(connection);
        ByteString currentUser = super.currentUser(connection);
        Object userData = super.currentUserData(connection);
        S3BucketACL bucketACL = readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), keyLink.bucketACL(bucket, currentUser));
        S3BucketPolicy bucketPolicy;
        S3UserPolicy userPolicy;
        
        if(isBucketACL){
            accessDecision = this.procedure.decideBucketACLRead(domain, currentUser, userData, bucketACL, bucketPolicy, userPolicy);
        }else{
            S3ObjectACL objectACL;
            accessDecision = this.procedure.decideObjectACLRead( domain, currentUser, userData, objectACL, bucketACL, bucketPolicy, userPolicy);
        }
        /*String operation;
        if(isBucketACL){operation="readBucketACL";}else{operation="readObjectACL";}
        S3BucketACL bucketACL;
        S3Request request=new S3Request(currentDomain(connection), super.currentUser(connection), super.currentUserData(connection), bucket, key, operation);
        if(!isBucketACL){
            S3ObjectACL objectACL;
            readACLUnchecked(downstream, descriptor, keyLink.securityBucket(bucket), key, key);
            request.addObjectACL(objectACL);
        }
        request.addBucketACL(bucketACL);
        request.addBucketPolicy();
        request.addUserPolicy();*/
        return accessDecision;
    }

    private boolean isAssignACLAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBbucketACL, ByteString bucket, ByteString key) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

    private boolean isreadPolicyAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean userPolicy, ByteString key) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

    private boolean isAssignPolicyAllowed(SocketSender downstream, Connection connection, ByteString descriptor, boolean userPolicy, ByteString key) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
}
