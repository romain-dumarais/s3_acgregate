package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import static eu.antidotedb.client.accessresources.S3Operation.ASSIGNBUCKETPOLICY;
import static eu.antidotedb.client.accessresources.S3Operation.READBUCKETPOLICY;
import java.util.ArrayList;
import java.util.List;

/**
 * class for Bucket Policy Management, extends the S3Policy abstract class
 * @author romain-dumarais
 */
public final class S3BucketPolicy extends S3Policy{
    
    public S3BucketPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    public S3BucketPolicy(){
        super(new ArrayList<>(), new ArrayList<>());
    }
    
    public S3BucketPolicy(ByteString encodedValue){
        super(encodedValue);
    }
    
    /**
     * updates the current policy object read from the database
     * @param tx
     * @param bucketID
     * TODO : Romain : make static and not use a cast
     */
    //@Override
    public void readPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        S3BucketPolicy remotePolicy = (S3BucketPolicy) tx.readPolicyHelper(READBUCKETPOLICY, bucketID);
        super.statements.clear(); super.groups.clear();
        remotePolicy.getGroups().stream().forEach((group) -> {super.addGroup(group);});
        remotePolicy.getStatements().stream().forEach((statement) -> {super.addStatement(statement);});
    }
    
    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx the transaction being used
     * @param bucketID key to which it is beig assigned
     */
    //@Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        tx.assignPolicyHelper(ASSIGNBUCKETPOLICY, bucketID, this);
    }
    
}
