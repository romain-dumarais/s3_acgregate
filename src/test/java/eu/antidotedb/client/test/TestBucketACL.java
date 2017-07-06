/*

 */
package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.AntidoteStaticTransaction;
import eu.antidotedb.client.CounterRef;
import eu.antidotedb.client.CrdtCounter;
import eu.antidotedb.client.IntegerRef;
import eu.antidotedb.client.InteractiveTransaction;
import eu.antidotedb.client.SecuredInteractiveTransaction;
import eu.antidotedb.client.SetRef;
import eu.antidotedb.client.ValueCoder;
import eu.antidotedb.client.decision.AccessControlException;
import eu.antidotedb.client.decision.BucketACL;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Romain
 */
public class TestBucketACL extends S3Test{
    
    public TestBucketACL() {
        super(false);
    }
    
    @Test
    public void secureInteractiveTransaction1(){
        CounterRef lowCounter = super.bucket1.counter("testCounter");
        try (InteractiveTransaction tx = antidoteClient.startTransaction()) {
            CrdtCounter counter = lowCounter.toMutable();
            counter.pull(tx);
            int oldValue = counter.getValue();
            assertEquals(0, oldValue);
            counter.increment(5);
            counter.push(tx);
            tx.commitTransaction();
            System.err.println("test failed");
        } catch (Exception e){
            System.out.println("echec "+e);
            System.out.println("test1 success");
        } 
    }
    
    @Test(timeout = 10000)
    public void secureInteractiveTransaction2() throws Exception{
        CounterRef lowCounter = bucket1.counter("testCounter");
        try (SecuredInteractiveTransaction tx = antidoteClient.startTransaction(ByteString.copyFromUtf8("user"))) {
            CrdtCounter counter = lowCounter.toMutable();
            //counter.pull(tx);
            int oldValue = counter.getValue();
            assertEquals(0, oldValue);
            counter.increment(5);
            //counter.push(tx);
            tx.commitTransaction();
            System.err.println("test2 failed : 'user' should not be able to commit");
        } catch (AccessControlException ace){
            System.out.println("test2 success "+ace);
        }catch(Exception e){
            throw e;
        } 
    }
    
        @Test(timeout = 10000)
    public void seqStaticTransaction() {
        try{
            CounterRef lowCounter = bucket1.counter("testCounter");
        IntegerRef lowInt = bucket1.integer("testInteger");
        SetRef<String> orSetRef = bucket1.set("testorSetRef", ValueCoder.utf8String);
        AntidoteStaticTransaction tx = antidoteClient.createStaticTransaction(ByteString.copyFromUtf8("user1"));
        lowInt.increment(tx, 3);
        lowCounter.increment(tx, 4);
        orSetRef.add(tx, "Hi");
        orSetRef.add(tx, "Bye");
        orSetRef.add(tx, "yo");
        tx.commitTransaction();
        System.err.println("test3 echec");
        } catch (Exception e){
            System.out.println("test3 success "+e);
        } 
    }
    
}
