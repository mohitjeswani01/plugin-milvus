package io.kestra.plugin.milvus;

import io.kestra.core.models.property.Property;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DropCollectionTest extends MilvusTest {

    @BeforeEach
    void setUpCollection() {
        if (!isMilvusAvailable()) return;
        var client = milvusClient();
        try {
            client.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .dimension(DIMENSION)
                .metricType(IndexParam.MetricType.COSINE.name())
                .primaryFieldName("id")
                .idType(DataType.Int64)
                .autoID(false)
                .vectorFieldName("vector")
                .build());
        } finally {
            client.close();
        }
    }

    @Test
    void run() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = DropCollection.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .build();

        var output = task.run(runContext);

        assertThat(output, is((Object) null));

        var client = milvusClient();
        try {
            boolean exists = client.hasCollection(HasCollectionReq.builder().collectionName(collectionName).build());
            assertThat(exists, is(false));
        } finally {
            client.close();
        }
    }
}
