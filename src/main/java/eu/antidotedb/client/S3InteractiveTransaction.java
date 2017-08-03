package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Operation;
import eu.antidotedb.client.accessresources.S3Policy;
import java.util.Collection;

/**
 * class for antidote interactive transactions inside S3-like access control model
 * @author romain-dumarais
 * TODO : Romain : use bound object to make changes atomics
 */
public class S3InteractiveTransaction extends InteractiveTransaction {
    private final S3AccessMonitor accessMonitor;
    
    public S3InteractiveTransaction(S3Client antidoteClient, S3AccessMonitor accessMonitor) {
        super(antidoteClient);
        this.accessMonitor=accessMonitor;
    }
    
    @Override
    protected void onReleaseConnection(Connection connection) {
        super.onReleaseConnection(connection);
        accessMonitor.unsetCurrentUser(connection);
        accessMonitor.unsetUserData(connection);
        accessMonitor.unsetDomain(connection);
    }
    
    //ACCESS RESOURCE MANAGEMENT HELPERS
    /**
     * forwards the read ACL request to the AccessMonitor, for object or bucket ACL
     * @param operation
     * @param bucket
     * @param key object key for an objct ACL, null for a BucketACL
     * @param userid ID of the user for which the ACL is requested 
     * @return permissions set of permissions for the user {@param userID} and the requested resource
     */
    public Collection<? extends ByteString> readACLHelper(S3Operation operation, ByteString bucket, ByteString key, ByteString userid){
        return accessMonitor.readACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), operation, bucket, key, userid);
    }
    
    /**
     * forwards the assign request to the AccessMonitor, for object or bucket ACL
     * @param operation
     * @param bucket
     * @param targetObject
     * @param targetUser ID of the user for which the ACL is assigned 
     * @param permissions set of permissions for the user {@param userID} and the requested resource
     */
    public void assignACLHelper(S3Operation operation, ByteString bucket, ByteString targetObject, ByteString targetUser, Collection<ByteString> permissions){
        accessMonitor.assignACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), operation, bucket, targetObject, targetUser, permissions);
    }

    public S3Policy readPolicyHelper(S3Operation operation, ByteString key) {
        return accessMonitor.readPolicy(new SocketSender(connection.getSocket()), connection, getDescriptor(), operation, key);
    }

    public void assignPolicyHelper(S3Operation operation, ByteString key, ByteString policyValue) {
        accessMonitor.assignPolicy(new SocketSender(connection.getSocket()), connection, getDescriptor(), operation, key,policyValue);
    }
    
}
