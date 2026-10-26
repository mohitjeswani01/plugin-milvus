package io.kestra.plugin.milvus;

import io.kestra.core.models.property.Property;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CreateCollectionTest extends MilvusTest {

    @Test
    void run() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = CreateCollection.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .dimension(Property.ofValue(DIMENSION))
            .metricType(Property.ofValue(IndexParam.MetricType.COSINE))
            .build();

        var output = task.run(runContext);

        assertThat(output, is((Object) null));

        var client = milvusClient();
        try {
            boolean exists = client.hasCollection(HasCollectionReq.builder().collectionName(collectionName).build());
            assertThat(exists, is(true));
        } finally {
            client.close();
        }
    }

    @AfterEach
    void tearDown() {
        if (!isMilvusAvailable()) return;
        var client = milvusClient();
        try {
            client.dropCollection(DropCollectionReq.builder().collectionName(collectionName).build());
        } catch (Exception ignored) {
        } finally {
            client.close();
        }
    }
}
