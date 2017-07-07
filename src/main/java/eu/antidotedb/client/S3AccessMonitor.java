package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.ObjectInBucket;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.decision.S3KeyLink;
import java.util.Collection;

/**
 * This class extends the Access Monitor transformer to S3 Access Control smeantics
 * @author Romain
 * TODO : Romain : everything
 * TODO : Romain : how get AccessResources ?
 */
public class S3AccessMonitor extends AccessMonitor{
    private final S3KeyLink keyLink=new S3KeyLink();
    private final S3AccessResources resources=new S3AccessResources();
    
    
    public S3AccessMonitor(S3DecisionProcedure proc) {
        super(proc);
    }
    
    /*
    Override the communication with the Protocol Buffer
    requests the different Access Ressources in the security Bucket and domain Bucket
    prevent to write directly in the Security Bucket.
    process the Decision algorithm : is the user the domain root ? Is the user known in this domain ? Is
    there any explicit deny ? Any explicit allow ?
    If needed, requests a group Policy
    */
    
    
    void assignBucketACLPermissions(SocketSender socketSender, Connection connection, ByteString descriptor, ByteString bucket, ByteString user, Collection<ByteString> permissions) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void unsetDomain(Connection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
