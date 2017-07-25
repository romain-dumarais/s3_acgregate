package eu.antidotedb.client.test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.ByteString;
import eu.antidotedb.client.accessresources.S3Statement;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * class to test JSON parsing of Policies
 * @author Romain
 */
public class JsonTest {
    private final S3Statement statement = new S3Statement(true,Arrays.asList("user1","user2"),Arrays.asList("*"), ByteString.copyFromUtf8("testBucket"), Arrays.asList("object1","object2"), "this is a condition block");
    
    @Test
    public void decode(){
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
    public void encode(){
        //System.out.println(statement.encode().toString());
        JsonValue value = Json.parse(statement.encode().toString()).asObject();
        //System.out.println(value.isObject());
    }
    
    @Test
    public void round1(){
        String stringStatement0 = "{\"Effect\":true,\"Principals\":[\"user1\"],\"Actions\":[\"*\"],\"Resources\":{\"bucket\":\"testBucketS3\",\"objects\":[\"object1TestS3\",\"object2TestS3\"]}}";
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
        System.out.println("____round 2____");
        JsonObject jsonstatement1 = statement.encode();
        S3Statement statement2 = S3Statement.decodeStatic(jsonstatement1);
        System.out.println("statement2 : "+statement2.getEffect()+" : "+statement2.getPrincipals().toString()+" : "+statement2.getActions().toString()+" : "+statement2.getResources().toString()+" : "+statement2.getResourceBucket().toStringUtf8()+" : "+statement2.getConditionBlock());
        System.out.println("statement0 : "+statement.getEffect()+" : "+statement.getPrincipals().toString()+" : "+statement.getActions().toString()+" : "+statement.getResources().toString()+" : "+statement.getResourceBucket().toStringUtf8()+" : "+statement.getConditionBlock());
        System.err.println("round 2 : "+statement.toString().equals(statement2.toString()));
        System.out.println("round 2 : "+statement.toString());
        System.out.println("round 2 : "+statement2.toString());
        assertEquals(statement,statement2);
    }
}
