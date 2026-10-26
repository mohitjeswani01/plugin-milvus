package io.kestra.plugin.milvus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchOutput;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Search for similar vectors in a Milvus collection.",
    description = "Runs an approximate nearest-neighbor vector similarity search, optionally narrowed with a scalar filter expression."
)
@Plugin(
    examples = {
        @Example(
            title = "Search for the 5 nearest neighbors of a query vector.",
            full = true,
            code = """
                id: milvus_search
                namespace: company.team

                tasks:
                  - id: search
                    type: io.kestra.plugin.milvus.Search
                    uri: http://localhost:19530
                    token: "{{ secret('MILVUS_TOKEN') }}"
                    collectionName: documents
                    vector: [0.05, 0.61, 0.76, 0.74]
                    limit: 5
                """
        )
    }
)
public class Search extends AbstractMilvus implements RunnableTask<FetchOutput> {
    @Schema(
        title = "Collection name"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> collectionName;

    @Schema(
        title = "Query vector",
        description = "The dense vector to search nearest neighbors for, as a list of numbers matching the collection's vector dimension."
    )
    @NotNull
    @PluginProperty(group = "main", dynamic = true)
    private Property<List<Double>> vector;

    @Schema(
        title = "Result limit",
        description = "The maximum number of nearest neighbors to return."
    )
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<@Min(1) @Max(16384) Integer> limit = Property.ofValue(10);

    @Schema(
        title = "Scalar filter expression",
        description = "A Milvus boolean filter expression narrowing the search to rows matching the condition, e.g. \"city == 'Berlin'\"."
    )
    @PluginProperty(group = "processing", dynamic = true)
    private Property<String> filter;

    @Schema(
        title = "Output fields",
        description = "Scalar fields to include in each result's entity map, in addition to the primary key and score."
    )
    @PluginProperty(group = "processing")
    private Property<List<String>> outputFields;

    @Schema(
        title = "Partition names",
        description = "Optional partitions to scope the search to."
    )
    @PluginProperty(group = "processing")
    private Property<List<String>> partitionNames;

    @Schema(
        title = "Fetch type",
        description = "How search results are returned: FETCH_ONE for the single best match, FETCH for all matches inline, STORE to write all matches to internal storage and return a URI."
    )
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public FetchOutput run(RunContext runContext) throws Exception {
        var rCollectionName = runContext.render(this.collectionName).as(String.class).orElseThrow(() -> new IllegalArgumentException("'collectionName' is required"));
        var rVector = runContext.render(this.vector).asList(Double.class);
        if (rVector.isEmpty()) {
            throw new IllegalArgumentException("'vector' is required and must not be empty");
        }
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(10);
        var rFilter = runContext.render(this.filter).as(String.class).orElse(null);
        var rOutputFields = runContext.render(this.outputFields).asList(String.class);
        var rPartitionNames = runContext.render(this.partitionNames).asList(String.class);
        var rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.FETCH);

        List<Float> queryVector = rVector.stream().map(Double::floatValue).toList();

        MilvusClientV2 client = this.buildClient(runContext);
        try {
            SearchReq.SearchReqBuilder requestBuilder = SearchReq.builder()
                .collectionName(rCollectionName)
                .data(List.of(new FloatVec(queryVector)))
                .limit(rLimit);
            if (rFilter != null && !rFilter.isBlank()) {
                requestBuilder.filter(rFilter);
            }
            if (!rOutputFields.isEmpty()) {
                requestBuilder.outputFields(rOutputFields);
            }
            if (!rPartitionNames.isEmpty()) {
                requestBuilder.partitionNames(rPartitionNames);
            }

            SearchResp response = client.search(requestBuilder.build());

            List<Map<String, Object>> rows = new ArrayList<>();
            if (!response.getSearchResults().isEmpty()) {
                for (SearchResp.SearchResult result : response.getSearchResults().get(0)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", result.getId());
                    row.put("score", result.getScore());
                    row.putAll(result.getEntity());
                    rows.add(row);
                }
            }

            return switch (rFetchType) {
                case FETCH_ONE -> FetchOutput.builder()
                    .size(rows.isEmpty() ? 0L : 1L)
                    .row(rows.isEmpty() ? null : rows.get(0))
                    .build();
                case STORE -> {
                    var tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                    try (var output = new java.io.BufferedOutputStream(new java.io.FileOutputStream(tempFile))) {
                        for (Map<String, Object> row : rows) {
                            io.kestra.core.serializers.FileSerde.write(output, row);
                        }
                        output.flush();
                    }
                    yield FetchOutput.builder()
                        .size((long) rows.size())
                        .uri(runContext.storage().putFile(tempFile))
                        .build();
                }
                default -> FetchOutput.builder()
                    .size((long) rows.size())
                    .rows(new ArrayList<>(rows))
                    .build();
            };
        } finally {
            client.close();
        }
    }
}
