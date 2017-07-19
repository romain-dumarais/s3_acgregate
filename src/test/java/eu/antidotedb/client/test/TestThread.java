package eu.antidotedb.client.test;

import com.google.protobuf.ByteString;
import eu.antidotedb.client.Host;
import eu.antidotedb.client.S3Client;

/**
 *
 * @author hitman
 */
class TestThread implements Runnable {
    private final ByteString user;
    private final S3Client client;
    
    public TestThread(ByteString user, Host host){
        this.client=new S3Client(host);
        this.user=user;
    }

    @Override
    public void run() {
        //TODO : Romain
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
