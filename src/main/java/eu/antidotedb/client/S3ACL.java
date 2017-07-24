package eu.antidotedb.client;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is just a handler to manage Policies from ACGregate
 *  it represents just ACL as set of permissions
 * @author Romain
 */
public abstract class S3ACL{
    protected Map<ByteString, Set<ByteString>> permissions;
    
    public S3ACL(){
        this.permissions = new HashMap<>();
    }
    
    
    public S3ACL(Map<String, String> rights){
        this.permissions = new HashMap<>();
        for(String user:rights.keySet()){
            this.permissions.put(ByteString.copyFromUtf8(user), encodeRight(rights.get(user)));
        }
    }
    
    public String getRight(String userid){
        return decodeRight(this.permissions.get(ByteString.copyFromUtf8(userid)));
    }
    
    /**
     * helper to translate a right to its format in ACL
     * @param right string in @code{"none","read","write","readACL","writeACL"}
     * @return set of ByteString for the corresponding right and the weaker rights
     */
    public static Set<ByteString> encodeRight(String right){
        Set<ByteString> rights = new HashSet<>();
        switch(right){
            case("default"):
                break;
            case("none"):
                rights.add(ByteString.copyFromUtf8("none"));
                break;
            case("read"):
                rights.add(ByteString.copyFromUtf8("none"));
                rights.add(ByteString.copyFromUtf8("read"));
                break;
            case("write"):
                rights.add(ByteString.copyFromUtf8("none"));
                rights.add(ByteString.copyFromUtf8("read"));
                rights.add(ByteString.copyFromUtf8("write"));
                break;
            case("readACL"):
                rights.add(ByteString.copyFromUtf8("none"));
                rights.add(ByteString.copyFromUtf8("read"));
                rights.add(ByteString.copyFromUtf8("write"));
                rights.add(ByteString.copyFromUtf8("readACL"));
                break;
            case("writeACL"):
                rights.add(ByteString.copyFromUtf8("none"));
                rights.add(ByteString.copyFromUtf8("read"));
                rights.add(ByteString.copyFromUtf8("write"));
                rights.add(ByteString.copyFromUtf8("readACL"));
                rights.add(ByteString.copyFromUtf8("writeACL"));
                break;
            default:
                throw new AccessControlException("not an ACL right");
        }
        return rights;
    }
    
    public static String decodeRight(Set<ByteString> acl){
        String result = "";
        switch(acl.size()){
            case(0):
                result="default";
                break;
            case(1):
                if(acl.equals(encodeRight("none"))){result="none";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(2):
                if(acl.equals(encodeRight("read"))){result="read";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(3):
                if(acl.equals(encodeRight("write"))){result="write";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(4):
                if(acl.equals(encodeRight("readACL"))){result="readACL";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(5):
                if(acl.equals(encodeRight("writeACL"))){result="writeACL";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            default:
                throw new AccessControlException("not an ACL right");
        }
        return result;
    }
    
    
    public void setRight(String userID, String right){
        this.permissions.put(ByteString.copyFromUtf8(userID), encodeRight(right));
    }
    
    
    public boolean explicitAllow(/*all the needed args*/){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(/*all the needed args*/){
        //TODO : Romain
        throw new UnsupportedOperationException("not implemented yet");
    }
}
