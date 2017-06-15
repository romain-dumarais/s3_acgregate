package eu.antidotedb.client.test;

import eu.antidotedb.client.AccessMonitor;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.SecureAntidoteClient;
import eu.antidotedb.client.decision.DecisionProcedure;
import eu.antidotedb.client.transformer.CountingTransformer;
import eu.antidotedb.client.transformer.LogTransformer;
import eu.antidotedb.client.transformer.TransformerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Romain
 */
public class SecureAntidoteTest {
    final boolean debugLog;
    final CountingTransformer messageCounter;
    final AccessMonitor accessMonitor;
    final SecureAntidoteClient antidoteClient;
    final DecisionProcedure decProc;
    final Bucket<String> bucket;
    final String bucketKey;
    
    /**
     * 
     * @param bucketKey name of the Bucket we will use
     * @param debugLog have a debuglog ?
     * @param decProc decision Procedure to use
     */
    public SecureAntidoteTest(String bucketKey, boolean debugLog, DecisionProcedure decProc){
        this.decProc=decProc;
        this.debugLog = debugLog;
        List<TransformerFactory> transformers = new ArrayList<>();
        transformers.add(messageCounter = new CountingTransformer());
        transformers.add(accessMonitor = new AccessMonitor(this.decProc));
        if (debugLog) {
            transformers.add(LogTransformer.factory);
        }

        // load host config from xml file ...
        AntidoteConfigManager antidoteConfigManager = new AntidoteConfigManager();


        antidoteClient = new SecureAntidoteClient(decProc, transformers, antidoteConfigManager.getConfigHosts());

        this.bucketKey = bucketKey;
        bucket = Bucket.create(bucketKey);
    }
   
}
