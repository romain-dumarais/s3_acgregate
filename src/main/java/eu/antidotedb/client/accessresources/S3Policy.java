package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.decision.S3Request;
import java.util.List;

/**
 * Extends MVregister
 * Builder from ByteString
 * checks for Explicit Allow (user, op, userData)*
 * checks for Explicit deny
 * resolves concurrent updates and interprets the conditionBlocks
 * @author romain-dumarais
 */
public abstract class S3Policy {

    //TODO : Romain : domain flag read-only
    protected List<S3Statement> statements;
    protected List<ByteString> groups;
    
    public S3Policy(List<ByteString> groups, List<S3Statement> statements){
        this.groups=groups;
        this.statements=statements;
    }
    
    public List<ByteString> getGroups(){
        return this.groups;
    }
    
    public void addStatement(S3Statement statement){
        this.statements.add(statement);
    }
    
    public void addGroup(ByteString group){
        this.groups.add(group);
    }
    
    public void removeStatement(S3Statement statement){
        this.statements.remove(statement);
    }
    
    public void removeGroup(ByteString group){
        this.groups.remove(group);
    }
    
    /**
     * updates the current policy object with a remote Policy value
     * @param tx the current transaction
     * @param key either the bucket key or the userID
     */
    public abstract void readPolicy(S3InteractiveTransaction tx, ByteString key);

    /**
     * assigns the current Policy object value to the remote policy 
     * @param tx the current transaction
     * @param key either the bucket key or the userID
     */
    public abstract void assignPolicy(S3InteractiveTransaction tx, ByteString key);
    
    public ByteString encode(){
        JsonArray jsongroups = Json.array(), jsonstatements = Json.array();
        this.groups.stream().forEach((group) -> {
            jsongroups.add(group.toStringUtf8());
        });
        this.statements.stream().forEach((stat) -> {
            jsonstatements.add(stat.encode());
        });
        JsonObject jsonPolicy = Json.object();
        jsonPolicy.add("Groups", jsongroups);
        jsonPolicy.add("Statements", jsonstatements);
        return ByteString.copyFromUtf8(jsonPolicy.toString());
    }
    
    public abstract void decode(String stringPolicy);
    
    public boolean explicitAllow(S3Request request){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(S3Request request){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
    
    //helpers for tests
    public List<S3Statement> getStatements(){
        return this.statements;
    }
    public S3Statement getStatement(int index){
        return this.statements.get(index);
    }
    public ByteString getGroup(int index){
        return this.groups.get(index);
    }

    //helpers for comparison
    public boolean containsGroup(ByteString group) {
        return this.groups.contains(group);
    }
    public boolean containsStatement(S3Statement statement) {
        return this.statements.contains(statement);
    }
}