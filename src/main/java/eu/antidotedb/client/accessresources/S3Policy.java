package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.S3InteractiveTransaction;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import eu.antidotedb.client.decision.S3Request;
import java.util.List;

/**
 * Extends MVregister
 * Builder from ByteString
 * checks for Explicit Allow (user, op, userData)*
 * checks for Explicit deny
 * resolves concurrent updates and interprets the conditionBlocks
 * TODO : statement : user as ByteString, operations as ENUM type
 * TODO : statement for user policy management : currently needs a non-null bucket
 * @author romain-dumarais
 */
public abstract class S3Policy{

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
    
    
    //--------------------------------
    //      communication
    //--------------------------------
    
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
    
    //--------------------------------
    //        Decision process
    //--------------------------------
    
    public boolean explicitAllow(S3Request request){
        boolean isExplicitAllow=false;
        for(S3Statement statement:statements){
            if(statement.getEffect()){
                switch(request.action){
                //if(request.action.equals(READBUCKETACL) || request.action.equals(WRITEBUCKETACL) || request.action.equals(READBUCKETPOLICY) || request.action.equals(ASSIGNBUCKETPOLICY)){
                    //targetBucket key stored in resource bucket and in targetKey in request
                    case READBUCKETACL:
                    case READBUCKETPOLICY:
                    case ASSIGNBUCKETPOLICY:
                    case WRITEBUCKETACL:
                        if(statement.getActions().contains(request.action) && (statement.getPrincipals().contains(request.subject.toStringUtf8()) || statement.getPrincipals().contains("*")) 
                        && statement.getResourceBucket().equals(request.targetBucket)){
                            isExplicitAllow=true;
                        }
                        break;
                    //user ID stored in targetKey in request, in Resources in statement,does not need a current user check (only one policy to check)
                    case READUSERPOLICY:
                    case ASSIGNUSERPOLICY:
                        if(statement.getActions().contains(request.action) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            isExplicitAllow=true;
                        }
                        break;
                    default:
                        if(statement.getActions().contains(request.action) && statement.getPrincipals().contains(request.subject.toStringUtf8()) 
                            && statement.getResourceBucket().equals(request.targetBucket) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            isExplicitAllow=true;
                        }
                        break;
                }
                    //TODO : Romain : add condition Block
            }
        }
        return isExplicitAllow;
    }
    
    public boolean explicitDeny(S3Request request){
        boolean isExplicitDeny=false;
        for(S3Statement statement: statements){
            if(!statement.getEffect()){
                switch(request.action){
                //if(request.action.equals(READBUCKETACL) || request.action.equals(WRITEBUCKETACL) || request.action.equals(READBUCKETPOLICY) || request.action.equals(ASSIGNBUCKETPOLICY)){
                    //targetBucket key stored in resource bucket and in targetKey in request
                    case READBUCKETACL:
                    case READBUCKETPOLICY:
                    case ASSIGNBUCKETPOLICY:
                    case WRITEBUCKETACL:
                        if(statement.getActions().contains(request.action) && (statement.getPrincipals().contains(request.subject.toStringUtf8()) || statement.getPrincipals().contains("*")) 
                        && statement.getResourceBucket().equals(request.targetBucket)){
                            isExplicitDeny=true;
                        }
                        break;
                    //user ID stored in targetKey in request, in Resources in statement,does not need a current user check (only one policy to check)
                    case READUSERPOLICY:
                    case ASSIGNUSERPOLICY:
                        if(statement.getActions().contains(request.action) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            isExplicitDeny=true;
                        }
                        break;
                    default:
                        if(statement.getActions().contains(request.action) && statement.getPrincipals().contains(request.subject.toStringUtf8()) 
                            && statement.getResourceBucket().equals(request.targetBucket) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            isExplicitDeny=true;
                        }
                        break;
                }
                    //TODO : Romain : add condition Block
            }
        }
        return isExplicitDeny;
    }
    
    //--------------------------------
    //          Helpers
    //--------------------------------
    
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