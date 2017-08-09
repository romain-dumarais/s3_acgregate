package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.AccessControlException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * this class represents just ACL as set of permissions
 * @author romain-dumarais
 */
public abstract class S3ACL{
    protected Map<ByteString, Set<ByteString>> permissions;
    
    public S3ACL(){
        this.permissions = new HashMap<>();
    }
    
    public S3ACL(Map<String, String> rights){
        this.permissions = new HashMap<>();
        for(String user:rights.keySet()){
            this.permissions.put(ByteString.copyFromUtf8(user), this.encodeRight(rights.get(user)));
        }
    }
    
    public String getRight(String userid){
        return decodeRight(this.permissions.get(ByteString.copyFromUtf8(userid)));
    }
    
    public void setRight(String userID, String right){
        this.permissions.put(ByteString.copyFromUtf8(userID), this.encodeRight(right));
    }
    
    /**
     * helper to translate a right to its format in ACL
     * @param right string in @code{"none","read","write","readACL","writeACL"}
     * @return set of ByteString for the corresponding right and the weaker rights
     */
    public abstract Set<ByteString> encodeRight(String right);

    /**
     * helper to read a encoded right
     * @param acl set of rights as encoded in Antidote
     * @return String {@code "default" | "none" | "read" | "write" | "readACL" | "writeACL"}
     */
    public String decodeRight(Set<ByteString> acl){
        String result = "";
        switch(acl.size()){
            case(0):
                result="default";
                break;
            case(1):
                if(acl.equals(this.encodeRight("none"))){result="none";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(2):
                if(acl.equals(this.encodeRight("read"))){result="read";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(3):
                if(acl.equals(this.encodeRight("write"))){result="write";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(4):
                if(acl.equals(this.encodeRight("readACL"))){result="readACL";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            case(5):
                if(acl.equals(this.encodeRight("writeACL"))){result="writeACL";}
                else{throw new AccessControlException("not an ACL right");}
                break;
            default:
                throw new AccessControlException("not an ACL right");
        }
        return result;
    }


}
