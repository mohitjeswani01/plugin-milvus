package io.kestra.plugin.milvus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a Milvus collection",
    description = "Creates a collection with a single vector field and a primary key field, using Milvus's quick-setup schema. For a custom multi-field schema, create the collection with the Milvus SDK or console first and use the Upsert, Search, and Delete tasks against it directly."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a collection for 768-dimension embeddings with cosine similarity.",
            full = true,
            code = """
                id: milvus_create_collection
                namespace: company.team

                tasks:
                  - id: create_collection
                    type: io.kestra.plugin.milvus.CreateCollection
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                    dimension: 768
                """
        )
    }
)
public class CreateCollection extends AbstractMilvus implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Collection name"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> collectionName;

    @Schema(
        title = "Vector dimension",
        description = "The number of dimensions of the vectors stored in this collection's vector field."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<@Min(1) @Max(32768) Integer> dimension;

    @Schema(
        title = "Collection description"
    )
    @PluginProperty(group = "main")
    private Property<String> collectionDescription;

    @Schema(
        title = "Metric type",
        description = "The similarity metric used for vector search on this collection."
    )
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<IndexParam.MetricType> metricType = Property.ofValue(IndexParam.MetricType.COSINE);

    @Schema(
        title = "Primary field name",
        description = "The name of the primary key field."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<String> primaryFieldName = Property.ofValue("id");

    @Schema(
        title = "Primary key type",
        description = "The data type of the primary key field. Int64 is auto-incrementing when autoId is true; VarChar requires the caller to supply an id on insert."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<DataType> idType = Property.ofValue(DataType.Int64);

    @Schema(
        title = "Auto-generate IDs",
        description = "When true, Milvus assigns the primary key automatically and it must be omitted on insert. Only valid when idType is Int64."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> autoId = Property.ofValue(false);

    @Schema(
        title = "Vector field name"
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<String> vectorFieldName = Property.ofValue("vector");

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        MilvusClientV2 client = this.buildClient(runContext);
        try {
            var rCollectionName = runContext.render(this.collectionName).as(String.class).orElseThrow(() -> new IllegalArgumentException("'collectionName' is required"));
            var rDimension = runContext.render(this.dimension).as(Integer.class).orElseThrow(() -> new IllegalArgumentException("'dimension' is required"));
            var rDescription = runContext.render(this.collectionDescription).as(String.class).orElse("");
            var rMetricType = runContext.render(this.metricType).as(IndexParam.MetricType.class).orElse(IndexParam.MetricType.COSINE);
            var rPrimaryFieldName = runContext.render(this.primaryFieldName).as(String.class).orElse("id");
            var rIdType = runContext.render(this.idType).as(DataType.class).orElse(DataType.Int64);
            var rAutoId = runContext.render(this.autoId).as(Boolean.class).orElse(false);
            var rVectorFieldName = runContext.render(this.vectorFieldName).as(String.class).orElse("vector");

            CreateCollectionReq request = CreateCollectionReq.builder()
                .collectionName(rCollectionName)
                .dimension(rDimension)
                .description(rDescription)
                .metricType(rMetricType.name())
                .primaryFieldName(rPrimaryFieldName)
                .idType(rIdType)
                .autoID(rAutoId)
                .vectorFieldName(rVectorFieldName)
                .build();

            client.createCollection(request);

            runContext.logger().info("Created Milvus collection '{}' (dimension={}, metricType={})", rCollectionName, rDimension, rMetricType);

            return null;
        } finally {
            client.close();
        }
    }
}
