package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.ObjectInBucket;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.decision.S3KeyLink;

/**
 * This class extends the Access Monitor transformer to S3 Access Control smeantics
 * @author Romain
 * TODO : Romain : everything
 * TODO : Romain : how get AccessResources ?
 */
public class S3AccessMonitor extends AccessMonitor{
    private final S3KeyLink keyLink=new S3KeyLink();
    
    
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
    
    
    
    /*
    Romain : where to put this ?
    //TODO : Romain : should be used in a transaction. Why not a helper ?
    public void assignACL(ObjectInBucket object, Iterable<? extends ByteString> permissions){
        //TODO : Romain : define args using assignPermissions + Policy.assign
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    //TODO : Romain : should be used in a transaction. Why not a helper ?
    public void assignACL(ByteString target_object, ByteString bucket, Iterable<? extends ByteString> permissions){
        //TODO : Romain : define args using assignPermissions
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public void assignPolicy(){
        //TODO : Romain : define args using assignPermissions
        throw new UnsupportedOperationException("not implemented yet");
    }*/
    
}
