package eu.antidotedb.client.test;

import org.junit.Test;

/**
 * this class tests some specific attacks against S3 access control monitor
 * scenario 11
 * @author Romain
 */
public class S3_Test3Attacks extends S3Test{
    
    public S3_Test3Attacks() {
        super(false);
    }
    
    /**
     * admin creates a security bucket for a new bucket name. -> should fail
     * admin writes its policy to access any object
     * I create a new bucket
     * admin fails to access its objects
     * admin fails to write the policies of the bucket
     */
    @Test
    public void scenario_11(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenarion not implemented yet");
    }
}
