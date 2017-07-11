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
 * TODO : everything
 */
public abstract class S3ACL{
    protected Map<ByteString, Set<ByteString>> permissions;
    
    //Romain : I would like to have methods to simply handle the setACL (==> that write the implied permissions)
    public S3ACL(){
        this.permissions = new HashMap<ByteString, Set<ByteString>>();
    }
    
    
    public S3ACL(Map<String, String> rights){
        this.permissions = new HashMap<ByteString, Set<ByteString>>();
        for(String user:rights.keySet()){
            this.permissions.put(ByteString.copyFromUtf8(user), encodeRight(rights.get(user)));
        }
    }
    
    /*
    @Override
    public String toString(){
        String acl="{ ";
        Set<ByteString> users = this.permissions.keySet();
        for(ByteString user:users){
            acl.concat("["+user.toStringUtf8()+":"+decodeRight(this.permissions.get(user))+"], ");
        }
        acl.concat(" }");
        return acl;
    }*/
    
    public String getRight(String userid){
        return decodeRight(this.permissions.get(ByteString.copyFromUtf8(userid)));
    }
    /**
     * helper to translate a right to its format in ACL
     * @param right
     * @return set of ByteString for the right and the weaker rights
     */
    public static Set<ByteString> encodeRight(String right){
        Set<ByteString> rights = new HashSet<>();
        switch(right){
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
        //may return a default right
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
    public void setRight(String userID, String right){
        this.permissions.put(ByteString.copyFromUtf8(userID), encodeRight(right));
    }
    
    
    public boolean explicitAllow(/*all the needed args*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    public boolean explicitDeny(/*all the needed args*/){
        throw new UnsupportedOperationException("not implemented yet");
    }
}
