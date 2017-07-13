package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.GMAP;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.ORSET;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.IntegerRef;
import eu.antidotedb.client.MapRef;
import eu.antidotedb.client.RegisterRef;
import eu.antidotedb.client.S3BucketPolicy;
import eu.antidotedb.client.S3DomainManager;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.S3Policy;
import eu.antidotedb.client.S3Statement;
import eu.antidotedb.client.S3UserPolicy;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Test Class to implement scenarii 5 to 10
 * @author Romain
 */
public class S3_Test2Policies extends S3Test{
    
    public S3_Test2Policies() {
        super(false);
    }
    
        /*TODO : Romain : remove
    try{
            SecuredInteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            tx1.commitTransaction();
            System.out.println("2 : user1 ACL : success");
        }catch(AccessControlException e){
            System.out.println("2 : user1 ACL : success");
        }catch(Exception e){
            System.err.println("2 : user1 ACL : fail");
            System.err.println(e);
    }
*/
    /**
     * TODO : Romain : initializes ACLs, 2 users, 1 bucket with 2 objects
     */
    
    /**
     * creates a "user2"
     * admin fails to write the bucket policy
     * user2 fails to access anything
     * domain root change bucket policies -> user2 can read object1, write object2
     * access tests
     */
    @Test
    public void scenario_5(){
        //admin can not create user2
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"), Arrays.asList("*"), ""));
            S3Policy user2Policy = new S3UserPolicy(new ArrayList<>(), statements);
            user2Policy.assignPolicy(tx1, user2);
            tx1.commitTransaction();
            System.err.println("5 : admin unauthorized to create user : fail");
        }catch(AccessControlException e){
            System.out.println("5 : admin unauthorized to create user : success");
        }catch(Exception e){
            System.err.println("5 : admin unauthorized to create user : fail");
            System.err.println(e);
        }
        //admin can not write policy
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList("*"), Arrays.asList("*"), ""));
            S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements);
            bucketPolicy.assignPolicy(tx2, bucket1.getName());
            tx2.commitTransaction();
            System.err.println("5 : admin unauthorized to write policies : fail");
        }catch(AccessControlException e){
            System.out.println("5 : admin unauthorized to write policies : success");
        }catch(Exception e){
            System.err.println("5 : admin unauthorized to write policies : fail");
            System.err.println(e);
        }
        //root creates user2, allows admin to write policies
        try{
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx3 = domainManager.startTransaction();
            domainManager.createUser(user2, tx3);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList("assignPolicy"), Arrays.asList("*"), ""));
            S3Policy adminPolicy = new S3UserPolicy(new ArrayList<>(), statements);
            adminPolicy.assignPolicy(tx3, user2);
            tx3.commitTransaction();
            System.out.println("5 : admin can write Policies : success");
        }catch(Exception e){
            System.err.println("5 : admin can write Policies : fail");
            System.err.println(e);
        }
        //admin gives user2 : r for object1, rw for object2
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            //TODO : Romain : java client operations like getValues, isEmpty, or read request
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("read"), Arrays.asList("object1TestS3"), ""));
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("read", "update"), Arrays.asList("object2TestS3"), ""));
            S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements);
            bucketPolicy.assignPolicy(tx4, bucket1.getName());
            tx4.commitTransaction();
            System.out.println("5 : admin writes bucket policy : success");
        }catch(Exception e){
            System.err.println("5 : admin writes bucket policy : fail");
            System.err.println(e);
        }
        //user2 fails to write object1
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user2, domain);
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            CrdtSet<String> object1crdt = object1Ref.toMutable();
            object1crdt.add("test 5 transaction 5 : unauthorized");//write in object1
            object1crdt.push(tx5);
            tx5.commitTransaction();
            System.err.println("5 : user2 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("5 : user2 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("5 : user2 fails to write object1 : fail");
            System.err.println(e);
        }
        //user2 writes in object2
        try{
            S3InteractiveTransaction tx6 = antidoteClient.startTransaction(user2, domain);
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            object2Ref.register("testRegister", ValueCoder.utf8String).set(tx6, "field1: update in test 5 transaction 6"); //write in object2
            tx6.commitTransaction();
            System.out.println("6 : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("6 : auser2 writes object2  : fail");
            System.err.println(e);
        }
    }
    
    /**
     * scenario5 based on the type of the object
     */
    @Test
    public void scenario_5bis(){
        //admin writes in bucket Policy : user1 : map allow set deny, user2 : allow any write op
        //admin writes in user2Policy : map allow set deny
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements1 = new ArrayList<>();
            statements1.add(new S3Statement(false, Arrays.asList("user1"), Arrays.asList("*"), ORSET, ""));
            statements1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList("*"), GMAP, ""));
            statements1.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"), Arrays.asList("*"), ""));
            List<S3Statement> statements2 = new ArrayList<>();
            statements2.add(new S3Statement(false, Arrays.asList("user2"), Arrays.asList("*"), ORSET, ""));
            statements2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"), GMAP, ""));
            S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements1);
            S3Policy user2Policy = new S3UserPolicy(new ArrayList<>(), statements2);
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            user2Policy.assignPolicy(tx1, user2);
            tx1.commitTransaction();
            System.out.println("5b : admin writes policies : success");
        }catch(Exception e){
            System.err.println("5b : admin writes policies : fail");
            System.err.println(e);
        }
        
        //user1 fails to write object1
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            CrdtSet<String> object1crdt = object1Ref.toMutable();
            object1crdt.add("test 5b transaction 2 : unauthorized");//write in object1
            object1crdt.push(tx2);
            tx2.commitTransaction();
            System.err.println("5b : user1 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("5b : user1 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("5b : user1 fails to write object1 : fail");
            System.err.println(e);
        }
        //user1 writes in object2
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            object2Ref.register("testRegister", ValueCoder.utf8String).set(tx3, "field1: update in test 5b transaction 3"); //write in object2
            tx3.commitTransaction();
            System.out.println("5b : user1 writes object2 : success");
        }catch(Exception e){
            System.err.println("5b : auser1 writes object2  : fail");
            System.err.println(e);
        }
        
        //user2 fails to write object1
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user2, domain);
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            CrdtSet<String> object1crdt = object1Ref.toMutable();
            object1crdt.add("test 5b transaction 4 : unauthorized");//write in object1
            object1crdt.push(tx4);
            tx4.commitTransaction();
            System.err.println("5b : user2 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("5b : user2 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("5b : user2 fails to write object1 : fail");
            System.err.println(e);
        }
        //user2 writes in object2
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user2, domain);
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            object2Ref.register("testRegister", ValueCoder.utf8String).set(tx5, "field1: update in test 5b transaction 5"); //write in object2
            tx5.commitTransaction();
            System.out.println("5b : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("5b : user2 writes object2  : fail");
            System.err.println(e);
        }
    }
    
    /**
     * I change admin user policy : it can write any policy
     * user1 fails to write its policy
     * amdin writes its policy : allows him to read everything
     * access tests
     */
    @Test
    public void scenario_6(){
        //admin writes in bucket Policy : user1 : map allow set deny, user2 : allow any write op
        //admin writes in user2Policy : map allow set deny
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements1 = new ArrayList<>();
            statements1.add(new S3Statement(false, Arrays.asList("user1"), Arrays.asList("*"), Arrays.asList("*"), ""));
            statements1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList("*"), Arrays.asList("*"), ""));
            statements1.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"), Arrays.asList("*"), ""));
            List<S3Statement> statements2 = new ArrayList<>();
            statements2.add(new S3Statement(false, Arrays.asList("user2"), Arrays.asList("*"), ORSET, ""));
            statements2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"), GMAP, ""));
            S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements1);
            S3Policy user2Policy = new S3UserPolicy(new ArrayList<>(), statements2);
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            user2Policy.assignPolicy(tx1, user2);
            tx1.commitTransaction();
            System.out.println("6 : admin writes policies : success");
        }catch(Exception e){
            System.err.println("6 : admin writes policies : fail");
            System.err.println(e);
        }
        
        //user1 fails to write object1
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            CrdtSet<String> object1crdt = object1Ref.toMutable();
            object1crdt.add("test 6 transaction 2 : unauthorized");//write in object1
            object1crdt.push(tx2);
            tx2.commitTransaction();
            System.err.println("6 : user1 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("6 : user1 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("6 : user1 fails to write object1 : fail");
            System.err.println(e);
        }
        //user1 writes in object2
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            object2Ref.register("testRegister", ValueCoder.utf8String).set(tx3, "field1: update in test 6 transaction 3"); //write in object2
            tx3.commitTransaction();
            System.out.println("6 : user1 writes object2 : success");
        }catch(Exception e){
            System.err.println("6 : auser1 writes object2  : fail");
            System.err.println(e);
        }
        
        //user2 fails to write object1
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user2, domain);
            SetRef<String> object1Ref = bucket1.set("object1TestS3", ValueCoder.utf8String); // orset
            CrdtSet<String> object1crdt = object1Ref.toMutable();
            object1crdt.add("test 6 transaction 4 : unauthorized");//write in object1
            object1crdt.push(tx4);
            tx4.commitTransaction();
            System.err.println("6 : user2 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("6 : user2 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("6 : user2 fails to write object1 : fail");
            System.err.println(e);
        }
        //user2 writes in object2
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user2, domain);
            MapRef<String> object2Ref = bucket1.map_g("object2TestS3"); // grow-only Map
            object2Ref.register("testRegister", ValueCoder.utf8String).set(tx5, "field1: update in test 6 transaction 5"); //write in object2
            tx5.commitTransaction();
            System.out.println("6 : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("6 : user2 writes object2  : fail");
            System.err.println(e);
        }
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
    /**
     * create complex scenario, check a little explicit deny in policy for usr1,
     * in ACL for user2 prevent them to do anything
     * access tests
     */
    @Test
    public void scenario_7(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
    /**
     * create complex scenario, check a little explicit allow in policy for usr1,
     * in ACL for user2 enables them to do anything
     * access tests
     */
    @Test
    public void scenario_8(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
    /**
     * create simple scenario. checks a lack of allow/deny results in an explicit deny
     * access tests
     */
    @Test
    public void scenario_9(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
    /**
     * create complex policies, restricting temporal access of user 1, 
     * restricting spatial access of user2
     * access tests
     */
    @Test
    public void scenario_10(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
}
