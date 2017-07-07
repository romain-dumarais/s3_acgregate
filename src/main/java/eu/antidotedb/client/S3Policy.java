package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.CrdtMVRegister;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.ValueCoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    //Romain : I need to choose the status of this class. Is it something that should be in the client package ?
    //extends CrdtMVregister<T>, or just MVRegisterRef ? Or takes this a private fields ?
    private List<Statement> statements;
    private List<ByteString> groups;
    
    public S3Policy(List<ByteString> groups, List<Statement> statements){
        this.groups=groups;
        this.statements=statements;
    }
    
    public List<ByteString> getGroups(){
        return this.groups;
    }
    
    public void addStatement(Statement statement){
        this.statements.add(statement);
    }
    
    public void addGroup(ByteString group){
        this.groups.add(group);
    }
    
    public void removeStatement(Statement statement){
        this.statements.remove(statement);
    }
    
    public void removeGroup(ByteString group){
        this.groups.remove(group);
    }
    
    public S3Policy readPolicy(S3InteractiveTransaction tx, ByteString key){
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void assignPolicy(S3InteractiveTransaction tx, ByteString key){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
    public boolean explicitAllow(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
}