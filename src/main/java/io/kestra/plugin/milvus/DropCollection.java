package io.kestra.plugin.milvus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Drop a Milvus collection",
    description = "Permanently deletes a collection and all of its rows. This cannot be undone."
)
@Plugin(
    examples = {
        @Example(
            title = "Drop a collection.",
            full = true,
            code = """
                id: milvus_drop_collection
                namespace: company.team

                tasks:
                  - id: drop_collection
                    type: io.kestra.plugin.milvus.DropCollection
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                """
        )
    }
)
public class DropCollection extends AbstractMilvus implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Collection name"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> collectionName;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var rCollectionName = runContext.render(this.collectionName).as(String.class).orElseThrow(() -> new IllegalArgumentException("'collectionName' is required"));

        MilvusClientV2 client = this.buildClient(runContext);
        try {
            client.dropCollection(DropCollectionReq.builder().collectionName(rCollectionName).build());

            runContext.logger().info("Dropped Milvus collection '{}'", rCollectionName);

            return null;
        } finally {
            client.close();
        }
    }
}
