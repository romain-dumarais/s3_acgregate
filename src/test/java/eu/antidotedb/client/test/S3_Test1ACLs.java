package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteStaticTransaction;
import eu.antidotedb.client.CounterRef;
import eu.antidotedb.client.CrdtCounter;
import eu.antidotedb.client.IntegerRef;
import eu.antidotedb.client.InteractiveTransaction;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.S3Client.S3DomainManager;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test Class to implement scenarios 1 to 4
 * @author romain-dumarais
 */
public class S3_Test1ACLs extends S3Test {
    
    public S3_Test1ACLs() {
        super();
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
        
        
        //create objects : give them keys & values of described tests
        //try {
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain,domain);
            
            domainManager.createBucket(bucket1.getName(), tx1);
            object1.add("test 1 field 1");
            object1.add("test 1 field 2");
            object1.push(tx1);
            
            object2.register("testRegister",ValueCoder.utf8String).set("field1:testRegister");//add field 1 : register
            object2.counter("testInteger").increment(1); //add field 2 : Integer
            object2.push(tx1);
            
            tx1.commitTransaction();
            System.out.println("1 : creating ressources : success");
        /*}catch(Exception e){
            System.err.println("1 : creating ressources : fail");
            System.err.println(e);
        }*/
        
        //create User Policies
        //try{
        //    S3Client rootinterface = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(domain,domain);
            domainManager.createUser(admin, tx2);
            domainManager.createUser(user1, tx2);            
            tx2.commitTransaction();
            System.out.println("1 : creating users : success");
        /*}catch(Exception e){
            System.err.println("1 : creating users : fail");
            System.err.println(e);
        }*/
        
        //Check for metadata
        //try{
        //    S3Client domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(domain,domain);
            S3ObjectACL object1ACL, object2ACL; 
            S3BucketACL bucketACL;            S3Policy bucketPolicy, adminPolicy;
            object1ACL = new S3ObjectACL(); object2ACL = new S3ObjectACL(); bucketACL= new S3BucketACL();
            bucketPolicy = new S3BucketPolicy(); adminPolicy = new S3UserPolicy();
            
            object1ACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), admin);
            object1ACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), user1);
            object2ACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"), admin);
            object2ACL.readForUser(tx3, bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"), user1);
            bucketACL.readForUser(tx3, bucket1.getName(), admin);
            bucketACL.readForUser(tx3, bucket1.getName(), user1);
            bucketPolicy.readPolicy(tx3, bucket1.getName());
            adminPolicy.readPolicy(tx3, admin);
            
            //verify ACL
            assert(object1ACL.getRight("admin").equals("none"));
            assert(object1ACL.getRight("user1").equals("none"));
            assert(object2ACL.getRight("admin").equals("none"));
            assert(object2ACL.getRight("user1").equals("none"));
            assert(bucketACL.getRight("admin").equals("none"));
            assert(bucketACL.getRight("user1").equals("none"));
            
            //TODO : Romain : verify Policy
            
            System.out.println("1 : checking metadata : success");
        /*}catch(Exception e){
            System.err.println("1 : checking metadata : fail");
            System.err.println(e);
        }*/
        
        //start unauthorized transaction
        //try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user2, domain);
            object1.add("test 1 unauthorized field 3");
            object1.push(tx4);
            
            object2.counter("testInteger").increment(); //add field 2 : Integer
            object2.push(tx4);
            
            tx4.commitTransaction();
            System.err.println("1 : unauthorised users : fail");
        /*}catch(AccessControlException e){
            System.out.println("1 : unauthorised users : success");
        }catch(Exception e){
            System.err.println("1 : unauthorised users : fail");
            System.err.println(e);
        }*/
    }
    
    /**
     * test Policies implementation
     * method sets rights in ACLs
     */
    @Test
    public void scenario_2(){

        //write the ACLs
        try{
            
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain,domain);
            
            
            HashMap<String, String> permissions1, permissions2;
            permissions1 = new HashMap<>();
            permissions2 = new HashMap<>();
            permissions1.put("admin","writeACL");
            permissions1.put("user1", "read");
            new S3ObjectACL(permissions1).assign(tx1, bucket1.getName(), object1.getRef().getKey());
            
            
            permissions2.put("admin","writeACL");
            permissions2.put("user1","none");
            new S3ObjectACL(permissions2).assign(tx1, bucket1.getName(), object2.getRef().getKey());
            
            tx1.commitTransaction();
            System.out.println("2 : write in policies : success");
        }catch(Exception e){
            System.err.println("2 : write in policies : fail");
            System.err.println(e);
        }
        
        //test admin ACLs
        try{
            SecuredInteractiveTransaction tx2 = antidoteClient.startTransaction(admin, domain);
            object1.add("test 2 field 1 (expected)");
            object1.push(tx2); //write object1
            int testInteger = object2.counter("testInteger").getValue();
            object2.push(tx2); //read object2
            tx2.commitTransaction();
            System.out.println("2 : admin ACL : success");
        }catch(Exception e){
            System.err.println("2 : admin ACL : fail");
            System.err.println(e);
        }
        
        //test user1 ACLs
        try{

            SecuredInteractiveTransaction tx3 = antidoteClient.startTransaction(admin, domain);
            object1.add("test 2 field 1 (expected)");
            object1.push(tx3); //write object1
            int testInteger = object2.counter("testInteger").getValue();
            object2.push(tx3); //read object2
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
 
        //admin sets ACLs
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            
            HashMap<String, String> permissions1, permissions2;
            permissions1 = new HashMap<>(); permissions2 = new HashMap<>();
            
            permissions1.put("admin","writeACL");
            permissions1.put("user1", "none");
            new S3ObjectACL(permissions1).assign(tx1, bucket1.getName(), object1.getRef().getKey());
            
            
            permissions2.put("admin","read");
            permissions2.put("user1","writeACL");
            new S3ObjectACL(permissions2).assign(tx1, bucket1.getName(), object2.getRef().getKey());
            
            tx1.commitTransaction();
            System.out.println("2 : user1 ACL : success");
        }catch(Exception e){
            System.err.println("3 : admin writeACL : fail");
            System.err.println(e);
        }
        
        //admin can not write in object2
        try{
            SecuredInteractiveTransaction tx2 = antidoteClient.startTransaction(admin, domain);         
            object2.counter("testInteger").increment(1); //add field 2 : Integer
            object2.push(tx2);
            tx2.commitTransaction();
            System.err.println("3 : admin unauthorized write : fail");
        }catch(AccessControlException e){
            System.out.println("3 : admin unauthorized write : success");
        }catch(Exception e){
            System.err.println("3 : admin unauthorized write : fail");
            System.err.println(e);
        }
        
        //user1 can not read object1
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            object1.getValues();
            tx3.commitTransaction();
            System.err.println("3 : user1 : fail");
        }catch(AccessControlException e){
            System.out.println("3 : user1 ACL : success");
        }catch(Exception e){
            System.err.println("3 : user1 ACL : fail");
            System.err.println(e);
        }
        
    //user1 write object2 and object ACL of object 2
    try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user1, domain);
            
            object2.register("testRegister",ValueCoder.utf8String).set("field1: updated in test 3 transaction 4");
            object2.push(tx4);
            
            HashMap<String, String> permissions = new HashMap<>();
            permissions.put("admin","writeACL");
            permissions.put("user1", "read");
            new S3ObjectACL(permissions).assign(tx4, bucket1.getName(), object1.getRef().getKey());
            
            tx4.commitTransaction();
            System.out.println("3 : user1 used rwRW rights : success");
        }catch(Exception e){
            System.err.println("3 : user1 used rwRW rights : fail");
            System.err.println(e);
    }
    
    //admin writes object 2
    try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(admin, domain);

            object2.register("testRegister",ValueCoder.utf8String).set("field1: updated in test 3 transaction 5");
            object2.push(tx5);
            
            tx5.commitTransaction();
            System.out.println("3 : verify ACL changes : success");
        }catch(Exception e){
            System.err.println("3 : verify ACL changes : fail");
            System.err.println(e);
    }
   
    //user1 fails to write object2
    try{
            S3InteractiveTransaction tx6 = antidoteClient.startTransaction(user1, domain);
            object2.register("testRegister",ValueCoder.utf8String).set("field1: updated in test 3 transaction 6");
            object2.push(tx6);
            tx6.commitTransaction();
            System.err.println("3 : verify ACL changes : fail");
        }catch(AccessControlException e){
            System.out.println("3 : verify ACL changes : success");
        }catch(Exception e){
            System.err.println("3 : verify ACL changes : fail");
            System.err.println(e);
        }
    }
    
    /**
     * user1 fails to change the ACLs
     * access tests
     */
    @Test
    public void scenario_4(){
        
        //user fails to grant itself permissions
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(user1, domain);
            S3ObjectACL.assignForUserStatic(tx1, bucket1.getName(), object1.getRef().getKey(), user1, "write");
            tx1.commitTransaction();
            System.err.println("4 : self-granting rights : fail");
        }catch(AccessControlException e){
            System.out.println("4 : self-granting rights : success");
        }catch(Exception e){
            System.err.println("4 : self-granting rights : fail");
            System.err.println(e);
        }

        S3ObjectACL object1ACL, object2ACL;
        object1ACL = new S3ObjectACL(); object2ACL = new S3ObjectACL(); 
        //verify ACL state
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            
            object1ACL.readForUser(tx2, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), admin);
            object1ACL.readForUser(tx2, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), user1);
            object2ACL.readForUser(tx2, bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"), admin);
            object2ACL.readForUser(tx2, bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"), user1);
            
            //verify ACL
            assert(object1ACL.getRight("admin").equals("writeACL"));
            assert(object1ACL.getRight("user1").equals("none"));
            assert(object2ACL.getRight("admin").equals("writeACL"));
            assert(object2ACL.getRight("user1").equals("read"));
            tx2.commitTransaction();
            System.err.println("4 : verify ACL state : success");
        }catch(Exception e){
            System.err.println("4 : verify ACL state : fail");
            System.err.println(e);
        }
        
        //set ACL
        try{
            S3InteractiveTransaction tx2bis = antidoteClient.startTransaction(admin, domain);
            object1ACL.setRight("admin", "writeACL");
            object1ACL.setRight("user1", "write");
            object2ACL.setRight("admin", "writeACL");
            object2ACL.setRight("user1", "read");
            object1ACL.assign(tx2bis, bucket1.getName(), object1.getRef().getKey());
            object2ACL.assign(tx2bis, bucket1.getName(), object2.getRef().getKey());
            tx2bis.commitTransaction();
            System.out.println("4 : set ACL : success");
        }catch(Exception e){
            System.err.println("4 : setACL: fail");
            System.err.println(e);
        }
        
        //prevent unauthorized then authorized write
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            //write object1
            object1.add("test 4 transaction 3 : unauthorized");
            object1.push(tx3);
            //write object2
            object2.register("testRegister",ValueCoder.utf8String).set("field1: unauthorized update in test 4 transaction 3");
            object2.push(tx3);
            tx3.commitTransaction();
            System.err.println("4 : unauthorized + authorized write : fail");
        }catch(AccessControlException e){
            System.out.println("4 : unauthorized + authorized write : partial success");
        }catch(Exception e){
            System.err.println("4 : unauthorized + authorized write : fail");
            System.err.println(e);
        }
        
        //prevent authorized then unauthorized write
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user1, domain);
            //write object2
            object2.register("testRegister",ValueCoder.utf8String).set("field1: unauthorized update in test 4 transaction 4");
            object2.push(tx4);
            //write object1
            object1.add("test 4 transaction 4 : unauthorized");
            object1.push(tx4);
            tx4.commitTransaction();
            System.err.println("4 : authorized + unauthorized write : fail");
        }catch(AccessControlException e){
            System.out.println("4 : authorized + unauthorized write : partial success");
        }catch(Exception e){
            System.err.println("4 : authorized + unauthorized write : fail");
            System.err.println(e);
        }
        
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(admin, domain);
            Set<String> readresult = object1.getValues();
            tx5.commitTransaction();
            if(!readresult.contains("test 4 transaction 4 : unauthorized")){System.out.println("4 : authorized + unauthorized write : verified success");}
            else{System.err.println("4 : authorized + unauthorized write : fail");}
            if(!readresult.contains("test 4 transaction 3 : unauthorized")){System.out.println("4 : unauthorized + authorized write : verified success");}
            else{System.err.println("4 : unauthorized + authorized : fail");}
        }catch(Exception e){
            System.err.println("4 : verification : fail");
            System.err.println(e);
    }
        
    }
    
}
