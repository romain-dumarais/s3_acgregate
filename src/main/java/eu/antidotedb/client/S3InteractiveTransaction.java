package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.Collection;

/**
 * class for antidote interactive transactions inside S3-like access control model
 * @author Romain
 */
public class S3InteractiveTransaction extends SecuredInteractiveTransaction {
    S3AccessMonitor accessMonitor;
    
    public S3InteractiveTransaction(S3Client antidoteClient, S3AccessMonitor accessMonitor) {
        super(antidoteClient, accessMonitor);
    }
    
    @Override
    protected void onReleaseConnection(Connection connection) {
        super.onReleaseConnection(connection);
        accessMonitor.unsetCurrentUser(connection);
        accessMonitor.unsetUserData(connection);
        accessMonitor.unsetDomain(connection);
    }
    
    //access Resources Management
    public Collection<? extends ByteString> readObjectACLHelper(ByteString bucket, ByteString key, ByteString user){
        return accessMonitor.readObjectACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, key, user);
    }
    
    public void objectACLAssignHelper(ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions) {
        accessMonitor.assignObjectACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, key, user, permissions);
    }
    
    public Collection<? extends ByteString>  readBucketACLHelper(ByteString bucket, ByteString userid) {
        return accessMonitor.readBucketACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, userid);
    }
    
    public void bucketACLAssignHelper(ByteString user, ByteString bucket, Collection<ByteString> permissions) {
        accessMonitor.assignBucketACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, user, permissions);
    }

    public Collection<String> readPolicyHelper(ByteString key, boolean b) {
        if(b){return accessMonitor.readUserPolicy(key);}
        else{return accessMonitor.readBucketPolicy(key);}
    }

    public void assignPolicyHelper(ByteString key, boolean b, Collection<String> groups, Collection<String> statements) {
        if(b){accessMonitor.assignUserPolicy(key,groups,statements);}
        else{accessMonitor.assignBucketPolicy(key,groups,statements);}
    }
    
    @Override
    void policyAssignUncheckedHelper(ByteString user, ByteString bucket, ByteString key, Iterable<ByteString> permissions) {
        throw new AccessControlException("Operation not permitted");
    }
}
