package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.decision.S3KeyLink;
import eu.antidotedb.client.messages.AntidoteRequest;
import eu.antidotedb.client.messages.AntidoteResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class extends the Access Monitor transformer to S3 Access Control smeantics
 * the differences in the management of user/bucket/object ACL/policies are hard coded
 * @author Romain
 */
public class S3AccessMonitor extends AccessMonitor{
    private final S3KeyLink keyLink=new S3KeyLink();
    private final Map<Connection,ByteString> domainMapping = new HashMap();
    
    
    public S3AccessMonitor(S3DecisionProcedure proc) {
        super(proc);
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
     * @param user
     * @param permissions 
     */
    void assignACL(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL,ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions){
        //TODO : Romain : check
        throw new UnsupportedOperationException("Not supported yet.");
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
        AntidotePB.ApbUpdateObjects policyUpdateOp = AntidotePB.ApbUpdateObjects.newBuilder()
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
        AntidoteRequest.MsgUpdateObjects request = AntidoteRequest.of(policyUpdateOp);
        //this bypasses transformers of the connection
        AntidoteResponse.Handler<AntidotePB.ApbOperationResp> responseExtractor = request.readResponseExtractor();
        AntidoteResponse response = request.accept(downstream);
        if (responseExtractor == null) {throw new IllegalStateException("Could not get response extractor for policy assign request");}
        if (response == null) {throw new AntidoteException("Missing response for " + request);}
        
        AntidotePB.ApbOperationResp operationResp = response.accept(responseExtractor);
        if (!operationResp.getSuccess()) {
            throw new AntidoteException("Could not perform policy update (error code: " + operationResp.getErrorcode() + ")");
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
     * @param user
     * @return permissions
     */
    Collection<? extends ByteString> readACL(SocketSender downstream, Connection connection, ByteString descriptor, boolean isBucketACL, ByteString bucket, ByteString key, ByteString user){
        //TODO : Romain : check
        throw new UnsupportedOperationException("Not supported yet.");
        //read
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
    
    void assignBucketPolicy(){
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    Collection<? extends ByteString> readBucketPolicy(){
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    void assignUserPolicy(){
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    Collection<? extends ByteString> readUserPolicy(){
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
        
    Collection<String> readUserPolicy(ByteString key) {
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

    Collection<String> readBucketPolicy(ByteString key) {
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    void assignUserPolicy(ByteString key, Collection<String> groups, Collection<String> statements) {
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

    void assignBucketPolicy(ByteString key, Collection<String> groups, Collection<String> statements) {
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    void policyMergerHelper(){
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    @Override
    void assignPermissions(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString txid, ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    /*
    process the Decision algorithm : 
    is the user the domain root ? Is the user known in this domain ? Is
    there any explicit deny ? Any explicit allow ?
    If needed, requests a group Policy
    */

}
