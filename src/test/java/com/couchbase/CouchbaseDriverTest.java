package com.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

public class CouchbaseDriverTest {

	@ClassRule
	public static CouchbaseContainer couchbase = new CouchbaseContainer()
            .withBeerSample(true)
            .withGamesIMSample(true)
            .withTravelSample(true)
            .withFTS(false)
            .withIndex(false)
            .withQuery(false)
            .withClusterUsername("Administrator")
            .withClusterPassword("password");

	@Test
	public void testSimple() throws Exception {
        CouchbaseCluster cc = couchbase.geCouchbaseCluster();
        ClusterManager cm = cc.clusterManager("Administrator","password");
        List<BucketSettings> buckets = cm.getBuckets();
        Assert.assertNotNull(buckets);
        Assert.assertTrue(buckets.size() == 3);
        BucketSettings settings = DefaultBucketSettings.builder()
                .enableFlush(true).name("default").quota(100).replicas(0).type(BucketType.COUCHBASE).build();
        settings = cm.insertBucket(settings);
        CouchbaseWaitStrategy s = new CouchbaseWaitStrategy();
        s.withBasicCredentials("Administrator", "password");
        s.waitUntilReady(couchbase);
        Bucket bucket = cc.openBucket("default");
        Assert.assertNotNull(bucket);
	}
}
