package io.kestra.plugin.milvus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete rows from a Milvus collection.",
    description = "Deletes rows by a list of primary key IDs, or by a scalar filter expression. Exactly one of ids or filter must be set."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete rows by primary key ID.",
            full = true,
            code = """
                id: milvus_delete
                namespace: company.team

                tasks:
                  - id: delete_rows
                    type: io.kestra.plugin.milvus.Delete
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                    ids: [1, 2, 3]
                """
        ),
        @Example(
            title = "Delete rows matching a scalar filter expression.",
            full = true,
            code = """
                id: milvus_delete_by_filter
                namespace: company.team

                tasks:
                  - id: delete_rows
                    type: io.kestra.plugin.milvus.Delete
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                    filter: "city == 'Berlin'"
                """
        )
    }
)
public class Delete extends AbstractMilvus implements RunnableTask<Delete.Output> {
    @Schema(
        title = "Collection name"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> collectionName;

    @Schema(
        title = "Primary key IDs to delete",
        description = "A list of primary key values to delete. Mutually exclusive with filter."
    )
    @PluginProperty(group = "main", dynamic = true)
    private Property<List<Object>> ids;

    @Schema(
        title = "Scalar filter expression",
        description = "A Milvus boolean filter expression; every row matching it is deleted. Mutually exclusive with ids."
    )
    @PluginProperty(group = "main", dynamic = true)
    private Property<String> filter;

    @Schema(
        title = "Partition name",
        description = "Optional partition to scope the delete to."
    )
    @PluginProperty(group = "processing")
    private Property<String> partitionName;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rCollectionName = runContext.render(this.collectionName).as(String.class).orElseThrow(() -> new IllegalArgumentException("'collectionName' is required"));
        var rIds = runContext.render(this.ids).asList(Object.class);
        var rFilter = runContext.render(this.filter).as(String.class).orElse(null);
        var rPartitionName = runContext.render(this.partitionName).as(String.class).orElse(null);

        boolean hasIds = !rIds.isEmpty();
        boolean hasFilter = rFilter != null && !rFilter.isBlank();
        if (hasIds == hasFilter) {
            throw new IllegalArgumentException("Exactly one of 'ids' or 'filter' must be set — got " + (hasIds ? "both" : "neither"));
        }

        MilvusClientV2 client = this.buildClient(runContext);
        try {
            DeleteReq.DeleteReqBuilder requestBuilder = DeleteReq.builder().collectionName(rCollectionName);
            if (hasIds) {
                requestBuilder.ids(rIds);
            } else {
                requestBuilder.filter(rFilter);
            }
            if (rPartitionName != null && !rPartitionName.isBlank()) {
                requestBuilder.partitionName(rPartitionName);
            }

            long deletedCount = client.delete(requestBuilder.build()).getDeleteCnt();

            runContext.logger().info("Deleted {} row(s) from collection '{}'", deletedCount, rCollectionName);

            return Output.builder().deletedCount(deletedCount).build();
        } finally {
            client.close();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Number of deleted rows"
        )
        private final long deletedCount;
    }
}
