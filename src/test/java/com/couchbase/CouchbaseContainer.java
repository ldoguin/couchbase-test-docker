package com.couchbase;

import com.couchbase.client.core.message.config.RestApiRequest;
import com.couchbase.client.core.message.config.RestApiResponse;
import com.couchbase.client.core.utils.Base64;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpHeaders;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.api.ClusterApiClient;
import com.couchbase.client.java.cluster.api.RestBuilder;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;

/**
 * @author Laurent Doguin
 */
public class CouchbaseContainer<SELF extends CouchbaseContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    private String memoryQuota = "400";

    private String indexMemoryQuota = "400";

    private String clusterUsername = "Administrator";

    private String clusterPassword = "password";

    private Boolean keyValue = true;

    private Boolean query = true;

    private Boolean index = true;

    private Boolean fts = true;

    private Boolean beerSample = false;

    private Boolean travelSample = false;

    private Boolean gamesIMSample = false;

    private CouchbaseEnvironment couchbaseEnvironment;

    private CouchbaseCluster couchbaseCluster;

    public CouchbaseContainer() {
        super("couchbase:4.5.0");
    }

    public CouchbaseContainer(String containerName) {
        super(containerName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(8091);
    }

    @Override
    protected void configure() {
        addExposedPorts(8091, 8092, 8093, 8094, 11207, 11210, 11211, 18091, 18092, 18093);
        setWaitStrategy(new HttpWaitStrategy().forPath("/ui/index.html#/"));
    }

    public CouchbaseEnvironment getCouchbaseEnvironnement() {
        if (couchbaseEnvironment == null) {
            initCluster();
            couchbaseEnvironment = DefaultCouchbaseEnvironment.builder()
                    .bootstrapCarrierDirectPort(getMappedPort(11210))
                    .bootstrapCarrierSslPort(getMappedPort(11207))
                    .bootstrapHttpDirectPort(getMappedPort(8091))
                    .bootstrapHttpSslPort(getMappedPort(18091))
                    .build();
        }
        return couchbaseEnvironment;
    }

    public CouchbaseCluster geCouchbaseCluster() {
        if (couchbaseCluster == null) {
            couchbaseCluster = CouchbaseCluster.create(getCouchbaseEnvironnement(), getContainerIpAddress());
        }
        return couchbaseCluster;
    }

    public SELF withClusterUsername(String username) {
        this.clusterUsername = username;
        return self();
    }

    public SELF withClusterPassword(String password) {
        this.clusterPassword = password;
        return self();
    }

    public SELF withMemoryQuota(String memoryQuota) {
        this.memoryQuota = memoryQuota;
        return self();
    }

    public SELF withIndexMemoryQuota(String indexMemoryQuota) {
        this.indexMemoryQuota = indexMemoryQuota;
        return self();
    }

    public SELF withKeyValue(Boolean withKV) {
        this.keyValue = withKV;
        return self();
    }

    public SELF withIndex(Boolean withIndex) {
        this.index = withIndex;
        return self();
    }

    public SELF withQuery(Boolean withQuery) {
        this.query = withQuery;
        return self();
    }

    public SELF withFTS(Boolean withFTS) {
        this.fts = withFTS;
        return self();
    }

    public SELF withTravelSample(Boolean withTravelSample) {
        this.travelSample = withTravelSample;
        return self();
    }

    public SELF withBeerSample(Boolean withBeerSample) {
        this.beerSample = withBeerSample;
        return self();
    }

    public SELF withGamesIMSample(Boolean withGamesIMSample) {
        this.gamesIMSample = withGamesIMSample;
        return self();
    }


    public void initCluster() {
        try {
            String urlBase = String.format("http://%s:%s", getContainerIpAddress(), getMappedPort(8091));

            String poolURL = urlBase + "/pools/default";
            String poolPayload = "memoryQuota=" + URLEncoder.encode(memoryQuota, "UTF-8") + "&indexMemoryQuota=" + URLEncoder.encode(indexMemoryQuota, "UTF-8");

            String setupServicesURL = urlBase + "/node/controller/setupServices";
            StringBuilder servicePayloadBuilder = new StringBuilder();
            if (keyValue) {
                servicePayloadBuilder.append("kv,");
            }
            if (query) {
                servicePayloadBuilder.append("n1ql,");
            }
            if (index) {
                servicePayloadBuilder.append("index,");
            }
            if (fts) {
                servicePayloadBuilder.append("fts,");
            }
            String setupServiceContent = "services=" + URLEncoder.encode(servicePayloadBuilder.toString(), "UTF-8");

            String webSettingsURL = urlBase + "/settings/web";
            String webSettingsContent = "username=" + URLEncoder.encode(clusterUsername, "UTF-8") + "&password=" + URLEncoder.encode(clusterPassword, "UTF-8") + "&port=8091";

            String bucketURL = urlBase + "/sampleBuckets/install";

            StringBuilder sampleBucketPayloadBuilder = new StringBuilder();
            sampleBucketPayloadBuilder.append('[');
            if (travelSample) {
                sampleBucketPayloadBuilder.append("\"travel-sample\",");
            }
            if (beerSample) {
                sampleBucketPayloadBuilder.append("\"beer-sample\",");
            }
            if (gamesIMSample) {
                sampleBucketPayloadBuilder.append("\"gamesim-sample\",");
            }
            sampleBucketPayloadBuilder.append(']');

            callCouchbaseRestAPI(poolURL, poolPayload);
            callCouchbaseRestAPI(setupServicesURL, setupServiceContent);
            callCouchbaseRestAPI(webSettingsURL, webSettingsContent);
            callCouchbaseRestAPI(bucketURL, sampleBucketPayloadBuilder.toString(), clusterUsername, clusterPassword);

            CouchbaseWaitStrategy s = new CouchbaseWaitStrategy();
            s.withBasicCredentials("Administrator", "password");
            s.waitUntilReady(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContainerName() {
        return "couchbase";
    }

    public void callCouchbaseRestAPI(String url, String content) throws IOException {
        callCouchbaseRestAPI(url, content, null, null);
    }

    public void callCouchbaseRestAPI(String url, String payload, String username, String password) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) ((new URL(url).openConnection()));
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        if (username != null) {
            String encoded = Base64.encode((username + ":" + password).getBytes("UTF-8"));
            httpConnection.setRequestProperty("Authorization", "Basic " + encoded);
        }
        DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
        out.writeBytes(payload);
        out.flush();
        out.close();
        httpConnection.getResponseCode();
        httpConnection.disconnect();


    }

    public void restAPICALL(String url, String payload) throws IOException {
        ClusterApiClient apiClient = couchbaseCluster.clusterManager().apiClient();
        RestBuilder rest = apiClient.post("settings", "web")
                .withHeader(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
        rest.body(payload);

        RestApiResponse response = rest.execute();
        

    }

    public void importData(String path) {

    }

}
