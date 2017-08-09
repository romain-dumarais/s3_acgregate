Antidote Access Control implementation
============

! CAUTION : WORK IN PROGRESS !
 * user & bucket groups : not implemented
 * static Transactions : not implemented
 * noTransaction : not implemented

This is an implementation of an Access Control System for Antidote, based on S3 Access Control model.
It is implemented above Mathias Weber implementation of ACGreGate model (Access Control for geo-replicated Gateway).

You can find the JAVA client on the repository of the SyncFree project : https://github.com/SyncFree/antidote-java-client

How to use it
-----------

You can download the Java Client by running 

		git clone https://github.com/SyncFree/antidote-java-client/git

The ACGreGate model implementation is not publicly available yet. Please feel free to contact me for further informations.

You can download this Access Control System by running 

		git clone https://github.com/romain-dumarais/antidote-java-client/git
		
To use it you have to start a local Antidote node by cloning Antidote data base and run in with : 

		git clone https://github.com/SyncFree/antidote.git
		cd antidote
		make compile
		make console

In your favorite IDE you can add these librairies to the Java Client, to use the Java API.
This Access Control also uses the Minimal JSON parser as a submodule, you can find documentation here : https://github.com/ralfstx/minimal-json

Documentation
-----------

please find all the needed informations [here](docs/README.md)

#### API ####

Create an instance of S3Client to start & commit transaction.
The data storage is partitioned into "domains", that can not intersect.

#### S3Client ####

- startTransaction(ByteString user, ByteString domain, Object userData) : starts an interactive transaction
- createStaticTransaction(ByteString user, ByteString domain, Object userData) : starts an Antidote static transaction
- noTransaction(ByteString user, ByteString domain, Object userData) : performs database operations without a transaction context.
- loginAsRoot() : use root credentials to initialize a domain and manage users

#### Managing Resources ####

find the API and the documentation for managing resources [here](docs/README.md)


Basic features
-----------

- buckets
- users
- Access resources :
	- objectACL
	- bucketACL
	- bucketPolicy
	- userPolicy

These 4 resources can be managed by admin users as specified in their Policy.



