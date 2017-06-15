/*

 */
package eu.antidotedb.client.test;

import eu.antidotedb.client.CounterRef;
import eu.antidotedb.client.CrdtCounter;
import eu.antidotedb.client.InteractiveTransaction;
import eu.antidotedb.client.decision.BucketACL;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Romain
 */
public class TestBucketACL extends SecureAntidoteTest{
    
    public TestBucketACL() {
        super("testBucket", false, new BucketACL());
    }
    
    @Test
    public void secureTransaction(){
        CounterRef lowCounter = bucket.counter("testCounter5");
        try (InteractiveTransaction tx = antidoteClient.startTransaction()) {
            CrdtCounter counter = lowCounter.toMutable();
            counter.pull(tx);
            int oldValue = counter.getValue();
            assertEquals(0, oldValue);
            counter.increment(5);
            counter.push(tx);
            tx.commitTransaction();
            System.out.println("committed");
        } catch (Exception e){
            System.out.println("echec "+e);
        } finally {
            System.out.println("fin");
        }
    }
    
}
