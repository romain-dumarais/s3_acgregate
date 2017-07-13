package eu.antidotedb.client;

import eu.antidotedb.client.S3Policy;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

/**
 * class for UserPolicy Management, extends the S3Policy general class
 * @author Romain 
 */
public final class S3UserPolicy extends S3Policy {
    
    public S3UserPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    public S3UserPolicy(){
        super(new ArrayList<ByteString>(), new ArrayList<Statement>());
    }
    
    /**
     * returns a policy object with the statement and groups from a remote user in the database
     * @param tx
     * @param userID user from which the policy is requested
     * @return userPolicy
     */
    @Override
    public S3UserPolicy readPolicy(S3InteractiveTransaction tx, ByteString userID){
        //Romain : TODO : Create a MVRegister with good reference, pass it to the PolicyReadHelper, then request it and parse the result
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    /**
     * assigns a policy object to a remote user
     * @param tx current transaction
     * @param userkey user to which assign this policy
     */
    @Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString userkey){
        //Romain : TODO : Create a MVRegister with good reference, pass it to the PolicyAssignHelper, then assigns it
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
