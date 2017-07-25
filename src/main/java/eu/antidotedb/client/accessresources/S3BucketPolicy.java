package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * class for Bucket Policy Management, extends the S3Policy abstract class
 * @author Romain
 */
public final class S3BucketPolicy extends S3Policy{
    
    
    public S3BucketPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    public S3BucketPolicy(){
        super(new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * updates the current policy object read from the database
     * @param tx
     * @param bucketID
     */
    @Override
    public void readPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        Collection<String> policy = tx.readPolicyHelper(bucketID,false);
        List<S3Statement> policystatements= new ArrayList<>();
        List<ByteString> policyGroups = new ArrayList<>();
        //TODO : Romain : parse JSON result
        super.statements.clear(); super.groups.clear();
        policyGroups.stream().forEach((group) -> {super.addGroup(group);});
        policystatements.stream().forEach((statement) -> {super.addStatement(statement);});
    }
    
    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx the transaction being used
     * @param bucketID key to which it is beig assigned
     */
    @Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        Set<String> policygroups=new HashSet<>(), policystatements=new HashSet<>();
        //TODO : Romain : parse to JSON values
        tx.assignPolicyHelper(bucketID,false,policygroups,policystatements);
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}
