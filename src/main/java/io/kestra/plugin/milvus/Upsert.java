package io.kestra.plugin.milvus;

import com.google.gson.JsonObject;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Insert or update rows in a Milvus collection",
    description = "Upserts rows in configurable batch sizes. Each row is a flat map with the collection's vector field, primary key field (unless auto-generated), and any scalar fields. Reads from an inline list or a Kestra internal storage URI."
)
@Plugin(
    examples = {
        @Example(
            title = "Upsert rows with a vector field and scalar payload into a Milvus collection.",
            full = true,
            code = """
                id: milvus_upsert
                namespace: company.team

                tasks:
                  - id: upsert_rows
                    type: io.kestra.plugin.milvus.Upsert
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                    rows:
                      - id: 1
                        vector: [0.05, 0.61, 0.76, 0.74]
                        city: Berlin
                      - id: 2
                        vector: [0.19, 0.81, 0.75, 0.11]
                        city: Paris
                """
        )
    }
)
public class Upsert extends AbstractMilvus implements RunnableTask<Upsert.Output> {
    @Schema(
        title = "Collection name"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> collectionName;

    @Schema(
        title = "Rows to upsert",
        description = "Row data source, either an inline list of maps or a Kestra internal storage URI to an ION file. Each row must include the collection's vector field as a list of numbers, its primary key field (unless the collection auto-generates IDs), and any dynamic scalar fields.",
        anyOf = {String.class, List.class, Map.class}
    )
    @NotNull
    @PluginProperty(group = "main", dynamic = true)
    private Object rows;

    @Schema(
        title = "Partition name",
        description = "Optional partition to upsert the rows into."
    )
    @PluginProperty(group = "processing")
    private Property<String> partitionName;

    @Schema(
        title = "Batch size",
        description = "The number of rows sent per upsert call."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<@Min(1) @Max(10000) Integer> batchSize = Property.ofValue(1000);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rCollectionName = runContext.render(this.collectionName).as(String.class).orElseThrow(() -> new IllegalArgumentException("'collectionName' is required"));
        var rPartitionName = runContext.render(this.partitionName).as(String.class).orElse(null);
        var rBatchSize = runContext.render(this.batchSize).as(Integer.class).orElse(1000);

        MilvusClientV2 client = this.buildClient(runContext);
        try {
            long totalUpserted = 0;
            List<JsonObject> batch = new java.util.ArrayList<>(rBatchSize);

            var rows = Data.from(this.rows).read(runContext).toIterable();
            for (Map<String, Object> row : rows) {
                batch.add(toJsonObject(row));
                if (batch.size() >= rBatchSize) {
                    totalUpserted += upsertBatch(client, rCollectionName, rPartitionName, batch);
                    batch = new java.util.ArrayList<>(rBatchSize);
                }
            }
            if (!batch.isEmpty()) {
                totalUpserted += upsertBatch(client, rCollectionName, rPartitionName, batch);
            }

            runContext.logger().info("Upserted {} row(s) into collection '{}'", totalUpserted, rCollectionName);

            return Output.builder().upsertedCount(totalUpserted).build();
        } finally {
            client.close();
        }
    }

    private static long upsertBatch(MilvusClientV2 client, String collectionName, String partitionName, List<JsonObject> batch) {
        UpsertReq.UpsertReqBuilder requestBuilder = UpsertReq.builder()
            .collectionName(collectionName)
            .data(batch);
        if (partitionName != null && !partitionName.isBlank()) {
            requestBuilder.partitionName(partitionName);
        }
        return client.upsert(requestBuilder.build()).getUpsertCnt();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Number of upserted rows"
        )
        private final long upsertedCount;
    }
}
