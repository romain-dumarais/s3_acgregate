package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;

/**
 * class for handling request
 * @author romain-dumarais
 */
public class S3Request {
    private final ByteString subject,targetBucket,targetKey;
    private final String action;
    private final Object userData;
    
    
    public S3Request(ByteString subject, String action, ByteString targetBucket, ByteString targetKey, Object userData){
        this.action=action;
        this.subject=subject;
        this.targetBucket=targetBucket;
        this.targetKey=targetKey;
        this.userData=userData;
    }
    
    
}
