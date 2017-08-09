Use S3 Access Control for Antidote
-----------

please find the formalization of the model [here](20170809_draft7.pdf).

## invariants ##

This S3 implementation provide 3 invariants :
 * no data can be accessed by a user 'userA' until its root owner grants 'userA' permissions on this data.
 * as soon as an update is visible, where the permissions of a user 'userA' on a data is explcitly revoked, 'userA' can not access this data until the permission is granted again.
 * there is always at least one user who can access a data (i.e and its metadata).
 
In this implementation, we separate the database into 'domains', and every domain has root credentials to create and delete buckets & users.
Please take note that in the second invariant, the action to assign a user to a group is not an explicit right revocation, and does not preserve this invariant. But to revoke a group permission verify this invariant.

## Resource management ##

Access Resources are organised as follows : 
![S3-ACGreGate_Access_Resources](SeenResources.png)

### Managing ACLs ###

- ACL can be build with an object : 

		Map<String user, String permissions>

- to read ACLs from the database, create an empty S3ACL object and call : 

		aclObject.readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid)

- to assign permissions to a remote ACL, create an object, pass it the permissions and assign it, for example with an object ACL :

		S3ACL acl = new S3ObjectACL();
		acl.setRight("user1", "read");
		acl.setRight("user2", "write");
		acl.assign(s3Transaction, bucketName, objectKey); 
- there is a static method to assign permission to a user in a remote ACL : 
		S3ACL.assignForUserStatic(s3Transaction, bucketName, userID, rightToAssign);

		
### Managing Policies ###

- Policies can be build with a list of groups and a list of S3Statement object : 

		S3Policy(List<ByteString> groups, List<S3Statement> statements)		

- to read a Policy from the database, create an empty S3Policy object and call : 

		aclObject.readForUser(S3InteractiveTransaction tx, ByteString bucket, ByteString userid)

- to assign permissions to a remote ACL, create an object, pass it the permissions and assign it, for example with an object ACL :

		S3Policy bucketPolicy = new S3BucketPolicy();
		bucketPolicy.addStatement(statement1);
		bucketPolicy.assign(s3Transaction, bucketKey);


#### Statement Object ####

S3Statement represent a permission, with the following fields : 
- effect : allow or deny
- principals : list of actors that can perform actions
- action : list of actions 
- ressources : either a list of bucket, of object keys or of object type
- conditionBlock : optional conditions on the access environment

S3Statement may be build with the API :

		S3Statement(boolean effect, List<String> principals, List<String> actions, ByteString bucketKey, String conditionBlock);


### Access Procedure ###

The access decision is performed using the following process :

![S3-ACGreGate_Access_Procedure](AccessDecision.png)

#### credits #### 
icons in following pictures :

  * MadeByOliver
  * Icon Pound

