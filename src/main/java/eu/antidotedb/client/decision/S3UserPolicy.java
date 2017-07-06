package eu.antidotedb.client.decision;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import java.util.Collection;
import java.util.List;

/**
 * class for UserPolicy Management, extends the S3Policy general class
 * @author Romain 
 */
public final class S3UserPolicy extends S3Policy {
    
    public S3UserPolicy(Collection<ByteString> policy) {
        super(policy);
    }
    
    public S3UserPolicy(List<ByteString> groups, List<Statement> statements) {
        super(groups, statements);
    }
    
    public static S3UserPolicy readPolicy(SecuredInteractiveTransaction tx, ByteString domain, ByteString userID){
        //Create a MVRegister with good reference, pass it to the PolicyReadHelper, then request it and parse the result
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    //hope we will have better semantics, like addStatement
    public void assignPolicy(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
