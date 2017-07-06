package eu.antidotedb.client.decision;

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
public class S3Policy extends CrdtMVRegister{
    //Romain : I need to choose the status of this class. Is it something that should be in the client package ?
    //extends CrdtMVregister<T>, or just MVRegisterRef ? Or takes this a private fields ?
    private List<Statement> statements;
    private List<ByteString> groups;
    //attributes like array

    //Romain : do I need this ?
    public S3Policy(Collection<ByteString> policy){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public S3Policy(List<ByteString> groups, List<Statement>){
        this.groups=groups;
        this.statements=statements;
    }

    
    public boolean explicitAllow(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(/*all the needed args + optional userData*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
}