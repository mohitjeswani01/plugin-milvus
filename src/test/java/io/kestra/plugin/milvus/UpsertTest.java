package io.kestra.plugin.milvus;

import io.kestra.core.models.property.Property;
import io.kestra.core.serializers.FileSerde;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class UpsertTest extends MilvusTest {

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

        List<Map<String, Object>> rows = List.of(
            Map.of("id", 1, "vector", List.of(0.05, 0.61, 0.76, 0.74)),
            Map.of("id", 2, "vector", List.of(0.19, 0.81, 0.75, 0.11))
        );

        var task = Upsert.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .rows(rows)
            .batchSize(Property.ofValue(10))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getUpsertedCount(), is(2L));
    }

    @Test
    void runUpsertFromInternalStorage() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (var output = new FileOutputStream(tempFile)) {
            FileSerde.write(output, Map.of("id", 10, "vector", List.of(0.05, 0.61, 0.76, 0.74)));
            FileSerde.write(output, Map.of("id", 11, "vector", List.of(0.19, 0.81, 0.75, 0.11)));
        }

        var storageUri = runContext.storage().putFile(tempFile);

        var task = Upsert.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .rows(storageUri.toString())
            .batchSize(Property.ofValue(1))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getUpsertedCount(), is(2L));
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
