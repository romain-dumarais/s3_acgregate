package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    /**
     * updates the current policy object read from the database
     * @param tx
     * @param bucketID
     * TODO : Romain : make static and not use a cast
     */
    @Override
    public void readPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        S3BucketPolicy remotePolicy = (S3BucketPolicy) tx.readPolicyHelper(false, bucketID);
        super.statements.clear(); super.groups.clear();
        remotePolicy.getGroups().stream().forEach((group) -> {super.addGroup(group);});
        remotePolicy.getStatements().stream().forEach((statement) -> {super.addStatement(statement);});
    }
    
    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx the transaction being used
     * @param bucketID key to which it is beig assigned
     */
    @Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString bucketID){
        tx.assignPolicyHelper(false, bucketID, this.encode());
    }
    
    @Override
    public void decode(String stringPolicy) {
        JsonObject value = Json.parse(stringPolicy).asObject();
        JsonArray jsonGroups = value.get("Groups").asArray();
        JsonArray jsonStatements = value.get("Statements").asArray();
        for(JsonValue jsongroup : jsonGroups){
            this.groups.add(ByteString.copyFromUtf8(jsongroup.asString()));
        }
        for(JsonValue jsonstatement : jsonStatements){
            this.statements.add(S3Statement.decodeStatic(jsonstatement.asObject()));
        }
    }
    
}
