package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CrdtMapDynamic;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.MapRef;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.S3Client.S3DomainManager;
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
    public void allTests(){
        //printResources();
        System.out.println("#### DACCORD tests ####");
        S3_Test1ACLs test1 = new S3_Test1ACLs();
        S3_Test2Policies test2 = new S3_Test2Policies();
        S3_Test3Attacks test3 = new S3_Test3Attacks();
        try{
        //verify that it is impossible to start unsecure transactions
        test1.scenario_0(); 
        
        //creation of resources by domain root, check the metadata
        test1.scenario_1(); 
        
        //domain root assigns some rights in ACLs, and we verify they are effective
        test1.scenario_2(); 
        
        //verify ACLs prevent unauthorized access, and that admin and user can switch rights 
        test1.scenario_3();
        
        //verify that an Access Control Exception aborts the whole transaction
        test1.scenario_4();
        
        //init Policies and verify user Policies are effective
        test2.scenario_5init();
        
        //test bucket Policies based on bucket names & resources
        test2.scenario_5();
        
        //test bucket Policies based on CRDT types
        //test2.scenario_5bis();//TODO : Romain : type of resource
        
        //test bucket Policies based on crdt-specific updates operations
        //test2.scenario_5ter();//TODO : Romain : crdt specific operations
        
        //test user policies based on buckets
        test2.scenario_6();
        
        //check explicit deny --> revocation invariant
        test2.scenario_7();
        
        //check default deny --> initial invariant
        test2.scenario_8();
        
        //check domain invariant & domain flag functions
        test2.scenario_9();
        
        /*
        test2.scenario_10();//TODO : Romain : appliation layer
        test3.scenario_11();
        test3.scenario_12();
        */
        }finally{
        resetACL();
        deleteEnv();
        }
    }
    
    private void deleteEnv(){
        S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain, domain);
        S3DomainManager root = antidoteClient.loginAsRoot(domain);
        root.deleteBucket(tx1, bucket1.getName());
        Bucket<String> bucket2 = Bucket.create("bucket2_for_other_TestS3");
        root.deleteBucket(tx1, bucket2.getName());
        root.deleteUser(tx1, user1);
        root.deleteUser(tx1, admin);
        root.deleteUser(tx1, user2);
    }
    
    private void resetACL(){
        S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain, domain);
        /*S3BucketPolicy bucketPolicy = new S3BucketPolicy();
        S3UserPolicy userPolicy = new S3UserPolicy();
        bucketPolicy.assignPolicy(tx1, bucket1.getName());
        userPolicy.assignPolicy(tx1, user1);
        userPolicy.assignPolicy(tx1, user2);*/

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
    
    // FOR DEBUG PURPOSE
    
    public void printResources(){
        try{
        S3InteractiveTransaction tx0 = antidoteClient.startTransaction(domain, domain);
         S3ObjectACL object1ACL, object2ACL,object3ACL; 
            S3BucketACL bucketACL;            S3BucketPolicy bucketPolicy;
            S3UserPolicy adminPolicy, user1Policy;
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
            bucketPolicy.getStatements().stream().forEach((stat) -> {System.out.println("  "
                    +stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","
                    +stat.getResources());});
            System.out.println("admin");
            adminPolicy.getStatements().stream().forEach((stat) -> {System.out.println("  "
                    +stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","
                    +stat.getResources());});
            System.out.println("user1");
            user1Policy.getStatements().stream().forEach((stat) -> {System.out.println("  "
                    +stat.getEffect()+","+stat.getActions()+","+stat.getPrincipals()+","
                    +stat.getResources());});
        }catch(Exception e){}
    }
    
}
