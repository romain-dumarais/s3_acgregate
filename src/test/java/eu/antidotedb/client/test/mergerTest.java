package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.S3AccessMonitor;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.accessresources.S3Operation;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * test class to test the minimal policy merging
 * @author romain-dumarais
 */
public class mergerTest {
    final S3AccessMonitor accessMonitor = new S3AccessMonitor();
    final Bucket<String> bucket1=Bucket.create("testbucket");
    final ByteString domain = ByteString.copyFromUtf8("test_domain");
    
    S3Statement statement1 = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is a condition block");
    S3Statement statement2 = new S3Statement(false,Arrays.asList("user3","user4"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "");
    S3Statement statement3 = new S3Statement(true,Arrays.asList("user1"),Arrays.asList(S3Operation.READOBJECTACL,S3Operation.WRITEOBJECTACL), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is another condition block");
    S3Statement statement4 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), "another condition block");
    S3Statement statement5 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), "statement5");
    
    
    @Test
    public void mergetestUser(){
        
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
        for(int i=0; i<expectedPolicy.getStatements().size();i++){
        Assert.assertEquals(expectedPolicy.getStatement(i),policy.getStatement(i));
        }
        //reverse inclusion
        for(int i=0; i<policy.getGroups().size();i++){
        Assert.assertEquals(policy.getGroup(i),expectedPolicy.getGroup(i));
        }
        for(int i=0; i<policy.getStatements().size();i++){
        Assert.assertEquals(policy.getStatement(i),expectedPolicy.getStatement(i));
        }
    }
    
}