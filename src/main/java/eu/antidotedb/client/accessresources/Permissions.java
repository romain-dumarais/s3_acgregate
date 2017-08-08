package eu.antidotedb.client.accessresources;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.decision.S3Request;
import java.util.Collection;

/**
 * simplified ACL for management in decision procedure
 * @author romain-dumarais
 */
public class Permissions implements S3AccessResource{
    private final Collection<ByteString> permissions;

    public Permissions(Collection<ByteString> permissions) {
        this.permissions=permissions;
    }

    @Override
    public boolean explicitDeny(S3Request request) {
        if(permissions.isEmpty()){
            return false;
        }else{
            return !permissions.contains(ByteString.copyFromUtf8(request.action.toString()));
        }
    }

    @Override
    public boolean explicitAllow(S3Request request) {
        return this.permissions.contains(ByteString.copyFromUtf8(request.action.toString()));
    }

}
