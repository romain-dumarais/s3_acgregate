package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.S3KeyLink;
import eu.antidotedb.client.transformer.StaticInteractiveTransformer;
import eu.antidotedb.client.transformer.TransformerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface to use the S3 Access Control over Antidote
 * creates the transaction with a userID, domainID [optional userData]
 * loginAsRoot → return a client for root operations
 * @author romain-dumarais from a model from mweber_ukl
 */
public class S3Client extends AntidoteClient{
    private final S3AccessMonitor accessMonitor;
    
    //----------------------------------------
    //          BUILDERS
    //----------------------------------------
    
    
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
        this.accessMonitor = new S3AccessMonitor();
        List<TransformerFactory> factories = new ArrayList<>();
        factories.add(this.accessMonitor);
        factories.add(new StaticInteractiveTransformer());
        factories.addAll(transformerFactories);
        super.init(factories, hosts);
    }
    
    
    
    //----------------------------------------
    //              TRANSACTIONS
    //----------------------------------------
    
    //INTERACTIVE
    public S3InteractiveTransaction startTransaction(ByteString user, ByteString domain, Map<String, ByteString> userData){
        //if(user.equals(domain)){throw new AccessControlException("using domain name is not permitted");}
        S3InteractiveTransaction tx = new S3InteractiveTransaction(this, this.accessMonitor);
        if(userData == null) {userData=new HashMap<>();}
        this.accessMonitor.setCurrentUser(tx.connection, user);
        userData.put("domain", domain);
        this.accessMonitor.setUserData(tx.connection, userData);
        return tx;
    }
    
    public S3InteractiveTransaction startTransaction(ByteString user, ByteString domain){
        return startTransaction(user, domain, null);
    }
    
    //STATIC
    public SecuredStaticTransaction createStaticTransaction(ByteString user, ByteString domain, Map<String, ByteString> userData) {
        //TODO : Romain : S3StaticTransaction
        //return new SecuredStaticTransaction(this, accessMonitor, user, userData);
        //if(user.equals(domain)){throw new AccessControlException("using domain name is not permitted");}
        throw new UnsupportedOperationException("not implemented yet");
    }

    public SecuredStaticTransaction createStaticTransaction(ByteString user, ByteString domain) {
        return createStaticTransaction(user, domain, null);
    }
    
    //NOT TRANSACTION
    public SecuredNoTransaction noTransaction(ByteString user, ByteString domain, Map<String, ByteString> userData) {
        //TODO : Romain : S3NoTransaction
        //return new SecuredNoTransaction(this, accessMonitor, user, userData);
        //if(user.equals(domain)){throw new AccessControlException("using domain name is not permitted");}
        throw new UnsupportedOperationException("not implemented yet");
    }

    public SecuredNoTransaction noTransaction(ByteString user, ByteString domain) {
        return noTransaction(user, domain, null);
    }
    
    //----------------------------------------
    //OVERRIDE ACGreGate & unsecure client API
    //----------------------------------------
    
    @Override
    public InteractiveTransaction startTransaction() {
        throw new AccessControlException("Currently active user and domain required!");
    }

    /*
    @Override
    public SecuredInteractiveTransaction startTransaction(ByteString user, Object userData) {
        throw new AccessControlException("Currently active domain required!");
    }

    @Override
    public SecuredInteractiveTransaction startTransaction(ByteString user) {
        throw new AccessControlException("Currently active domain required!");
    }*/

    @Override
    public AntidoteStaticTransaction createStaticTransaction() {
        throw new AccessControlException("Currently active user and domain required!");
    }

    /*
    @Override
    public SecuredStaticTransaction createStaticTransaction(ByteString user, Object userData) {
        throw new AccessControlException("Currently active domain required!");
    }

    @Override
    public SecuredStaticTransaction createStaticTransaction(ByteString user) {
        throw new AccessControlException("Currently active domain required!");
    }*/

    @Override
    public NoTransaction noTransaction() {
        throw new AccessControlException("Currently active user required!");
    }

    /*
    @Override
    public SecuredNoTransaction noTransaction(ByteString user, Object userData) {
        throw new AccessControlException("Currently active user and domain required!");
    }

    @Override
    public SecuredNoTransaction noTransaction(ByteString user) {
        throw new AccessControlException("Currently active user and domain required!");
    }*/
    
    
    //----------------------------------------
    //        Get Key Mapping
    //----------------------------------------
    
    public S3DomainManager loginAsRoot(ByteString domain){
        return new S3DomainManager(domain);
    }

    public static class S3DomainManager {
        private final ByteString domain;

        public S3DomainManager(ByteString domain) {
            this.domain=domain;
        }
        
        public void createBucket(S3InteractiveTransaction tx, ByteString bucketKey){
            tx.initHelper(false, domain, bucketKey);
        }

        public void deleteBucket(S3InteractiveTransaction tx, ByteString bucketKey){
            tx.deleteHelper(false, domain, bucketKey);
        }

        public void createUser(S3InteractiveTransaction tx, ByteString userKey){
            tx.initHelper(true, domain, userKey);
        }

        public void deleteUser(S3InteractiveTransaction tx, ByteString userKey){
            tx.deleteHelper(true, domain, userKey);
        }

        public S3KeyLink getKeyLink(){
            return new S3KeyLink();
        }
    }
    

}
