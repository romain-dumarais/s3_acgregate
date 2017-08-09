package eu.antidotedb.client.accessresources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import eu.antidotedb.client.decision.S3Request;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class S3Policy implements S3AccessResource{

    protected List<S3Statement> statements;
    protected List<ByteString> groups;
    
    public S3Policy(List<ByteString> groups, List<S3Statement> statements){
        this.groups=groups;
        this.statements=statements;
    }
    
    public S3Policy(ByteString encodedValue){
        this.groups=new ArrayList<>();
        this.statements=new ArrayList<>();
        JsonObject value = Json.parse(encodedValue.toStringUtf8()).asObject();
        JsonArray jsonGroups = value.get("Groups").asArray();
        JsonArray jsonStatements = value.get("Statements").asArray();
        for(JsonValue jsongroup : jsonGroups){
            this.groups.add(ByteString.copyFromUtf8(jsongroup.asString()));
        }
        for(JsonValue jsonstatement : jsonStatements){
            this.statements.add(S3Statement.decodeStatic(jsonstatement.asObject()));
        }
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
    
    
    //--------------------------------
    //        Decision process
    //--------------------------------
    //TODO : Romain : refactor for isExplicitAllow --> return
    @Override
    public boolean explicitAllow(S3Request request){
        boolean isExplicitAllow=false;
        for(S3Statement statement:statements){
            if(statement.getEffect()){
                switch(request.action){
                    //targetBucket key stored in resource bucket and in targetKey in request
                    case READBUCKETACL:
                    case READBUCKETPOLICY:
                    case ASSIGNBUCKETPOLICY:
                    case WRITEBUCKETACL:
                        if(statement.getActions().contains(request.action) && (statement.getPrincipals().contains(request.subject.toStringUtf8()) || statement.getPrincipals().contains("*")) 
                        && statement.getResourceBucket().equals(request.targetBucket)){
                            if(!statement.getConditionBlock().isEmpty()){isExplicitAllow = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitAllow=true;}
                        }
                        break;
                    //user ID stored in targetKey in request, in Resources in statement,does not need a current user check (only one policy to check)
                    case READUSERPOLICY:
                    case ASSIGNUSERPOLICY:
                        if(statement.getActions().contains(request.action) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            if(!statement.getConditionBlock().isEmpty()){isExplicitAllow = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitAllow=true;}
                        }
                        break;
                    default:
                        if(statement.getActions().contains(request.action) && statement.getPrincipals().contains(request.subject.toStringUtf8()) 
                            && statement.getResourceBucket().equals(request.targetBucket) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            if(!statement.getConditionBlock().isEmpty()){isExplicitAllow = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitAllow=true;}
                        }
                        break;
                }
                    //TODO : Romain : add condition Block
            }
        }
        return isExplicitAllow;
    }
    
    @Override
    public boolean explicitDeny(S3Request request){
        boolean isExplicitDeny=false;
        for(S3Statement statement:statements){
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
                            if(!statement.getConditionBlock().isEmpty()){isExplicitDeny = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitDeny=true;}
                        }
                        break;
                    //user ID stored in targetKey in request, in Resources in statement,does not need a current user check (only one policy to check)
                    case READUSERPOLICY:
                    case ASSIGNUSERPOLICY:
                        if(statement.getActions().contains(request.action) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            if(!statement.getConditionBlock().isEmpty()){isExplicitDeny = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitDeny=true;}
                        }
                        break;
                    default:
                        if(statement.getActions().contains(request.action) && statement.getPrincipals().contains(request.subject.toStringUtf8()) 
                            && statement.getResourceBucket().equals(request.targetBucket) && (statement.getResources().contains(request.targetKey.toStringUtf8()) || statement.getResources().contains("*"))){
                            if(!statement.getConditionBlock().isEmpty()){isExplicitDeny = parseConditionBlock(statement.getConditionBlock(),request.userData);}
                            else{isExplicitDeny=true;}
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

    /**
     * naive interpretation ofthe condition block as an map of objects 
     * {key, value} and compare it to the userData map. if all values are 
     * contained in the userData, return true, else false
     * TODO : add more field : is in, is not, etc
     * TODO : get values of data in antidote > cf ACGreGate & Security layers
     * @param conditionBlock
     * @param userData
     * @return {@code true} if every pair is in the userData, {@code false} if not
     */
    private boolean parseConditionBlock(Map<String,String> conditionBlock, Map<String, ByteString> userData) {
        if(conditionBlock.isEmpty()){return true;}
        for(String key:conditionBlock.keySet()){
            if(!userData.get(key).toStringUtf8().equals(conditionBlock.get(key))){return false;}
        }
        return true;
    }
    
}