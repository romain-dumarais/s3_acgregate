package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteStaticTransaction;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CounterRef;
import eu.antidotedb.client.CrdtCounter;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.IntegerRef;
import eu.antidotedb.client.InteractiveTransaction;
import eu.antidotedb.client.MapRef;
import eu.antidotedb.client.Policy;
import eu.antidotedb.client.RegisterRef;
import eu.antidotedb.client.S3DomainManager;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.ObjectInBucket;
import eu.antidotedb.client.decision.S3ACL;
import eu.antidotedb.client.decision.S3BucketACL;
import eu.antidotedb.client.decision.S3BucketPolicy;
import eu.antidotedb.client.decision.S3ObjectACL;
import eu.antidotedb.client.decision.S3Policy;
import eu.antidotedb.client.decision.S3UserPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test Class to implement scenarii 1 to 4
 * @author Romain
 */
public class S3_TestACLs extends S3Test {
    
    public S3_TestACLs() {
        super(false);
    }
    
    
    /**
     * These tests verify that it is impossible to start unsecure transactions
     */
    @Test
    public void scenario_0(){
        try{
            CounterRef lowCounter = bucket1.counter("testCounter");
            IntegerRef lowInt = bucket1.integer("testInteger");
            SetRef<String> orSetRef = bucket1.set("testorSetRef", ValueCoder.utf8String);
            AntidoteStaticTransaction tx = antidoteClient.createStaticTransaction();
            lowInt.increment(tx, 3);
            lowCounter.increment(tx, 4);
            orSetRef.add(tx, "Hi");
            orSetRef.add(tx, "Bye");
            orSetRef.add(tx, "yo");
            tx.commitTransaction();
            System.err.println("0 : unsecure staticTransaction test : fail");
        }catch(AccessControlException e){
            System.out.println("0 : unsecure staticTransaction test : success");
        }
        CounterRef lowCounter = bucket1.counter("testCounter5");
        CrdtCounter counter = lowCounter.toMutable();
        try{
            InteractiveTransaction tx = antidoteClient.startTransaction();
            counter.pull(tx);
            int oldValue = counter.getValue();
            assertEquals(0, oldValue);
            counter.increment(5);
            counter.push(tx);
            tx.commitTransaction();
            System.err.println("0 : unsecure interactiveTransaction test : fail");
        }catch(AccessControlException e){
            System.out.println("0 : unsecure interactiveTransaction test : success");
        }
        try{
            antidoteClient.pull(antidoteClient.noTransaction(), Arrays.asList(counter));
            System.err.println("0 : unsecure noTransaction test : fail");
        }catch(AccessControlException e){
            System.out.println("0 : unsecure noTransaction test : success");
        }
    }
    /**
     * check no unsecure transaction
     * creation of 1 bucket with 2 objects
     * creation of 2 user : "admin" & "user"
     * creation of empty policies
     * check for the metadata
     * check that an unregistered user can start any transaction
     */
    @Test
    public void scenario_1(){
        SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
        MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
        CrdtSet<String> object1 = object1Ref.toMutable();
        
        //create objects : give them keys & values of described tests
        try {
            S3DomainManager rootinterface = antidoteClient.loginAsRoot(domain);
            SecuredInteractiveTransaction tx1 = rootinterface.startTransaction();
            
            rootinterface.createBucket(bucket1.getName(), tx1);
            object1.add("test 1 field 1");
            object1.add("test 1 field 2");
            object1.push(tx1);
            
            RegisterRef<String> testRegister = object2Ref.register("testRegister", ValueCoder.utf8String); //add field 1 : testRegister
            testRegister.set(tx1, "field1:testRegister");
            IntegerRef testInteger = object2Ref.integer("testInteger"); //add field 2 : testInteger
            testInteger.set(tx1, 0);
            testInteger.increment(tx1, 1);
            object2Ref.counter("testCounter").increment(tx1, 5); // add field3 : testCounter
            //object2.push(tx);
            
            tx1.commitTransaction();
            System.out.println("1 : creating ressources : success");
        }catch(Exception e){
            System.err.println("1 : creating ressources : fail");
            System.err.println(e);
        }
        
        //create User Policies
        Bucket<String> userBucket = Bucket.create(antidoteClient.loginAsRoot(domain).getuserBucket().toStringUtf8());
        Set<String> emptypermissions =new HashSet<String>();
        try{
            S3DomainManager rootinterface = antidoteClient.loginAsRoot(domain);
            SecuredInteractiveTransaction tx2 = rootinterface.startTransaction();
            rootinterface.createUser(ByteString.copyFromUtf8("admin"), tx2);
            rootinterface.createUser(ByteString.copyFromUtf8("user1"), tx2);            
            tx2.commitTransaction();
            System.out.println("1 : creating users : success");
        }catch(Exception e){
            System.err.println("1 : creating users : fail");
            System.err.println(e);
        }
        
        //Check for metadata
        try{
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            SecuredInteractiveTransaction tx3 = domainManager.startTransaction();
            S3ACL object1ACLadmin, object1ACLuser1, object2ACLadmin, object2ACLuser1, bucketACLadmin, bucketACLuser1;
            S3Policy bucketPolicy, adminPolicy;
                        
            object1ACLadmin = S3ObjectACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), admin);
            object1ACLuser1 = S3ObjectACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), user1);
            object2ACLadmin = S3ObjectACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), admin);
            object2ACLuser1 = S3ObjectACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), user1);
            bucketACLadmin = S3BucketACL.readForUser(tx3, bucket1.getName(), admin);
            bucketACLuser1 = S3BucketACL.readForUser(tx3, bucket1.getName(), user1);
            bucketPolicy = S3BucketPolicy.readPolicy(tx3, domain, bucket1.getName());
            adminPolicy = S3UserPolicy.readPolicy(tx3, domain, admin);
            //TODO : Romain : verify contents
            assert(object1ACLadmin.toString().equals("none"));
            assert(object2ACLadmin.toString().equals("none"));
            assert(object1ACLuser1.toString().equals("none"));
            assert(object2ACLuser1.toString().equals("none"));
            assert(bucketACLadmin.toString().equals("none"));
            assert(bucketACLuser1.toString().equals("none"));
            System.out.println("1 : checking metadata : success");
        }catch(Exception e){
            System.err.println("1 : checking metadata : fail");
            System.err.println(e);
        }
        
        //start unauthorized transaction
        try{
            SecuredInteractiveTransaction tx4 = antidoteClient.startTransaction(ByteString.copyFromUtf8("user2"));
            object1.add("test 1 unauthorized field 3");
            object1.push(tx4);
            RegisterRef<String> testRegister = object2Ref.register("testRegister", ValueCoder.utf8String);
            testRegister.set(tx4, "field4:unauthorized Register");
            tx4.commitTransaction();
            System.err.println("1 : unauthorised users : fail");
        }catch(AccessControlException e){
            System.out.println("1 : unauthorised users : success");
        }catch(Exception e){
            System.err.println("1 : unauthorised users : fail");
            System.err.println(e);
        }
    }
    
    /**
     * test Policies implementation
     * method sets rights in ACLs
     */
    @Test
    public void scenario_2(){

        ByteString admin=ByteString.copyFromUtf8("admin");
        ByteString user1=ByteString.copyFromUtf8("user1");

        //write the ACLs
        try{
            ObjectInBucket object1 = new ObjectInBucket(bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"));
            ObjectInBucket object2 = new ObjectInBucket(bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"));
            
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            SecuredInteractiveTransaction tx1 = domainManager.startTransaction();
            
            
            HashMap<String, String> permissions1, permissions2;
            permissions1 = new HashMap<>();
            permissions2 = new HashMap<>();
            permissions1.put("admin","writeACL");
            permissions1.put("user1", "read");
            new S3ObjectACL(permissions1).assign(tx1, bucket1.getName(), object1.getKey());
            
            
            permissions2.put("admin","writeACL");
            permissions2.put("user1","none");
            new S3ObjectACL(permissions2).assign(tx1, bucket1.getName(), object2.getKey());
            
            tx1.commitTransaction();
            System.out.println("2 : write in policies : success");
        }catch(Exception e){
            System.err.println("2 : write in policies : fail");
            System.err.println(e);
        }
        
        //test admin ACLs
        try{
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            CrdtSet<String> object1 = object1Ref.toMutable();
            SecuredInteractiveTransaction tx2 = antidoteClient.startTransaction(admin);
            object1.add("test 2 field 1 (expected)");
            object1.push(tx2); //write object1
            IntegerRef testInteger = object2Ref.integer("testInteger");
            testInteger.read(tx2); //read objject2
            tx2.commitTransaction();
            System.out.println("2 : admin ACL : success");
        }catch(Exception e){
            System.err.println("2 : admin ACL : fail");
            System.err.println(e);
        }
        
        //test user1 ACLs
        try{
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            CrdtSet<String> object1 = object1Ref.toMutable();
            SecuredInteractiveTransaction tx3 = antidoteClient.startTransaction(admin);
            object1.add("test 2 field 2 (unexpected)"); //write object1
            object1.push(tx3); 
            IntegerRef testInteger = object2Ref.integer("testInteger");
            testInteger.read(tx3); //read objject2
            tx3.commitTransaction();
            System.err.println("2 : user1 ACL : fail");
        }catch(AccessControlException e){
            System.out.println("2 : user1 ACL : success");
        }catch(Exception e){
            System.err.println("2 : user1 ACL : fail");
            System.err.println(e);
        }
    }
    
    /**
     * test Policies updates
     * admin changes ACLs
     * access tests
     * user1 changes ACL of object2
     * access tests
     */
    @Test
    public void scenario_3(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenarion not implemented yet");
    }
    
    /**
     * user1 fails to change the ACLs
     * access tests
     */
    @Test
    public void scenario_4(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenarion not implemented yet");
    }
    
}
