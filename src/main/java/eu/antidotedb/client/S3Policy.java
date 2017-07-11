package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import java.util.List;

/**
 * Extends MVregister
• Builder from ByteString
• checks for Explicit Allow (user, op, userData)*
• checks for Explicit deny*
* resolves concurrent updates and interprets the conditionBlocks
 * 
 * @author Romain
 * TODO : everything
 */
public abstract class S3Policy {
    private List<S3Statement> statements;
    private List<ByteString> groups;
    
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
    
    public S3Policy readPolicy(S3InteractiveTransaction tx, ByteString key){
        //TODO : create a MVreg object and read it, via a dedicated helper in the S3InteractiveTransaction
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void assignPolicy(S3InteractiveTransaction tx, ByteString key){
        //TODO : create a MVreg object and update it, via a dedicated helper in the S3InteractiveTransaction
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
    public boolean explicitAllow(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
}