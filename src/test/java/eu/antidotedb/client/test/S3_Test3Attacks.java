package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.CrdtMVRegister;
import eu.antidotedb.client.Host;
import eu.antidotedb.client.MVRegisterRef;
import eu.antidotedb.client.S3Client.S3DomainManager;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Operation;
import eu.antidotedb.client.accessresources.S3Policy;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * this class tests some specific attacks against S3 access control monitor
 * scenario 11
 * @author romain-dumarais
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3_Test3Attacks extends S3Test{
    
    public S3_Test3Attacks() {
        super();
    }
    
    /**
     * admin fails to write hadcoded object1 acl in hard coded security bucket
     * admin fails to create a user3 Policy in userbucket
     * access tests
     */
    @Test
    public void scenario_11(){
        //set situation
        try{
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(admin, domain);
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(),new ArrayList<>());
            S3UserPolicy user1Policy = new S3UserPolicy(new ArrayList<>(),new ArrayList<>());
            S3Statement user1Statement = new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), "");
            S3UserPolicy user2Policy = new S3UserPolicy(new ArrayList<>(), Arrays.asList(user1Statement));
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            user1Policy.assignPolicy(tx1, user1);
            user2Policy.assignPolicy(tx1, user2);
            
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
            System.out.println("11: admin reset all resources : success");
        }catch(Exception e){
            System.err.println("11: admin reset all resources : fail");
        }
        S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
        ByteString mappingSecurityBucket = domainManager.getKeyLink().securityBucket(bucket1.getName());
        ByteString mappingObjectACL = domainManager.getKeyLink().objectACL(object1.getRef().getKey(),user1);
        //admin fails to write hadcoded object1 acl in hard coded security bucket
        try{
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(admin, domain);
            Bucket<String> securityBucket = Bucket.create(mappingSecurityBucket.toStringUtf8());
            MVRegisterRef<String> aclObj1Ref = securityBucket.multiValueRegister(mappingObjectACL.toStringUtf8());
            CrdtMVRegister<String> aclobject1 = aclObj1Ref.toMutable();
            aclobject1.set("write");
            aclobject1.push(tx2);
            tx2.commitTransaction();
            System.err.println("11: admin fails to bypass ACL write check : fail");
        }catch(AccessControlException e){
            System.out.println("11: admin fails to bypass ACL write check : conditional success");
        }catch(Exception e){
            System.err.println("11: admin fails to bypass ACL write check : fail");
            System.err.println(e);
        }
        //test previous update
        try{
            S3InteractiveTransaction tx3 = antidoteClient.startTransaction(user1, domain);
            object1.add("test 11 transaction 3 : unauthorized field");
            object1.push(tx3);
            tx3.commitTransaction();
            System.err.println("11: admin fails to bypass ACL write check : fail");
        }catch(AccessControlException e){
            System.out.println("11: admin fails to bypass ACL write check : success");
        }catch(Exception e){
            System.err.println("11: access from time restriction : fail");
            System.err.println(e);
        }
        ByteString mappingUserBucket = domainManager.getKeyLink().userBucket(domain);
        ByteString mappingUserPolicy = domainManager.getKeyLink().userPolicy(user1);
        //admin fails to create a user3 Policy in userbucket
        try{
            S3InteractiveTransaction tx4 = antidoteClient.startTransaction(admin, domain);
            Bucket<String> userBucket = Bucket.create(mappingUserBucket.toStringUtf8());
            MVRegisterRef<String> user1PolicyRef = userBucket.multiValueRegister(mappingUserPolicy.toStringUtf8());
            CrdtMVRegister<String> aclobject1 = user1PolicyRef.toMutable();
            //S3Statement fullrights = new S3Statement(true, Arrays.asList("user1"), Arrays.asList("*"), Arrays.asList("object1TestS3"), "");
            String fullrights = "\"Statement\": [{\n    \"Sid\": \"1\",\n    \"Effect\": \"Allow\",\n    \"Principal\":[\""+user1.toStringUtf8()+"\"],\n    \"Action\":\"*\",\n    \"Resource\":{\"object\": ["+object1.getRef().getKey().toStringUtf8()+"]}\n  }]";
            aclobject1.set(fullrights);
            aclobject1.push(tx4);
            tx4.commitTransaction();
            System.err.println("11: admin fails to bypass Policy write check : fail");
        }catch(AccessControlException e){
            System.out.println("11: admin fails to bypass Policy write check : conditional success");
        }catch(Exception e){
            System.err.println("11: admin fails to bypass Policy write check : fail");
            System.err.println(e);
        }
        //test previous update
        try{
            S3InteractiveTransaction tx5 = antidoteClient.startTransaction(user1, domain);
            object1.add("test 11 transaction 5 : unauthorized field");
            object1.push(tx5);
            tx5.commitTransaction();
            System.err.println("11: admin fails to bypass Policy write check : fail");
        }catch(AccessControlException e){
            System.out.println("11: admin fails to bypass Policy write check : success");
        }catch(Exception e){
            System.err.println("11: access from time restriction : fail");
            System.err.println(e);
        }
    }
    
    /**
     * creates several threads with a client each, trying to access and modify ACLs & Policies concurrently for 1 Antidote ncde
     */
    @Test
    public void scenario_12(){
        try{
            S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx1 = antidoteClient.startTransaction(domain,domain);
            for(int i=0; i<5;i++){
                ByteString user = ByteString.copyFromUtf8("user"+i);
                domainManager.createUser(tx1, user);
                ArrayList<S3Statement> statements = new ArrayList<>();
                statements.add(new S3Statement(true, Arrays.asList(user.toStringUtf8()), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), ""));
                S3UserPolicy userPolicy = new S3UserPolicy(new ArrayList<>(), statements);
                userPolicy.assignPolicy(tx1, user);
            }
            tx1.commitTransaction();
        }catch(Exception e){
            System.err.println("12: set situation : fail");
        }
        try{
            Host commonHost = new Host("localhost", 8087);
            for(int i=0; i<5;i++){
                ByteString user = ByteString.copyFromUtf8("user"+i);
                TestThread testThread = new TestThread(user, commonHost);
                testThread.run();
            }
        }catch(Exception e){
            System.err.println("12: several users write : fail");
        }
        S3BucketPolicy buckPol = new S3BucketPolicy(), buckPolVerif = new S3BucketPolicy();
        S3BucketACL buckACL = new S3BucketACL();
        S3UserPolicy userPol = new S3UserPolicy(), userPolVerif = new S3UserPolicy();
        S3ObjectACL objACL1 = new S3ObjectACL();
        S3ObjectACL objACL2 = new S3ObjectACL();
        try{
            //S3DomainManager domainManager = antidoteClient.loginAsRoot(domain);
            S3InteractiveTransaction tx2 = antidoteClient.startTransaction(domain,domain);
            buckPol.readPolicy(tx2, bucket1.getName());
            buckACL.readForUser(tx2, bucket1.getName(), user2);
            userPol.readPolicy(tx2, ByteString.copyFromUtf8("user3"));
            objACL1.readForUser(tx2, bucket1.getName(), object1.getRef().getKey(), ByteString.copyFromUtf8("user4"));
            objACL2.readForUser(tx2, bucket1.getName(), object2.getRef().getKey(), ByteString.copyFromUtf8("user4"));
            tx2.commitTransaction();
        }catch(Exception e){
            System.err.println("12: read resources : fail");
        }
        buckPolVerif.addStatement(new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT),bucket1.getName(), Arrays.asList("object2TestS3"), ""));
        userPolVerif.addStatement(new S3Statement(true, Arrays.asList("user3"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(),Arrays.asList("object2TestS3"), ""));
        try{
            assertEquals(buckPol,buckPolVerif);
            assertEquals(buckACL.getRight("user2"), "none");
            assertEquals(userPol, userPolVerif);
            assertEquals(objACL1.getRight("user4"), "none");
            assertEquals(objACL1.getRight("user4"), "write");
            System.out.println("12: test success");
        }catch(Exception e){
            System.err.println("12: test failed");
        }
        
    }
}
