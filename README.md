Antidote Access Control implementation
============

! CAUTION : WORK IN PROGRESS !

This is an implementation of an Access Control System for Antidote, based on S3 Access Control model.
It is based from Mathias Weber implementation of ACGreGate Access Control model for geo-replicated databases.

You can find the JAVA client on the repository of the SyncFree project : https://github.com/SyncFree/antidote-java-client

To use it
-----------

You can download the Java Client by running 

		git clone https://github.com/SyncFree/antidote-java-client/git

You can download this Access Control System by running 

		git clone https://github.com/romain-dumarais/antidote-java-client/git
		
To use it you have to start an Antidote node by cloning Antidote data base and run in with : 

		git clone https://github.com/SyncFree/antidote.git
		cd antidote
		make compile
		make console

In your favorite IDE you can add these librairies to the Java Client, to use the Java API.

### API ###

Create an instance of S3Client to start & commit transaction.
The data storage is partitioned into "domains", that can not intersect.
To initialise the domain, use loginAsRoot function in the S3Client.

Basic features
-----------

- buckets
- users
- Access resources :
	- objectACL
	- bucketACL
	- bucketPolicy
	- userPolicy

