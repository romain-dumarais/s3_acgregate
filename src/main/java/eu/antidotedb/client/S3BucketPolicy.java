package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import java.util.List;

/**
 * class for Bucket Policy Management, extends the S3Policy abstract class
 * @author Romain
 */
public final class S3BucketPolicy extends S3Policy{
    
    
    public S3BucketPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    /**
     * returns a policy object read from the database
     * @param tx
     * @param bucketID
     * @return bucketPlicy policy object from the @bucketID bucket
     */
    @Override
    public S3BucketPolicy readPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        //Create a MVRegister with good reference, pass it to the PolicyReadHelper, then request it and parse the result
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    /**
     * assigns the current bucketPolicy Object to a remote user.
     * @param tx the transaction being used
     * @param bucketkey key to which it is beig assigned
     */
    @Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString bucketkey){
        //Romain : TODO : Create a MVRegister with good reference, pass it to the PolicyReadHelper, then request it and parse the result
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
