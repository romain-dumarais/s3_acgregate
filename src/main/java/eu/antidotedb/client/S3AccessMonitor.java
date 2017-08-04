package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.POLICY;
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
                    return readACLUnchecked(downstream, descriptor, aclRef);
                case READOBJECTACL:
                    aclRef = AntidotePB.ApbBoundObject.newBuilder().setBucket(S3KeyLink.securityBucket(targetObject.getBucket())).setKey(S3KeyLink.objectACL(targetObject.getKey(), user)).setType(POLICY).build();
                    return readACLUnchecked(downstream, descriptor, aclRef);
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
    private Collection<ByteString> readACLUnchecked(AntidoteRequest.Handler<AntidoteResponse> downstream, ByteString descriptor, AntidotePB.ApbBoundObject aclRef){
        AntidotePB.ApbReadObjects.Builder readRequest = AntidotePB.ApbReadObjects.newBuilder()
                    .setTransactionDescriptor(descriptor)
                    .addBoundobjects(aclRef);

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
    void assignPolicy(SocketSender downstream, Connection connection, ByteString descriptor, S3Operation operation, ByteString key, S3Policy policyObject){
        if(!isOpPolicyAllowed(downstream, connection, descriptor, operation, key)){
            throw new AccessControlException("Policy assign not allowed");
        }else{
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
            policyObject.addGroup(currentDomain(connection));
            ByteString policyValue = policyObject.encode();
                  
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
                resultPolicy = readPolicyUnchecked(downstream, descriptor, true, S3KeyLink.userBucket(currentDomain(connection)), S3KeyLink.userPolicy(key));
                break;
            case READBUCKETPOLICY:
                resultPolicy = readPolicyUnchecked(downstream, descriptor, false, S3KeyLink.securityBucket(key),S3KeyLink.bucketPolicy());
                break;
            default:
                throw new AccessControlException("unsupported operation");
            }
            //make the read-only domain flag not readable for users
            resultPolicy.removeGroup(currentDomain(connection));
            return resultPolicy;
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
                return new S3BucketPolicy();//arbitrary choice
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
    private boolean isOpObjectAllowed(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString descriptor, S3Operation operation, AntidotePB.ApbBoundObject targetObject) {
        //get AccessResources
        ByteString domain=currentDomain(connection);
        ByteString currentUser = currentUser(connection);

        //root credentials
        if(domain.equals(currentUser)){return true;}
        
        Collection<ByteString> bucketACL = readACLUnchecked(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.bucketACL(currentUser))
                .setType(POLICY)
                .build());
        Collection<ByteString> objectACL = readACLUnchecked(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                .setType(POLICY)
                .build());
        S3UserPolicy userPolicy; S3BucketPolicy bucketPolicy;//TODO : Romain : remove casts
        
        userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));
        bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, S3KeyLink.securityBucket(targetObject.getBucket()), S3KeyLink.bucketPolicy());
        //call decision Procedure
        switch(operation){
        case WRITEOBJECT:
            return this.decisionprocedure.decideObjectWrite(currentUser, targetObject, currentUserData(connection), objectACL, bucketACL, bucketPolicy, userPolicy);
        case READOBJECT:
            //TODO : Romain : pass operation to decicion procedure
            return this.decisionprocedure.decideObjectRead(currentUser, targetObject, currentUserData(connection), objectACL, bucketACL, bucketPolicy, userPolicy);
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
        if(domain.equals(currentUser)){return true;}
        
        Collection<ByteString> bucketACL = readACLUnchecked(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                .setKey(S3KeyLink.bucketACL(currentUser))
                .setType(POLICY)
                .build());
        Collection<ByteString> objectACL;
        
        //TODO : Romain : remove casts
        S3UserPolicy userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));
        S3BucketPolicy bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, S3KeyLink.securityBucket(targetObject.getBucket()), S3KeyLink.bucketPolicy());
        switch(operation){
            case READOBJECTACL:
                 objectACL = readACLUnchecked(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                         .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                         .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                         .setType(POLICY)
                         .build());
                return this.decisionprocedure.decideObjectACLRead(currentUser, targetObject, currentUserData(connection), objectACL, bucketACL, bucketPolicy, userPolicy);
            case WRITEOBJECTACL:
                objectACL = readACLUnchecked(downstream, descriptor, AntidotePB.ApbBoundObject.newBuilder()
                        .setBucket(S3KeyLink.securityBucket(targetObject.getBucket()))
                        .setKey(S3KeyLink.objectACL(targetObject.getKey(), currentUser))
                        .setType(POLICY)
                        .build());
                return this.decisionprocedure.decideObjectACLAssign(currentUser, targetObject, currentUserData(connection), objectACL, bucketACL, bucketPolicy, userPolicy);
            case READBUCKETACL:
                return this.decisionprocedure.decideBucketACLRead(currentUser, targetObject, currentUserData(connection), bucketACL, bucketPolicy, userPolicy);
            case WRITEBUCKETACL:
                return this.decisionprocedure.decideBucketACLAssign(currentUser, targetObject, currentUserData(connection), bucketACL, bucketPolicy, userPolicy);
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
        if(domain.equals(currentUser)){return true;}
        
        //TODO : Romain : remove casts
        S3BucketPolicy bucketPolicy;
        S3UserPolicy userPolicy = (S3UserPolicy) readPolicyUnchecked(downstream, descriptor, true, S3KeyLink.userBucket(domain), S3KeyLink.userPolicy(currentUser));
        switch(operation){
            case ASSIGNUSERPOLICY:
                return this.decisionprocedure.decideUserPolicyAssign(key, userPolicy, currentUserData(connection));
            case READUSERPOLICY:
                return this.decisionprocedure.decideUserPolicyRead(key, userPolicy, currentUserData(connection));
            case ASSIGNBUCKETPOLICY:
                bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, S3KeyLink.securityBucket(key), S3KeyLink.bucketPolicy());
                return this.decisionprocedure.decideBucketPolicyAssign(currentUser, key, currentUserData(connection), bucketPolicy, userPolicy);
            case READBUCKETPOLICY:
                bucketPolicy = (S3BucketPolicy) readPolicyUnchecked(downstream, descriptor, false, S3KeyLink.securityBucket(key), S3KeyLink.bucketPolicy());
                return this.decisionprocedure.decideBucketPolicyRead(currentUser, key, currentUserData(connection), bucketPolicy, userPolicy);
            default:
                throw new AccessControlException("unsupported operation");
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
            
            //TODO : Romain : handle Static transactions
           
        };
    }
    
    //----------------------------------------------
    //              Resources Lifecycle
    //----------------------------------------------
    

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
        S3UserPolicy flag=new S3UserPolicy(); flag.addGroup(domain);
        domainflag=flag.encode();}
        else{policyBucket=S3KeyLink.securityBucket(targetKey);
        policyKey=S3KeyLink.bucketPolicy();
        S3BucketPolicy flag=new S3BucketPolicy(); flag.addGroup(domain);
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
