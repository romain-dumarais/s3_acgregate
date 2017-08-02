package eu.antidotedb.client.accessresources;

/**
 * ENUM class for operations
 * @author romain-dumarais
 */
public enum S3Operation {
    READOBJECT, WRITEOBJECT,
    APPLICATIONOPERATION,
    READOBJECTACL,WRITEOBJECTACL, READBUCKETACL, WRITEBUCKETACL,
    ASSIGNBUCKETPOLICY,READBUCKETPOLICY,READUSERPOLICY,ASSIGNUSERPOLICY;
}
