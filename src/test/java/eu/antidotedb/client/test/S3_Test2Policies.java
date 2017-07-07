package eu.antidotedb.client.test;

import eu.antidotedb.client.decision.S3DecisionProcedure;
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
    
    /***
     * initializes ACLs, 2 users, 1 bucket with 2 objects
     */
    @Test
    public void init(){
        //TODO : Romain : same code than scenario 1
        throw new UnsupportedOperationException("test scenarion not implemented yet");
    }
    
    /**
     * creates a "user2"
     * admin fails to write the bucket policy
     * user2 fails to access anything
     * I change bucket policies -> user2 can read object1, write object2
     * access tests
     */
    @Test
    public void scenario_5(){
        //TODO : Romain
        throw new UnsupportedOperationException("test scenario not implemented yet");
    }
    
    /**
     * I change admin user policy : it can write any policy
     * user1 fails to write its policy
     * amdin writes its policy : allows him to read everything
     * access tests
     */
    @Test
    public void scenario_6(){
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
