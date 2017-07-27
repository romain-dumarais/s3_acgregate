package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.Collection;

/**
 * class for antidote interactive transactions inside S3-like access control model
 * @author romain-dumarais
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
    
    //ACCESS RESOURCE MANAGEMENT HELPERS
    /**
     * forwards the read ACL request to the AccessMonitor, for object or bucket ACL
     * @param isBucketACL {@code true} if the requested ACL is a bucket ACL, {@code false} for an object ACL
     * @param bucket
     * @param key object key for an objct ACL, null for a BucketACL
     * @param userid ID of the user for which the ACL is requested 
     * @return permissions set of permissions for the user {@param userID} and the requested resource
     */
    public Collection<? extends ByteString> readACLHelper(boolean isBucketACL, ByteString bucket, ByteString key, ByteString userid){
        return accessMonitor.readACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), isBucketACL, bucket, key, userid);
    }
    /**
     * forwards the assign request to the AccessMonitor, for object or bucket ACL
     * @param isBucketACL {@code true} if the requested ACL is a bucket ACL, {@code false} for an object ACL
     * @param bucket
     * @param key
     * @param userid ID of the user for which the ACL is assigned 
     * @param permissions set of permissions for the user {@param userID} and the requested resource
     */
    public void assignACLHelper(boolean isBucketACL, ByteString bucket, ByteString key, ByteString userid, Collection<ByteString> permissions){
        accessMonitor.assignACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), isBucketACL, bucket, key, userid, permissions);
    }

    public S3Policy readPolicyHelper(boolean isUserPolicy, ByteString key) {
        return accessMonitor.readPolicy(new SocketSender(connection.getSocket()), connection, getDescriptor(), isUserPolicy, key);
    }

    public void assignPolicyHelper(boolean isUserPolicy, ByteString key, ByteString policyValue) {
        accessMonitor.assignPolicy(new SocketSender(connection.getSocket()), connection, getDescriptor(), isUserPolicy, key,policyValue);
    }
    
    @Override
    void policyAssignUncheckedHelper(ByteString user, ByteString bucket, ByteString key, Iterable<ByteString> permissions) {
        throw new AccessControlException("Operation not permitted");
    }
}
