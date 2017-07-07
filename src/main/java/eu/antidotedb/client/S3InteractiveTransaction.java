package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.S3AccessMonitor;
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
    
    void objectACLAssignHelper(ByteString user, ByteString bucket, ByteString key, Collection<ByteString> permissions) {
        super.policyAssignHelper(user, bucket, key, permissions);
    }
    
    void bucketACLAssignHelper(ByteString user, ByteString bucket, Collection<ByteString> permissions) {
        accessMonitor.assignBucketACLPermissions(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, user, permissions);
    }

    @Override
    void policyAssignUncheckedHelper(ByteString user, ByteString bucket, ByteString key, Iterable<ByteString> permissions) {
        throw new AccessControlException("Operation not permitted");
    }
    
}
