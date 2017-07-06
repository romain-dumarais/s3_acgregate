package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.transformer.StaticInteractiveTransformer;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.transformer.TransformerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Interface to use the S3 Access Control over Antidote
 * creates the transaction with a userID, domainID [optional userData]
 * loginAsRoot → return an interface for root operations
 * TODO : everything
 * @author Romain from a model from mweber_ukl
 */
public final class S3Client extends SecureAntidoteClient{
    
    /*
    Creates the transactions, with a
loginAsRoot → return a Domain instance
prevent to create Txn with the domain name as user ID.
    */
    //BUILDERS
    public S3Client(Host... hosts) {
        this(Collections.emptyList(), hosts);
    }

    public S3Client(List<Host> hosts) {
        this(Collections.emptyList(), hosts);
    }

    public S3Client(List<TransformerFactory> transformerFactories, Host... hosts) {
        this(transformerFactories, Arrays.asList(hosts));
    }

    
    public S3Client(List<TransformerFactory> transformerFactories, List<Host> hosts) {
        List<TransformerFactory> factories = new ArrayList<>();
        super.accessMonitor = new S3AccessMonitor(new S3DecisionProcedure());
        factories.add(accessMonitor);
        factories.add(new StaticInteractiveTransformer());
        factories.addAll(transformerFactories);
        init(factories, hosts);
    }
    
    
    public S3DomainManager loginAsRoot(ByteString domain){
        return new S3DomainManager(domain);
    }
    
    //INTERACTIVE
    public SecuredInteractiveTransaction startTransaction(ByteString user, ByteString domain, Object userData){
        //TODO : Romain
        if(user.equals(domain)){throw new AccessControlException("root credentials not permitted");}
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public SecuredInteractiveTransaction startTransaction(ByteString user, ByteString domain){
        return startTransaction(user, domain, null);
    }
    
    //STATIC
    public SecuredStaticTransaction createStaticTransaction(ByteString user, ByteString domain, Object userData) {
        //TODO : Romain
        //return new SecuredStaticTransaction(this, accessMonitor, user, userData);
        if(user.equals(domain)){throw new AccessControlException("root credentials not permitted");}
        throw new UnsupportedOperationException("not implemented yet");
    }

    public SecuredStaticTransaction createStaticTransaction(ByteString user, ByteString domain) {
        return createStaticTransaction(user, null);
    }
    
    //NOT TRANSACTION
    public SecuredNoTransaction noTransaction(ByteString user, ByteString domain, Object userData) {
        //TODO : Romain
        //return new SecuredNoTransaction(this, accessMonitor, user, userData);
        if(user.equals(domain)){throw new AccessControlException("root credentials not permitted");}
        throw new UnsupportedOperationException("not implemented yet");
    }

    public SecuredNoTransaction noTransaction(ByteString user, ByteString domain) {
        return noTransaction(user, domain, null);
    }
    
    
    //OVERRIDE ACGreGate & unsecure client API
    
    @Override
    public InteractiveTransaction startTransaction() {
        throw new IllegalStateException("Currently active user and domain required!");
    }

    @Override
    public SecuredInteractiveTransaction startTransaction(ByteString user, Object userData) {
        throw new IllegalStateException("Currently active domain required!");
    }

    @Override
    public SecuredInteractiveTransaction startTransaction(ByteString user) {
        throw new IllegalStateException("Currently active domain required!");
    }

    @Override
    public AntidoteStaticTransaction createStaticTransaction() {
        throw new IllegalStateException("Currently active user and domain required!");
    }

    @Override
    public SecuredStaticTransaction createStaticTransaction(ByteString user, Object userData) {
        throw new IllegalStateException("Currently active domain required!");
    }

    @Override
    public SecuredStaticTransaction createStaticTransaction(ByteString user) {
        throw new IllegalStateException("Currently active domain required!");
    }

    @Override
    public NoTransaction noTransaction() {
        throw new IllegalStateException("Currently active user required!");
    }

    @Override
    public SecuredNoTransaction noTransaction(ByteString user, Object userData) {
        throw new IllegalStateException("Currently active user and domain required!");
    }

    @Override
    public SecuredNoTransaction noTransaction(ByteString user) {
        throw new IllegalStateException("Currently active user and domain required!");
    }
    
}
