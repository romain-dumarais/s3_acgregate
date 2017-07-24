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
    
    protected Collection<? extends ByteString> readObjectACLHelper(ByteString bucket, ByteString key, ByteString user){
        return accessMonitor.readObjectACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, key, user);
    }
    
    protected void objectACLAssignHelper(ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions) {
        accessMonitor.assignObjectACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, key, user, permissions);
    }
    
    protected Collection<? extends ByteString>  readBucketACLHelper(ByteString bucket, ByteString userid) {
        return accessMonitor.readBucketACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, userid);
    }
    
    protected void bucketACLAssignHelper(ByteString user, ByteString bucket, Collection<ByteString> permissions) {
        accessMonitor.assignBucketACL(new SocketSender(connection.getSocket()), connection, getDescriptor(), bucket, user, permissions);
    }

    //TODO : Romain : Policies
    
    @Override
    void policyAssignUncheckedHelper(ByteString user, ByteString bucket, ByteString key, Iterable<ByteString> permissions) {
        throw new AccessControlException("Operation not permitted");
    }

    
    
}
