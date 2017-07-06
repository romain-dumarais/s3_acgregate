package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import java.util.Collection;
import java.util.List;

/**
 * class for Bucket Policy Management, extends the S3Policy general class
 * @author Romain
 */
public class S3BucketPolicy extends S3Policy{
    
    public S3BucketPolicy(Collection<ByteString> policy) {
        super(policy);
    }
    
    public S3BucketPolicy(List<ByteString> groups, List<Statement> statements) {
        super(groups, statements);
    }
    
    public static S3BucketPolicy readPolicy(SecuredInteractiveTransaction tx, ByteString domain, ByteString bucketID){
        //Create a MVRegister with good reference, pass it to the PolicyReadHelper, then request it and parse the result
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    //hope we will have better semantics, like addStatement
    public void assignPolicy(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
