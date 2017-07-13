package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CrdtMapDynamic;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.MapRef;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
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
    
    final CrdtSet<String> object1;
    final CrdtMapDynamic<String> object2;
    
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
        SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
        MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
        object1 = object1Ref.toMutable();
        object2 = object2Ref.toMutable();
    }
    
}
