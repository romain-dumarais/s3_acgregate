package eu.antidotedb.client;

import com.google.protobuf.ByteString;
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
        domainMapping.put(connection,domain); //TODO : Romain
    }
    
    void unsetDomain(Connection connection) {
        domainMapping.remove(connection);
    }
    
    /*
    Override the communication with the Protocol Buffer
    requests the different Access Ressources in the security Bucket and domain Bucket
    prevent to write directly in the Security Bucket.*/
    /*
    process the Decision algorithm : 
    is the user the domain root ? Is the user known in this domain ? Is
    there any explicit deny ? Any explicit allow ?
    If needed, requests a group Policy
    */
    
    
    void assignObjectACL(SocketSender socketSender, Connection connection, ByteString descriptor, ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions){
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    Collection<? extends ByteString> readObjectACL(SocketSender socketSender, Connection connection, ByteString descriptor, ByteString bucket, ByteString key, ByteString user){
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    void assignBucketACL(SocketSender socketSender, Connection connection, ByteString descriptor, ByteString bucket, ByteString user, Collection<ByteString> permissions) {
        //check
        //assign
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }
    
    Collection<? extends ByteString> readBucketACL(SocketSender socketSender, Connection connection, ByteString descriptor, ByteString bucket, ByteString user){
        //check
        //read
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
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

    /*
    //TODO : Romain : i don't think it is a good idea
    Policy bucketACLPolicyCreator(ByteString bucket, ByteString userid) {
        Policy bucketACL = new Policy(this.keyLink.securityBucket(bucket),this.keyLink.bucketACL(bucket, userid),ValueCoder.utf8String);
        return bucketACL;
    }*/
    
    @Override
    void assignPermissions(AntidoteRequest.Handler<AntidoteResponse> downstream, Connection connection, ByteString txid, ByteString bucket, ByteString key, ByteString user, Collection<ByteString> permissions) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO : Romain
    }

}
