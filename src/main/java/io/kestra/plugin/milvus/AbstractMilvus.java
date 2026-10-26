package io.kestra.plugin.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractMilvus extends Task implements MilvusConnectionInterface {
    private static final Gson GSON = new Gson();

    @Schema(
        title = "Milvus server URI",
        description = "The URI of the Milvus server or Zilliz Cloud endpoint, e.g. http://localhost:19530."
    )
    @NotNull
    @PluginProperty(group = "connection")
    protected Property<String> uri;

    @Schema(
        title = "API token",
        description = "The API token for authentication, typically used with Zilliz Cloud or a Milvus instance with token-based auth enabled."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    protected Property<String> token;

    @Schema(
        title = "Username",
        description = "Username for a self-hosted Milvus instance with username/password authentication enabled. Leave unset if using a token or an unauthenticated instance."
    )
    @PluginProperty(group = "connection")
    protected Property<String> username;

    @Schema(
        title = "Password",
        description = "Password for a self-hosted Milvus instance with username/password authentication enabled."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    protected Property<String> password;

    @Schema(
        title = "Database name",
        description = "The Milvus database to operate against. Defaults to the server's default database when not set."
    )
    @PluginProperty(group = "connection")
    protected Property<String> databaseName;

    protected MilvusClientV2 buildClient(RunContext runContext) throws IllegalVariableEvaluationException {
        var rUri = runContext.render(this.uri).as(String.class).orElseThrow(() -> new IllegalArgumentException("'uri' is required"));
        var rToken = runContext.render(this.token).as(String.class).orElse(null);
        var rUsername = runContext.render(this.username).as(String.class).orElse(null);
        var rPassword = runContext.render(this.password).as(String.class).orElse(null);
        var rDatabaseName = runContext.render(this.databaseName).as(String.class).orElse(null);

        ConnectConfig.ConnectConfigBuilder configBuilder = ConnectConfig.builder().uri(rUri);
        if (rToken != null && !rToken.isBlank()) {
            configBuilder.token(rToken);
        }
        if (rUsername != null && !rUsername.isBlank()) {
            configBuilder.username(rUsername);
        }
        if (rPassword != null && !rPassword.isBlank()) {
            configBuilder.password(rPassword);
        }
        if (rDatabaseName != null && !rDatabaseName.isBlank()) {
            configBuilder.dbName(rDatabaseName);
        }

        return new MilvusClientV2(configBuilder.build());
    }

    protected static JsonObject toJsonObject(Map<String, Object> map) {
        return GSON.toJsonTree(map).getAsJsonObject();
    }

    protected static Map<String, Object> fromJsonObject(JsonObject jsonObject) {
        return GSON.fromJson(jsonObject, Map.class);
    }
}
