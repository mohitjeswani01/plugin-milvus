package io.kestra.plugin.milvus;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;

public interface MilvusConnectionInterface {
    @Schema(
        title = "Milvus server URI",
        description = "The URI of the Milvus server or Zilliz Cloud endpoint, e.g. http://localhost:19530."
    )
    Property<String> getUri();

    @Schema(
        title = "API token",
        description = "The API token for authentication, typically used with Zilliz Cloud or a Milvus instance with token-based auth enabled."
    )
    Property<String> getToken();

    @Schema(
        title = "Username",
        description = "Username for a self-hosted Milvus instance with username/password authentication enabled. Leave unset if using a token or an unauthenticated instance."
    )
    Property<String> getUsername();

    @Schema(
        title = "Password",
        description = "Password for a self-hosted Milvus instance with username/password authentication enabled."
    )
    Property<String> getPassword();

    @Schema(
        title = "Database name",
        description = "The Milvus database to operate against. Defaults to the server's default database when not set."
    )
    Property<String> getDatabaseName();
}
