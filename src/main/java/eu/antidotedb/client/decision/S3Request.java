package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Operation;

/**
 * class for handling request
 * @author romain-dumarais
 */
public final class S3Request {
    public final ByteString subject,targetBucket,targetKey;
    public final S3Operation action;
    public final Object userData;
    
    
    public S3Request(ByteString subject, S3Operation action, ByteString targetBucket, ByteString targetKey, Object userData){
        this.action=action;
        this.subject=subject;
        this.targetBucket=targetBucket;
        this.targetKey=targetKey;
        this.userData=userData;
    }
    
    
}
