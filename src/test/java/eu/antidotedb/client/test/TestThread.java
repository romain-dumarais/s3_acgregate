package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.Bucket;
import eu.antidotedb.client.Host;
import eu.antidotedb.client.accessresources.S3BucketACL;
import eu.antidotedb.client.accessresources.S3BucketPolicy;
import eu.antidotedb.client.S3Client;
import eu.antidotedb.client.S3InteractiveTransaction;
import eu.antidotedb.client.accessresources.S3ObjectACL;
import eu.antidotedb.client.accessresources.S3Operation;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author hitman
 */
class TestThread implements Runnable {
    private final ByteString user, domain;
    private final S3Client client;
    final Bucket<String> bucket1;
    
    public TestThread(ByteString user, Host host){
        this.client=new S3Client(host);
        this.user=user;
        this.domain = ByteString.copyFromUtf8("test_domain");
        this.bucket1 = Bucket.create("bucketTestS3");
    }

    /**
     * test the effect on concurrent writes of bucket policy on user1.
     * test the effect on concurrent writes of bucket ACL on user2.
     * test the effect on concurrent writes of user policy on user3.
     * test the effect on concurrent writes of objectACL on user4.
     * every user tries to access the object1 and prints the result
     */
    @Override
    public void run() {
        try{
            S3InteractiveTransaction tx1 = client.startTransaction(user, domain);
            //user1
            S3Statement statement1 = new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), Arrays.asList("object1TestS3"), null);
            S3Statement statement2 = new S3Statement(true, Arrays.asList("user1"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), Arrays.asList("object2TestS3"), null);
            S3BucketPolicy bucketPolicy = new S3BucketPolicy(new ArrayList<>(),Arrays.asList(statement1,statement2));
            bucketPolicy.assignPolicy(tx1, bucket1.getName());
            //user2
            S3BucketACL.assignForUserStatic(tx1, bucket1.getName(),ByteString.copyFromUtf8("user2"),"write");
            //user3
            S3Statement statement3 = new S3Statement(true, Arrays.asList("user3"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), Arrays.asList("object1TestS3"), null);
            S3Statement statement4 = new S3Statement(true, Arrays.asList("user3"), Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), bucket1.getName(), Arrays.asList("object2TestS3"), null);
            S3UserPolicy user3Policy = new S3UserPolicy(new ArrayList<>(), Arrays.asList(statement3,statement4));
            user3Policy.assignPolicy(tx1, ByteString.copyFromUtf8("user3"));
            //user4
            S3ObjectACL.assignForUserStatic(tx1, bucket1.getName(), ByteString.copyFromUtf8("object1TestS3"), ByteString.copyFromUtf8("user4"),"write");
            S3ObjectACL.assignForUserStatic(tx1, bucket1.getName(), ByteString.copyFromUtf8("object2TestS3"), ByteString.copyFromUtf8("user4"),"write");
            
            tx1.commitTransaction();
            System.out.println("11: admin reset all resources : success");
        }catch(Exception e){
            System.err.println("11: admin reset all resources : fail");
        }
    }
    
}
