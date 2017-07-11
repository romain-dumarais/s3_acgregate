package eu.antidotedb.client;

import java.util.List;

/**
 * A class to implement statement structure in the S3 Access Control Procedure
 * extends the OLD API
 * @author Romain
 */
public class S3Statement {
    private boolean effect;
    private String principalType;
    private List<String> principals;
    private List<String> action;
    private List<String> ressources;
    private String conditionBlock;
    
    
    /*API

  "Statement": [{
    "Sid": "1",
    "Effect": "Allow",
    "Principal": {"AWS": ["arn:aws:iam::ACCOUNT-ID-WITHOUT-HYPHENS:root"]},
    "Action": "s3:*",
    "Resource": [
      "arn:aws:s3:::mybucket",
      "arn:aws:s3:::mybucket/*"
    ]
  }]
    */

    public S3Statement(boolean effect, String principalType, List<String> principals, List<String> actions, List<String> resources, String conditionBlock){
        this.action=actions;
        this.conditionBlock=conditionBlock;
        this.effect=effect;
        this.principalType=principalType;
        this.principals=principals;
        this.ressources=resources;
    }
    
    
}
