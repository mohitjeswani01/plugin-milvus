package io.kestra.plugin.milvus;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SearchTest extends MilvusTest {

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

            client.upsert(UpsertReq.builder()
                .collectionName(collectionName)
                .data(List.of(
                    toJsonObject(Map.of("id", 1, "vector", List.of(0.05f, 0.61f, 0.76f, 0.74f))),
                    toJsonObject(Map.of("id", 2, "vector", List.of(0.19f, 0.81f, 0.75f, 0.11f)))
                ))
                .build());
        } finally {
            client.close();
        }
    }

    @Test
    void runFetch() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .vector(Property.ofValue(List.of(0.05, 0.61, 0.76, 0.74)))
            .limit(Property.ofValue(2))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getRows(), notNullValue());
        assertThat(output.getRows().size(), is(2));
    }

    @Test
    void runFetchOne() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .vector(Property.ofValue(List.of(0.05, 0.61, 0.76, 0.74)))
            .limit(Property.ofValue(1))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(1L));
        assertThat(output.getRow(), notNullValue());
        assertThat(output.getRow().get("id"), is(1L));
    }

    @Test
    void runStore() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .vector(Property.ofValue(List.of(0.05, 0.61, 0.76, 0.74)))
            .limit(Property.ofValue(2))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getUri(), notNullValue());
    }

    @Test
    void runWithFilter() throws Exception {
        assumeTrue(isMilvusAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .uri(Property.ofValue(uri))
            .collectionName(Property.ofValue(collectionName))
            .vector(Property.ofValue(List.of(0.05, 0.61, 0.76, 0.74)))
            .filter(Property.ofValue("id == 1"))
            .limit(Property.ofValue(10))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), greaterThan(0L));
        assertThat(output.getRows().get(0), notNullValue());
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
