package eu.antidotedb.client.test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Operation;
import eu.antidotedb.client.accessresources.S3Statement;
import eu.antidotedb.client.accessresources.S3UserPolicy;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * class to test JSON parsing of Policies
 * @author romain-dumarais
 */
public class JsonTest {
    private final S3Statement statement = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is a condition block");
    
    @Test
    public void decodeStatement(){
        String jsonString = statement.encode().toString();
        
        JsonObject value = Json.parse(jsonString).asObject();
        S3Statement resultstatement = S3Statement.decodeStatic(value);
        //print results
        /*
        System.out.println("effect :"+resultstatement.getEffect());
        System.out.println("principals : "+resultstatement.getPrincipals().toString());
        System.out.println("actions : "+resultstatement.getActions().toString());
        System.out.println("resources list : "+resultstatement.getResources().toString());
        System.out.println("resource Bucket ID : "+resultstatement.getResourceBucket().toStringUtf8());
        System.out.println("condition block : "+resultstatement.getConditionBlock());*/
    }
    
    @Test
    public void encodeStatement(){
        //System.out.println(statement.encode().toString());
        JsonValue value = Json.parse(statement.encode().toString()).asObject();
        //System.out.println(value.isObject());
    }
    
    @Test
    public void round1(){
        String stringStatement0 = "{\"Effect\":true,\"Principals\":[\"user1\"],\"Actions\":[\"READOBJECT\"],\"Resources\":{\"bucket\":\"testBucketS3\",\"objects\":[\"object1TestS3\",\"object2TestS3\"]}}";
        //System.out.println("stringstatement0 : "+stringStatement0);
        JsonObject objectStatement = Json.parse(stringStatement0).asObject();
        S3Statement statement1 =S3Statement.decodeStatic(objectStatement);
        JsonObject jsonstatement2 = statement1.encode();
        String stringstatement3 = jsonstatement2.toString();
        //System.out.println("stringstatement3 : "+stringstatement3);
        assertEquals(stringStatement0,stringstatement3);
    }
    
    @Test
    public void round2(){
        JsonObject jsonstatement1 = statement.encode();
        S3Statement statement2 = S3Statement.decodeStatic(jsonstatement1);
        //System.out.println("statement2 : "+statement2.getEffect()+" : "+statement2.getPrincipals().toString()+" : "+statement2.getActions().toString()+" : "+statement2.getResources().toString()+" : "+statement2.getResourceBucket().toStringUtf8()+" : "+statement2.getConditionBlock());
        //System.out.println("statement0 : "+statement.getEffect()+" : "+statement.getPrincipals().toString()+" : "+statement.getActions().toString()+" : "+statement.getResources().toString()+" : "+statement.getResourceBucket().toStringUtf8()+" : "+statement.getConditionBlock());
        assertEquals(statement,statement2);
        
        assertEquals(statement.getEffect(),statement2.getEffect());
        assertEquals(statement.getPrincipals(),statement2.getPrincipals());
        assertEquals(statement.getActions(),statement2.getActions());
        assertEquals(statement.getResources(),statement2.getResources());
        assertEquals(statement.getResourceBucket(),statement2.getResourceBucket());
        assertEquals(statement.getConditionBlock(),statement2.getConditionBlock());
    }
    
    @Test
    public void policyRound(){
        S3Statement statement1 = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is a condition block");
        S3Statement statement2 = new S3Statement(false,Arrays.asList("user3","user4"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "");
        S3Statement statement3 = new S3Statement(true,Arrays.asList("user1"),Arrays.asList(S3Operation.READOBJECTACL,S3Operation.WRITEOBJECTACL), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is another condition block");
        S3Statement statement4 = new S3Statement(true,Arrays.asList("user2"),Arrays.asList(S3Operation.READOBJECT,S3Operation.WRITEOBJECT), ByteString.copyFromUtf8("testBucket2"), "another condition block");
        S3UserPolicy policy1 = new S3UserPolicy(Arrays.asList(ByteString.copyFromUtf8("user_group1")), Arrays.asList(statement1, statement2, statement3, statement4));
        ByteString stringPolicy = policy1.encode();
        //System.out.println(stringPolicy);
        
        S3UserPolicy policy2 = new S3UserPolicy(stringPolicy);
        
        //policy1 is included in policy2
        for(int i=0;i<policy1.getStatements().size();i++){
            S3Statement statPolicy1, statPolicy2;
            statPolicy1 = policy1.getStatement(i);
            statPolicy2 = policy2.getStatement(i);
            assertEquals(statPolicy1, statPolicy2);
            assertEquals(statPolicy1.getEffect(),statPolicy2.getEffect());
            assertEquals(statPolicy1.getPrincipals(),statPolicy2.getPrincipals());
            assertEquals(statPolicy1.getActions(),statPolicy2.getActions());
            assertEquals(statPolicy1.getResources(),statPolicy2.getResources());
            assertEquals(statPolicy1.getResourceBucket(),statPolicy2.getResourceBucket());
            assertEquals(statPolicy1.getConditionBlock(),statPolicy2.getConditionBlock());
        }
        for(int i=0;i<policy1.getGroups().size();i++){
            assertEquals(policy1.getGroup(i), policy2.getGroup(i));
        }
        //policy1 = policy2
        for(int i=0;i<policy2.getStatements().size();i++){
            S3Statement statPolicy1, statPolicy2;
            statPolicy1 = policy1.getStatement(i);
            statPolicy2 = policy2.getStatement(i);
            assertEquals(statPolicy1.getEffect(),statPolicy2.getEffect());
            assertEquals(statPolicy1.getPrincipals(),statPolicy2.getPrincipals());
            assertEquals(statPolicy1.getActions(),statPolicy2.getActions());
            assertEquals(statPolicy1.getResources(),statPolicy2.getResources());
            assertEquals(statPolicy1.getResourceBucket(),statPolicy2.getResourceBucket());
            assertEquals(statPolicy1.getConditionBlock(),statPolicy2.getConditionBlock());
        }
        for(int i=0;i<policy2.getGroups().size();i++){
            assertEquals(policy1.getGroup(i), policy2.getGroup(i));
        }
    }
}
