package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CrdtMapDynamic;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.MapRef;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.transformer.CountingTransformer;
import eu.antidotedb.client.transformer.LogTransformer;
import eu.antidotedb.client.transformer.TransformerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author romain-dumarais
 */
public class S3Test {
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
     * init fot other tests
     */
    public S3Test(){
        this.debugLog = false;
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
    
    /**
     * helper for netbeans environment
     */
    @Test
    public void initTests(){
        System.out.println("#### tests ####");
        S3_Test1ACLs test1 = new S3_Test1ACLs();
        S3_Test2Policies test2 = new S3_Test2Policies();
        S3_Test3Attacks test3 = new S3_Test3Attacks();
        resetAll();
        test1.scenario_0();
        test1.scenario_1();
        test1.scenario_2();
        test1.scenario_3();
        test1.scenario_4();
        test2.scenario_5init();
        test2.scenario_5();/*
        test2.scenario_5bis();
        test2.scenario_5ter();
        test2.scenario_6();
        test2.scenario_7();
        test2.scenario_8();
        test2.scenario_9();
        test2.scenario_10();
        test3.scenario_11();*/
    }
    
    public void resetAll(){
        S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain, domain);
        S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(),new ArrayList<>());
        S3Policy userPolicy = new S3UserPolicy(new ArrayList<>(),new ArrayList<>());
        bucketPolicy.assignPolicy(tx1, bucket1.getName());
        userPolicy.assignPolicy(tx1, user1);
        userPolicy.assignPolicy(tx1, user2);

        HashMap<String, String> defaultPermissions;
        defaultPermissions = new HashMap<>();
        defaultPermissions.put("admin","default");
        defaultPermissions.put("user1","default");
        defaultPermissions.put("user2","default");
        S3ObjectACL objectACL = new S3ObjectACL(defaultPermissions);
        S3BucketACL bucketACL = new S3BucketACL(defaultPermissions);
        objectACL.assign(tx1, bucket1.getName(), object1.getRef().getKey());
        objectACL.assign(tx1, bucket1.getName(), object2.getRef().getKey());
        bucketACL.assign(tx1, bucket1.getName());
        tx1.commitTransaction();
    }
    
}
