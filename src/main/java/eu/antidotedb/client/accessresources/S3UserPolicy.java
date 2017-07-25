package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * class for UserPolicy Management, extends the S3Policy general class
 * @author Romain 
 */
public final class S3UserPolicy extends S3Policy {
    
    public S3UserPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    public S3UserPolicy(){
        super(new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * updates the current policy object with the statement and groups from a remote user in the database
     * @param tx
     * @param userID user from which the policy is requested
     */
    @Override
    public void readPolicy(S3InteractiveTransaction tx, ByteString userID){
        Collection<String> policy = tx.readPolicyHelper(userID,true);
        List<S3Statement> policystatements= new ArrayList<>();
        List<ByteString> policyGroups = new ArrayList<>();
        //TODO : Romain : parse JSON result
        
        super.statements.clear(); super.groups.clear();
        policyGroups.stream().forEach((group) -> {super.addGroup(group);});
        policystatements.stream().forEach((statement) -> {super.addStatement(statement);});
    }
    
    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx current transaction
     * @param userkey user to which assign this policy
     */
    @Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString userkey){
        Set<String> policygroups=new HashSet<>(), policystatements=new HashSet<>();
        //TODO : Romain : parse to JSON objects
        tx.assignPolicyHelper(userkey,true,policygroups,policystatements);
        throw new UnsupportedOperationException("not implemented yet");
    }
}
