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
import eu.antidotedb.client.accessresources.S3Statement;
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
        System.out.println("#### DACCORD tests ####");
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
        test2.scenario_5();
        //test2.scenario_5bis();//TODO : Romain : type of resource
        //test2.scenario_5ter();//TODO : Romain : crdt specific operations
        test2.scenario_6();
        test2.scenario_7();
        test2.scenario_8();/*
        test2.scenario_9();//TODO : Romain : domain flags
        test2.scenario_10();//TODO : Romain : appliation layer
        test3.scenario_11();
        test3.scenario_12();
        */
    }
    
    void resetAll(){
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
    
    public void printResources(){
        try{
        S3InteractiveTransaction tx0 = antidoteClient.startTransaction(domain, domain);
         S3ObjectACL object1ACL, object2ACL,object3ACL; 
            S3BucketACL bucketACL;            S3Policy bucketPolicy, adminPolicy, user1Policy;
            object1ACL = new S3ObjectACL(); object2ACL = new S3ObjectACL(); object3ACL = new S3ObjectACL();
            bucketACL= new S3BucketACL();
            bucketPolicy = new S3BucketPolicy(); adminPolicy = new S3UserPolicy();
            user1Policy = new S3UserPolicy();
            
            object1ACL.readForUser(tx0, bucket1.getName(), object1.getRef().getKey(), admin);
            object1ACL.readForUser(tx0, bucket1.getName(), object1.getRef().getKey(), user1);
            object1ACL.readForUser(tx0, bucket1.getName(), object1.getRef().getKey(), user2);
            object2ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), admin);
            object2ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), user1);
            object2ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), user2);
            object3ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), admin);
            object3ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), user1);
            object3ACL.readForUser(tx0, bucket1.getName(), object2.getRef().getKey(), user2);
            bucketACL.readForUser(tx0, bucket1.getName(), admin);
            bucketACL.readForUser(tx0, bucket1.getName(), user1);
            bucketACL.readForUser(tx0, bucket1.getName(), user2);
            bucketPolicy.readPolicy(tx0, bucket1.getName());
            adminPolicy.readPolicy(tx0, admin);
            user1Policy.readPolicy(tx0, user1);
            tx0.commitTransaction();
            System.out.println("user1 / object 1 : "+object1ACL.getRight("user1"));
            System.out.println("user2 / object 1 : "+object1ACL.getRight("user2"));
            System.out.println("admin / object 1 : "+object1ACL.getRight("admin"));
            System.out.println("user1 / object 2 : "+object2ACL.getRight("user1"));
            System.out.println("user2 / object 2 : "+object2ACL.getRight("user2"));
            System.out.println("admin / object 2 : "+object2ACL.getRight("admin"));
            System.out.println("user1 / object 3 : "+object3ACL.getRight("user1"));
            System.out.println("user2 / object 3 : "+object3ACL.getRight("user2"));
            System.out.println("admin / object 3 : "+object3ACL.getRight("admin"));
            System.out.println("user1 / bucket ACL : "+bucketACL.getRight("user1"));
            System.out.println("user2 / bucket ACL : "+bucketACL.getRight("user2"));
            System.out.println("admin / bucket ACL : "+bucketACL.getRight("admin"));
            System.out.println("bucket");
            for(S3Statement stat:bucketPolicy.getStatements()){
                System.out.println("  "+stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","+stat.getResources());
            }
            System.out.println("admin");
            for(S3Statement stat:adminPolicy.getStatements()){
                System.out.println("  "+stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","+stat.getResources());
            }
            System.out.println("user1");
            for(S3Statement stat:user1Policy.getStatements()){
                System.out.println("  "+stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","+stat.getResources());
            }
        }catch(Exception e){}
    }
    
}
