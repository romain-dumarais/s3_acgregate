package eu.antidotedb.client.accessresources;

import eu.antidotedb.client.decision.S3Request;


/**
 * interface used in decision procedure to manage Policies & ACL
 * @author romain-dumarais
 */
public interface S3AccessResource {
    
    boolean explicitDeny(S3Request request);   
    
    boolean explicitAllow(S3Request request);
}
