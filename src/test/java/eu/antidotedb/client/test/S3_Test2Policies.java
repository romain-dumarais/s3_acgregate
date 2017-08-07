package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.GMAP;
import static eu.antidotedb.antidotepb.AntidotePB.CRDT_type.ORSET;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CrdtRegister;
import eu.antidotedb.client.CrdtSet;
import eu.antidotedb.client.RegisterRef;
import eu.antidotedb.client.S3Client.S3DomainManager;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.accessresources.S3Operation;
import static eu.antidotedb.client.accessresources.S3Operation.*;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.S3KeyLink;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test Class to implement scenarios 5 to 10
 * @author romain-dumarais
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3_Test2Policies extends S3Test{
    final CrdtSet<String> object3;
    final Bucket<String> bucket2;
    
    public S3_Test2Policies() {
        super();
        this.bucket2 = Bucket.create("bucket2_for_other_TestS3");
        this.object3 = bucket2.set("object1TestS3", ValueCoder.utf8String).toMutable();
    }
    
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
    public void scenario_5init(){
        //admin can not create user2
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT),bucket1.getName(), Arrays.asList("*"), ""));
            S3UserPolicy user2Policy = new S3UserPolicy(Arrays.asList(domain), statements);
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
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(S3Operation.ASSIGNBUCKETPOLICY),bucket1.getName(), Arrays.asList("*"), ""));
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements);
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
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(domain,domain);
            domainManager.createUser(tx3, user2);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(ASSIGNBUCKETPOLICY),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(ASSIGNUSERPOLICY),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(WRITEOBJECTACL),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(WRITEBUCKETACL),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(READBUCKETPOLICY),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(READUSERPOLICY),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(READOBJECTACL),bucket1.getName(), Arrays.asList("*"), ""));
            statements.add(new S3Statement(true, Arrays.asList("admin"), Arrays.asList(READBUCKETACL),bucket1.getName(), Arrays.asList("*"), ""));
            S3UserPolicy adminPolicy = new S3UserPolicy(new ArrayList<>(), statements);
            adminPolicy.assignPolicy(tx3, admin);
            tx3.commitTransaction();
            System.out.println("5 : admin can write Policies : success");
        }catch(Exception e){
            System.err.println("5 : admin can write Policies : fail");
            System.err.println(e);
        }
        //admin resets all ACLs
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(admin, domain);
            HashMap<String, String> permissions;
            permissions = new HashMap<>();
            permissions.put("admin","writeACL");
            permissions.put("user1", "default");
            permissions.put("user2","default");
            S3ObjectACL resetObjACL = new S3ObjectACL(permissions);
            S3BucketACL resetBuckACL = new S3BucketACL(permissions);
            resetObjACL.assign(tx3, bucket1.getName(), object1.getRef().getKey());
            resetObjACL.assign(tx3, bucket1.getName(), object2.getRef().getKey());
            resetBuckACL.assign(tx3, bucket1.getName());
            tx3.commitTransaction();
            System.out.println("5 : admin can write ACLs : success");
        }catch(Exception e){
            System.err.println("5 : admin can write ACLs : fail");
            System.err.println(e);
        }
    }
    
    @Test
    public void scenario_5(){
        //admin gives user2 : r for object1, rw for object2
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            //TODO : Romain : java client operations like getValues, isEmpty, or read request
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT),bucket1.getName(), Arrays.asList("object1TestS3"), ""));
            statements.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT),bucket1.getName(), Arrays.asList("object2TestS3"), ""));
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements);
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
            object1.add("test 5 transaction 5 : unauthorized");
            object1.push(tx5);
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
            object2.register("testRegister",ValueCoder.utf8String).set("field1: update in test 5 transaction 6");
            object2.push(tx6);
            tx6.commitTransaction();
            System.out.println("5 : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("5 : auser2 writes object2  : fail");
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
            statements1.add(new S3Statement(false, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ORSET, ""));
            statements1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), GMAP, ""));
            statements1.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT),bucket1.getName(), Arrays.asList("*"), ""));
            List<S3Statement> statements2 = new ArrayList<>();
            statements2.add(new S3Statement(false, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ORSET, ""));
            statements2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), GMAP, ""));
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements1);
            S3UserPolicy user2Policy = new S3UserPolicy(new ArrayList<>(), statements2);
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
            object1.add("test 5b transaction 2 : unauthorized");
            object1.push(tx2);
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
            object2.register("testRegister",ValueCoder.utf8String).set("field1: update in test 5b transaction 3");
            object2.counter("testInteger").increment(1);
            object2.push(tx3);
            tx3.commitTransaction();
            System.out.println("5b : user1 writes object2 : success");
        }catch(Exception e){
            System.err.println("5b : auser1 writes object2  : fail");
            System.err.println(e);
        }
        
        //user2 fails to write object1
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user2, domain);
            object1.add("test 5b transaction 4 : unauthorized");
            object1.push(tx4);
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
            object2.register("testRegister",ValueCoder.utf8String).set("field1: update in test 5b transaction 5");//write in object2
            object2.counter("testInteger").increment(1);
            object2.push(tx5);
            tx5.commitTransaction();
            System.out.println("5b : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("5b : user2 writes object2  : fail");
            System.err.println(e);
        }
    }
    
    /**
     * scenario 5 based on operation type
     * 
     */
    /*TODO : Romain : feature not added yet
    @Test
    public void scenario_5ter(){
        //admin writes in bucket Policy : user1 : add allow, increment deny. user2 allow any op
        //admin writes in user2Policy : map allow set deny
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements1 = new ArrayList<>();
            statements1.add(new S3Statement(false, Arrays.asList("user1"), Arrays.asList("add"), bucket1.getName(), Arrays.asList("*"), ""));//user1 can not update object1
            statements1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList("increment","set"),bucket1.getName(), Arrays.asList("*"), ""));//user1 can update object2
            statements1.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("*"),bucket1.getName(), Arrays.asList("*"), ""));
            List<S3Statement> statements2 = new ArrayList<>();
            statements2.add(new S3Statement(false, Arrays.asList("user2"), Arrays.asList("add"),bucket1.getName(), Arrays.asList("*"), ""));
            statements2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList("increment","set"),bucket1.getName(), Arrays.asList("*"), ""));
            S3Policy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements1);
            S3Policy user2Policy = new S3UserPolicy(new ArrayList<>(), statements2);
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            user2Policy.assignPolicy(tx1, user2);
            tx1.commitTransaction();
            System.out.println("5ter : admin writes policies : success");
        }catch(Exception e){
            System.err.println("5ter : admin writes policies : fail");
            System.err.println(e);
        }
        //user1 fails to write object1
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            object1.add("test 5ter transaction 2 : unauthorized");
            object1.push(tx2);
            tx2.commitTransaction();
            System.err.println("5ter : user1 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("5ter : user1 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("5ter : user1 fails to write object1 : fail");
            System.err.println(e);
        }
        //user1 writes in object2
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            object2.register("testRegister",ValueCoder.utf8String).set("field1: update in test 5ter transaction 3");//write in object2
            object2.counter("testInteger").increment(1);
            object2.push(tx3);
            tx3.commitTransaction();
            System.out.println("5ter : user1 writes object2 : success");
        }catch(Exception e){
            System.err.println("5ter : user1 writes object2  : fail");
            System.err.println(e);
        }
        //user2 fails to write object1
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user2, domain);
            object1.add("test 5ter transaction 4 : unauthorized");//write in object1
            object1.push(tx4);
            tx4.commitTransaction();
            System.err.println("5ter : user2 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("5ter : user2 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("5ter : user2 fails to write object1 : fail");
            System.err.println(e);
        }
        //user2 writes in object2
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user2, domain);
            object2.register("testRegister",ValueCoder.utf8String).set("field1: update in test 5ter transaction 5"); //write in object2
            object2.counter("testInteger").increment(1);
            object2.push(tx5);
            tx5.commitTransaction();
            System.out.println("5ter : user2 writes object2 : success");
        }catch(Exception e){
            System.err.println("5ter : user2 writes object2  : fail");
            System.err.println(e);
        }
        
    }
    */
    /**
     * admin writes user1 its policy : allows him to read everything in bucket1
     * access tests
     */
    @Test
    public void scenario_6(){
        //admin WritePolicy user1 : can read anything bucket
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements = new ArrayList<>();
            statements.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT), bucket1.getName(), ""));
            S3UserPolicy user1Policy = new S3UserPolicy(new ArrayList<>(), statements);
            user1Policy.assignPolicy(tx1, user1);
            tx1.commitTransaction();
            System.out.println("6 : admin write policy : success");
        }catch(Exception e){
            System.err.println("6 : admin write policy : fail");
            System.err.println(e);
        }
        //set up bucket 2
        try{
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(domain,domain);
            domainManager.createBucket(tx2, bucket2.getName());
            object3.add("field 1 test 6");
            object3.push(tx2);
            tx2.commitTransaction();
            System.out.println("6 : init bucket2 & object3 : success");
        }catch(Exception e){
            System.err.println("6 : init bucket2 & object3 : fail");
            System.err.println(e);
        }
        //user1 fails to write object1
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            object1.add("test 6 transaction 3 : unauthorized");
            object1.push(tx3);
            tx3.commitTransaction();
            System.err.println("6 : user1 fails to write object1 : fail");
        }catch(AccessControlException e){
            System.out.println("6 : user1 fails to write object1 : success");
        }catch(Exception e){
            System.err.println("6 : user1 fails to write object1 : fail");
            System.err.println(e);
        }
        //user1 reads object2
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(user1, domain);
            object2.register("testRegister",ValueCoder.utf8String).getValue();
            tx4.commitTransaction();
            System.out.println("6 : user1 reads object2 : success");
        }catch(Exception e){
            System.err.println("6 : user1 reads object2  : fail");
            System.err.println(e);
        }
        //user2 fails to read object3 in another bucket
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user1, domain);
            object3.getRef().read(tx5);
            tx5.commitTransaction();
            System.err.println("6 : user1 fails to read object3 : fail");
        }catch(AccessControlException e){
            System.out.println("6 : user1 fails to read object3 : success");
        }catch(Exception e){
            System.err.println("6 : user1 fails to read object3 : fail");
            
        }
    }
    
    /**
     * verify explicit deny :
     * -user1: statement deny in bucketACL, allow in Policy
     * -user2: statement deny in bucket Policy
     * access tests
     */
    @Test
    public void scenario_7(){
        //set situation
         try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            List<S3Statement> statements0 = new ArrayList<>();
            statements0.add(new S3Statement(false, Arrays.asList("user2"), Arrays.asList(READOBJECT), bucket1.getName(), ""));
            statements0.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(READOBJECT), bucket1.getName(), ""));
            List<S3Statement> statements1 = new ArrayList<>();
            statements1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(READOBJECT), bucket1.getName(), ""));
            List<S3Statement> statements2 = new ArrayList<>();
            statements2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(READOBJECT), bucket1.getName(), ""));
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), statements0);
            S3UserPolicy user1Policy = new S3UserPolicy(new ArrayList<>(),statements1);
            S3UserPolicy user2Policy = new S3UserPolicy(new ArrayList<>(), statements2);
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            user1Policy.assignPolicy(tx1, user1);
            user2Policy.assignPolicy(tx1, user2);
            
            HashMap<String, String> readPermissions, restrictedPermissions;
            readPermissions = new HashMap<>(); restrictedPermissions = new HashMap<>();
            readPermissions.put("admin","writeACL");
            readPermissions.put("user1", "read");
            readPermissions.put("user2","read");
            restrictedPermissions.put("admin","writeACL");
            restrictedPermissions.put("user1","none");
            restrictedPermissions.put("user2","read");
            S3ObjectACL objectACL = new S3ObjectACL(readPermissions);
            S3BucketACL bucketACL = new S3BucketACL(restrictedPermissions);
            objectACL.assign(tx1, bucket1.getName(), object1.getRef().getKey());
            objectACL.assign(tx1, bucket1.getName(), object2.getRef().getKey());
            bucketACL.assign(tx1, bucket1.getName());
            tx1.commitTransaction();
            System.out.println("7 : admin writes policies : success");
        }catch(Exception e){
            System.err.println("7 : admin writes policies : fail");
            System.err.println(e);
        }
         
        //user1 fails to read object1
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            object1.getRef().read(tx2);
            tx2.commitTransaction();
            System.err.println("7 : user1 fails to read object1 : fail");
        }catch(AccessControlException e){
            System.out.println("7 : user1 fails to read object1 : success");
        }catch(Exception e){
            System.err.println("7 : user1 fails to read object1 : fail");
            System.err.println(e);
        }
        //user2 fails to read object1
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user2, domain);
            object1.getRef().read(tx3);
            tx3.commitTransaction();
            System.err.println("7 : user2 fails to read object1 : fail");
        }catch(AccessControlException e){
            System.out.println("7 : user2 fails to read object1 : success");
        }catch(Exception e){
            System.err.println("7 : user2 fails to read object1 : fail");
            System.err.println(e);
        }
    }
    
    /**
     * reset all access Resources
     * check that without any statement, the default operation is deny
     */
    @Test
    public void scenario_8(){
        //set situation
         try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(),new ArrayList<>());
            S3UserPolicy userPolicy = new S3UserPolicy(new ArrayList<>(),new ArrayList<>());
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            userPolicy.assignPolicy(tx1, user1);
            userPolicy.assignPolicy(tx1, user2);
            
            HashMap<String, String> defaultPermissions;
            defaultPermissions = new HashMap<>();
            defaultPermissions.put("admin","writeACL");
            defaultPermissions.put("user1","default");
            defaultPermissions.put("user2","default");
            S3ObjectACL objectACL = new S3ObjectACL(defaultPermissions);
            S3BucketACL bucketACL = new S3BucketACL(defaultPermissions);
            objectACL.assign(tx1, bucket1.getName(), object1.getRef().getKey());
            objectACL.assign(tx1, bucket1.getName(), object2.getRef().getKey());
            bucketACL.assign(tx1, bucket1.getName());
            tx1.commitTransaction();
            System.out.println("8 : admin reset all resources : success");
        }catch(Exception e){
            System.err.println("8 : admin reset all resources : fail");
            System.err.println(e);
        }
         //user1 fails to read object1
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
            object1.getRef().read(tx2);
            tx2.commitTransaction();
            System.err.println("8 : user1 fails to read object1 : fail");
        }catch(AccessControlException e){
            System.out.println("8 : user1 fails to read object1 : success");
        }catch(Exception e){
            System.err.println("8 : user1 fails to read object1 : fail");
            System.err.println(e);
        }
        //user2 fails to read object1
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user2, domain);
            object1.getRef().read(tx3);
            tx3.commitTransaction();
            System.err.println("8 : user2 fails to read object1 : fail");
        }catch(AccessControlException e){
            System.out.println("8 : user2 fails to read object1 : success");
        }catch(Exception e){
            System.err.println("8 : user2 fails to read object1 : fail");
            System.err.println(e);
        }
    }
    
    /**
     * ownership tests
     */
    @Test
    public void scenario_9(){
        ByteString newdomain = ByteString.copyFromUtf8("newdomain");
        //try to transfer ownership of bucket2 to another domain : newdomain
        try{
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain,domain);
            Bucket<String> securityBucket = Bucket.create(domainManager.getKeyLink().securityBucket(bucket1.getName()).toStringUtf8());
            RegisterRef<String> domainFlagRef = securityBucket.register("domain", ValueCoder.utf8String); // grow-only Map
            CrdtRegister<String> domainFlag = domainFlagRef.toMutable();
            domainFlag.set("newdomain");
            tx1.commitTransaction();
            System.err.println("9 : transfer ownerhsip : fail");
        }catch(AccessControlException e){
            System.out.println("9 : transfer ownership : success");
        }catch(Exception e){
            System.err.println("9 : transfer ownerhsip : fail");
            System.err.println(e);
        }
        try{
            
            S3DomainManager newdomainManager = antidoteClient.loginAsRoot(newdomain);
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(newdomain,newdomain);
            newdomainManager.createUser(tx2, admin);
            newdomainManager.createUser(tx2, ByteString.copyFromUtf8("user3"));            
            tx2.commitTransaction();
            System.out.println("9 : newdomain : success");
        }catch(Exception e){
            System.err.println("9 : newdomain : fail");
            System.err.println(e);
        }
        //user3 in domain2 try to access domain
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(ByteString.copyFromUtf8("user3"), domain);
            object1.getRef().read(tx3);
            tx3.commitTransaction();
            System.err.println("9 : user3 (newdomain) fails to read in domain : fail");
        }catch(AccessControlException e){
            System.out.println("9 : user3 (newdomain) fails to read in domain : success");
        }catch(Exception e){
            System.err.println("9 : user3 (newdomain) fails to read in domain : fail");
            System.err.println(e);
        }
        //admin from newdomain tries to get object1
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(admin, newdomain);
            object1.getRef().read(tx4);
            tx4.commitTransaction();
            System.err.println("9 : admin (newdomain) reads object1 (domain) : fail");
        }catch(AccessControlException e){
            System.out.println("9 : admin (newdomain) reads object1 (domain) : success");
        }catch(Exception e){
            System.err.println("9 : admin (newdomain) reads object1 (domain) : fail");
            System.err.println(e);
        }
        List<String> obj1NewDomain = null, obj1Domain = null;
        //domain2 root try to access domain
        try{
            //S3KeyLink newdomainManager = antidoteClient.loginAsRoot(newdomain);
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(newdomain,newdomain);
            obj1NewDomain = object1.getRef().read(tx5);
            tx5.commitTransaction();
            System.err.println("9 : newdomain roots tries to access object1 : conditional success");
        }catch(Exception e){
            System.err.println("9 : newdomain roots tries to access object1 : fail");
            System.err.println(e);
        }
        try{
            S3InteractiveTransaction tx6 = antidoteClient.startTransaction(admin, domain);
            obj1Domain = object1.getRef().read(tx6);
            tx6.commitTransaction();
        }catch(Exception e){
            System.err.println("9 : verifying transaction 5 : fail");
            System.err.println(e);
        }
        if(obj1Domain!=null && obj1Domain.equals(obj1NewDomain)){
            System.out.println("newdomain roots tries to access object1 : success");
        }else{
            System.err.println("newdomain roots tries to access object1 : fail");
        }
    }
    
    /**
     * restricting temporal access of user 1, 
     * restricting spatial access of user2
     * access tests
     * restriction in policies are a String of instructions, but not arbitrary code
     * more accurate spatial tests are coming
     */
    @Test
    public void scenario_10(){
        String temporalrestriction = "java.time.LocalTime:now():080000:170000";
        String iprestriction ="java.net.InetAddress:getLocalHost():127.0.0.1";
        //resetACL
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            HashMap<String, String> permissions;
            permissions = new HashMap<>();
            permissions.put("admin","writeACL");
            permissions.put("user1", "default");
            permissions.put("user2","default");
            S3ObjectACL resetObjACL = new S3ObjectACL(permissions);
            S3BucketACL resetBuckACL = new S3BucketACL(permissions);
            resetObjACL.assign(tx1, bucket1.getName(), object1.getRef().getKey());
            resetObjACL.assign(tx1, bucket1.getName(), object2.getRef().getKey());
            resetBuckACL.assign(tx1, bucket1.getName());
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(), new ArrayList<>());
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            //restricting temporal access of user 1
            ArrayList<S3Statement> statement1 = new ArrayList<>();
            statement1.add(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), temporalrestriction));
            S3UserPolicy user1Policy = new S3UserPolicy(new ArrayList<>(), statement1);
            user1Policy.assignPolicy(tx1, user1);
            //restricting spatial access of user 2
            ArrayList<S3Statement> statement2 = new ArrayList<>();
            statement2.add(new S3Statement(true, Arrays.asList("user2"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), iprestriction));
            S3UserPolicy user2Policy = new S3UserPolicy(new ArrayList<>(), statement2);
            user2Policy.assignPolicy(tx1, user2);
            tx1.commitTransaction();
            System.out.println("10 : reset ACL, retrict policies : success");
        }catch(Exception e){
            System.err.println("10 : reset ACL, retrict policies : fail");
            System.err.println(e);
        }
        
        int localTestHour = LocalDateTime.now().getHour();
        InetAddress localTestIP = null;
        InetAddress testIP = null;
        try {
             localTestIP = InetAddress.getLocalHost();
             testIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            Logger.getLogger(S3_Test2Policies.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("10 : get Test IP address : fail");
        }
        //test temporal restriction
        if(localTestHour>=8 && localTestHour<17){
            try{
                S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
                object1.getRef().read(tx2);
                tx2.commitTransaction();
                System.out.println("10 : access from time restriction : success");
            }catch(Exception e){
                System.err.println("10 : access from time restriction : fail");
                System.err.println(e);
            }
        }else{
            try{
                S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user1, domain);
                object1.getRef().read(tx2);
                tx2.commitTransaction();
                System.out.println("10 : access outside time restriction : fail");
            }catch(AccessControlException e){
                System.out.println("10 : access outside time restriction : success");
            }catch(Exception e){
                System.err.println("10 : access outside time restriction : fail");
                System.err.println(e);
            }
        }
        //test spatial restriction
        if(localTestIP !=null && localTestIP.equals(testIP)){
            try{
                S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user2, domain);
                object1.getRef().read(tx2);
                tx2.commitTransaction();
                System.out.println("10 : access from IP restriction : success");
            }catch(Exception e){
                System.err.println("10 : access from IP restriction : fail");
                System.err.println(e);
            }
        }else{
            try{
                S3InteractiveTransaction tx2 = antidoteClient.startTransaction(user2, domain);
                object1.getRef().read(tx2);
                tx2.commitTransaction();
                System.out.println("10 : access outside IP restriction : fail");
            }catch(AccessControlException e){
                System.out.println("10 : access outside IP restriction : success");
            }catch(Exception e){
                System.err.println("10 : access outside IP restriction : fail");
                System.err.println(e);
            }
        }
    }
    
    
    
}
