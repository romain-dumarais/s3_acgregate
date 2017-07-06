package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AccessMonitor;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.S3AccessMonitor;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.SecureAntidoteClient;
import eu.antidotedb.client.decision.DecisionProcedure;
import eu.antidotedb.client.decision.S3DecisionProcedure;
import eu.antidotedb.client.transformer.CountingTransformer;
import eu.antidotedb.client.transformer.LogTransformer;
import eu.antidotedb.client.transformer.TransformerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Romain
 */
public abstract class S3Test {
    final boolean debugLog;
    final CountingTransformer messageCounter;
    final S3Client antidoteClient;
    final Bucket<String> bucket1;
    final ByteString domain = ByteString.copyFromUtf8("test_domain");
    final ByteString admin=ByteString.copyFromUtf8("admin");
    final ByteString user1=ByteString.copyFromUtf8("user1");
    final ByteString user2=ByteString.copyFromUtf8("user2");
    
    /**
     * 
     * @param bucketKey name of the Bucket we will use
     * @param debugLog have a debuglog ?
     * @param decProc decision Procedure to use
     */
    public S3Test(boolean debugLog){
        this.debugLog = debugLog;
        List<TransformerFactory> transformers = new ArrayList<>();
        transformers.add(messageCounter = new CountingTransformer());
        if (debugLog) {
            transformers.add(LogTransformer.factory);
        }
        // load host config from xml file ...
        AntidoteConfigManager antidoteConfigManager = new AntidoteConfigManager();
        antidoteClient = new S3Client(transformers, antidoteConfigManager.getConfigHosts());
        
        bucket1 = Bucket.create("bucketTestS3");
    }
    
}
