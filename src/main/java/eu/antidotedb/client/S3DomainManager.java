package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.S3KeyLink;

/**
 * Class for operation with domain root credentials
 * CreateBucket (bucket_key) : initializes a bucket by setting the domain flag
 * DeleteBucket ( bucket_key) : deletes a bucket by setting the domain flag
 * start Transaction : create S3 Interactive Transactions with root credentials
 * @author romain-dumarais
 * TODO : everything
 */
public class S3DomainManager{
    private final ByteString domain;
    private final S3KeyLink keyLink = new S3KeyLink();

    S3DomainManager(ByteString domain) {
        this.domain = domain;
    }
    //TODO : Romain : a way to list the users 
    
    public void createBucket(ByteString bucketKey, SecuredInteractiveTransaction tx){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public void deleteBucket(ByteString bucketKey, SecuredInteractiveTransaction tx){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public void createUser(ByteString userKey, SecuredInteractiveTransaction tx){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public void deleteUser(ByteString userKey, SecuredInteractiveTransaction tx){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    // TODO : rootTransaction : create SecuredTransactions with root credentials
    // copy from S3 client
    
    
    //reduced API to start root transactions
    public S3InteractiveTransaction startTransaction(){
        //TODO : Romain : start transaction with user = domain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    //get keylink mapping
    public ByteString getsecurityBucket(ByteString bucketKey){
        return this.keyLink.dataBucket(bucketKey);
    }
    public ByteString getdataBucket(ByteString bucketKey){
        return this.keyLink.dataBucket(bucketKey);
    }
    
    public ByteString getuserBucket(){
        return this.keyLink.userBucket(domain);
    }
    
    public ByteString getobjectACL(ByteString objectKey, ByteString userID){
        return this.keyLink.objectACL(objectKey, userID);
    }
    
    public ByteString getbucketACL(ByteString bucketKey, ByteString userID){
        return this.keyLink.bucketACL(bucketKey, userID);
    }
    
    public ByteString getbucketPolicy(){
        return this.keyLink.bucketPolicy();
    }
    
    public ByteString getuserPolicy(ByteString user){
        return this.keyLink.userPolicy(user);
    }
}
