package com.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.ClassRule;
import org.junit.Test;;
import org.testcontainers.containers.GenericContainer;

import static org.junit.Assert.assertTrue;

/**
 * Created by ldoguin on 15/07/16.
 */
public class ExampleTest {

    @ClassRule
    public static GenericContainer couchbase =
            new GenericContainer("mycouchbase:latest")
                    .withExposedPorts(8091, 8092, 8093, 8094, 11207, 11210, 11211, 18091, 18092, 18093)
                    .waitingFor(new CouchbaseWaitStrategy());


    @Test
    public void beerBucketTest() throws InterruptedException {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
                .bootstrapCarrierDirectPort(couchbase.getMappedPort(11210))
                .bootstrapCarrierSslPort(couchbase.getMappedPort(11207))
                .bootstrapHttpDirectPort(couchbase.getMappedPort(8091))
                .bootstrapHttpSslPort(couchbase.getMappedPort(18091))
                .build();
        CouchbaseCluster cc = CouchbaseCluster.create(env);
        ClusterManager cm = cc.clusterManager("Administrator", "password");
        assertTrue(cm.hasBucket("beer-sample"));
        Bucket bucket = cc.openBucket("beer-sample");
        assertTrue(bucket.exists("21st_amendment_brewery_cafe"));
        bucket.close();
    }

}
