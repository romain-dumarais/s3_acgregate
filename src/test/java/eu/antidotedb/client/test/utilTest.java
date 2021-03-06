package eu.antidotedb.client.test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.AntidoteConfigManager;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.S3AccessMonitor;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.accessresources.Permissions;
import eu.antidotedb.client.accessresources.S3AccessResource;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3Operation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * test class to test the minimal policy merging and other features used in the 
 * Access Control
 * @author romain-dumarais
 */
public class utilTest {
    final S3AccessMonitor accessMonitor = new S3AccessMonitor();
    final Bucket<String> bucket1=Bucket.create("testbucket");
    final ByteString domain = ByteString.copyFromUtf8("test_domain");
    
    
    
    @Test
    public void mergetestUser(){
        Map<String,String> condition1= new HashMap<>(); condition1.put("IP", "168.168.168.168");
        Map<String,String> condition2= new HashMap<>(); condition2.put("hour","12"); condition2.put("day","friday");
        Map<String,String> condition3= new HashMap<>(); condition3.putAll(condition2); condition3.putAll(condition1);


        S3Statement statement1 = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition1);
        S3Statement statement2 = new S3Statement(false,Arrays.asList("user3","user4"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition2);
        S3Statement statement3 = new S3Statement(true,Arrays.asList("user1"),Arrays.asList(S3Operation.READOBJECTACL,S3Operation.WRITEOBJECTACL), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition3);
        S3Statement statement4 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), condition1);
        S3Statement statement5 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), condition2);
        
        S3UserPolicy policy1object = new S3UserPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"))), new ArrayList(Arrays.asList(statement1, statement2, statement3, statement4)));
        S3UserPolicy policy2object = new S3UserPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group2"))), new ArrayList(Arrays.asList(statement1, statement2, statement3, statement5)));
        S3UserPolicy policy3object = new S3UserPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"), ByteString.copyFromUtf8("group2"))), new ArrayList(Arrays.asList(statement1, statement2, statement4)));
        
        ByteString policy1 = policy1object.encode();
        ByteString policy2 = policy2object.encode();
        ByteString policy3 = policy3object.encode();
        
        S3Policy minimalpolicy0 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy1)));
        S3Policy minimalpolicy1 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy2)));
        S3Policy minimalpolicy2 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy3)));
        S3Policy minimalpolicy3 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy2, policy3)));
        
        S3UserPolicy expectedminimalpolicy1 = new S3UserPolicy(new ArrayList<>(), new ArrayList(Arrays.asList(statement1, statement2, statement3)));
        S3UserPolicy expectedminimalpolicy2 = new S3UserPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"))), new ArrayList(Arrays.asList(statement1, statement2, statement4)));
        S3UserPolicy expectedminimalpolicy3 = new S3UserPolicy(new ArrayList<>(), new ArrayList(Arrays.asList(statement1, statement2)));
        
        comparePolicy(policy1object,minimalpolicy0);
        comparePolicy(expectedminimalpolicy1,minimalpolicy1);
        comparePolicy(expectedminimalpolicy2,minimalpolicy2);
        comparePolicy(expectedminimalpolicy3,minimalpolicy3);
    }
    
        @Test
    public void mergetestBucket(){
         Map<String,String> condition1= new HashMap<>(); condition1.put("IP", "168.168.168.168");
        Map<String,String> condition2= new HashMap<>(); condition2.put("hour","12"); condition2.put("day","friday");
        Map<String,String> condition3= new HashMap<>(); condition3.putAll(condition2); condition3.putAll(condition1);


        S3Statement statement1 = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition1);
        S3Statement statement2 = new S3Statement(false,Arrays.asList("user3","user4"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition2);
        S3Statement statement3 = new S3Statement(true,Arrays.asList("user1"),Arrays.asList(S3Operation.READOBJECTACL,S3Operation.WRITEOBJECTACL), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), condition3);
        S3Statement statement4 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), condition1);
        S3Statement statement5 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), condition2);
        
        S3BucketPolicy policy1object = new S3BucketPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"))), new ArrayList(Arrays.asList(statement1, statement2, statement3, statement4)));
        S3BucketPolicy policy2object = new S3BucketPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group2"))), new ArrayList(Arrays.asList(statement1, statement2, statement3, statement5)));
        S3BucketPolicy policy3object = new S3BucketPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"), ByteString.copyFromUtf8("group2"))), new ArrayList(Arrays.asList(statement1, statement2, statement4)));
        
        ByteString policy1 = policy1object.encode();
        ByteString policy2 = policy2object.encode();
        ByteString policy3 = policy3object.encode();
        
        S3Policy minimalpolicy0 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy1)));
        S3Policy minimalpolicy1 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy2)));
        S3Policy minimalpolicy2 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy1, policy3)));
        S3Policy minimalpolicy3 = accessMonitor.policyMergerHelper(new ArrayList(Arrays.asList(policy1, policy2, policy3)));
        
        S3BucketPolicy expectedminimalpolicy1 = new S3BucketPolicy(new ArrayList<>(), new ArrayList(Arrays.asList(statement1, statement2, statement3)));
        S3BucketPolicy expectedminimalpolicy2 = new S3BucketPolicy(new ArrayList(Arrays.asList(ByteString.copyFromUtf8("user_group1"))), new ArrayList(Arrays.asList(statement1, statement2, statement4)));
        S3BucketPolicy expectedminimalpolicy3 = new S3BucketPolicy(new ArrayList<>(), new ArrayList(Arrays.asList(statement1, statement2)));
        
        comparePolicy(policy1object,minimalpolicy0);
        comparePolicy(expectedminimalpolicy1,minimalpolicy1);
        comparePolicy(expectedminimalpolicy2,minimalpolicy2);
        comparePolicy(expectedminimalpolicy3,minimalpolicy3);
    }
    
    public void comparePolicy(S3Policy expectedPolicy,S3Policy policy){
        //direct inclusion
        for(int i=0; i<expectedPolicy.getGroups().size();i++){
        Assert.assertEquals(expectedPolicy.getGroup(i),policy.getGroup(i));
        }
        for(S3Statement stat : expectedPolicy.getStatements()){
            Assert.assertTrue(policy.getStatements().contains(stat));
        }
        //reverse inclusion
        for(int i=0; i<policy.getGroups().size();i++){
        Assert.assertEquals(policy.getGroup(i),expectedPolicy.getGroup(i));
        }
        for(S3Statement stat : policy.getStatements()){
            Assert.assertTrue(expectedPolicy.getStatements().contains(stat));
        }
    }
    
    /**
     * short test for instanceof in initialization flag checking
     */
    @Test
    public void instanceofTest(){
        S3AccessResource permission1, userPolicy, policy;
        permission1=new Permissions(new ArrayList<>());
        userPolicy = new S3UserPolicy();
        S3Policy bucketPolicy = new S3BucketPolicy();
        policy = new S3Policy(bucketPolicy.encode());
        List<S3AccessResource> list = Arrays.asList(permission1, userPolicy, bucketPolicy, policy);
        for(S3AccessResource resource : list){
            if(resource instanceof Permissions){
                Assert.assertEquals(resource,permission1);
            }
            if(resource instanceof S3Policy){
                Assert.assertTrue(Arrays.asList(userPolicy, policy, bucketPolicy).contains(resource));
            }
        }
    }
    
    /**
     * test to check if a domain flag is understood as specified
     */
    @Test
    public void domainFlagTest(){
        AntidoteConfigManager antidoteConfigManager = new AntidoteConfigManager();
        S3Client antidoteClient = new S3Client(antidoteConfigManager.getConfigHosts());
        //Bucket<String> bucket1 = Bucket.create("bucketTestS3");
        S3InteractiveTransaction tx = antidoteClient.startTransaction(domain, domain);
        S3UserPolicy userPolicy = new S3UserPolicy(Arrays.asList(ByteString.copyFromUtf8("_hello")), new ArrayList<>());
        userPolicy.assignPolicy(tx, ByteString.copyFromUtf8("user"));
        tx.commitTransaction();
    }
    
}