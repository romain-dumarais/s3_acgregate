package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import static eu.antidotedb.client.accessresources.S3Operation.ASSIGNUSERPOLICY;
import static eu.antidotedb.client.accessresources.S3Operation.READUSERPOLICY;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * class for UserPolicy Management, extends the S3Policy general class
 * @author romain-dumarais 
 */
public final class S3UserPolicy extends S3Policy {
    
    public S3UserPolicy(List<ByteString> groups, List<S3Statement> statements) {
        super(groups, statements);
    }
    
    public S3UserPolicy(){
        super(new ArrayList<>(), new ArrayList<>());
    }
    
    public S3UserPolicy(ByteString encodedValue){
        super(encodedValue);
    }
    
    /**
     * updates the current policy object with the statement and groups from a remote user in the database
     * @param tx
     * @param userID user from which the policy is requested
     */
    //@Override
    public void readPolicy(S3InteractiveTransaction tx, ByteString userID){
        S3UserPolicy remotePolicy = (S3UserPolicy) tx.readPolicyHelper(READUSERPOLICY, userID);
        super.statements.clear(); super.groups.clear();
        remotePolicy.getGroups().stream().forEach((group) -> {super.addGroup(group);});
        remotePolicy.getStatements().stream().forEach((statement) -> {super.addStatement(statement);});
    }
    
    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx current transaction
     * @param userkey user to which assign this policy
     */
    //@Override
    public void assignPolicy(S3InteractiveTransaction tx, ByteString userkey){
        tx.assignPolicyHelper(ASSIGNUSERPOLICY, userkey, this);
    }
    
    /*
    @Override
    public void decode(String stringPolicy) {
        JsonObject value = Json.parse(stringPolicy).asObject();
        this.statements.clear(); this.groups.clear();
        JsonArray jsonGroups = value.get("Groups").asArray();
        JsonArray jsonStatements = value.get("Statements").asArray();
        for(JsonValue jsongroup : jsonGroups){
            this.groups.add(ByteString.copyFromUtf8(jsongroup.asString()));
        }
        for(JsonValue jsonstatement : jsonStatements){
            this.statements.add(S3Statement.decodeStatic(jsonstatement.asObject()));
        }
    }   */
}