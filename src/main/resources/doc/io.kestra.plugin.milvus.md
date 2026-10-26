# Milvus Plugin

Use the Milvus plugin to manage vector collections, upsert embeddings, run similarity search, and delete rows in [Milvus](https://milvus.io/), an open-source vector database, or [Zilliz Cloud](https://zilliz.com/).

## Connection

All tasks inherit common connection properties:
- `uri`: The URI of the Milvus server or Zilliz Cloud endpoint, e.g. `http://localhost:19530` (required).
- `token`: The API token for authentication, typically used with Zilliz Cloud or a Milvus instance with token-based auth enabled (optional, secret).
- `username` / `password`: Credentials for a self-hosted Milvus instance with username/password authentication enabled (optional, `password` is secret).
- `databaseName`: The Milvus database to operate against. Defaults to the server's default database when not set.

## Tasks

- `CreateCollection`: Creates a collection with a single vector field and a primary key field, using Milvus's quick-setup schema (`dimension`, `metricType`, `primaryFieldName`, `idType`, `autoId`, `vectorFieldName`). For a custom multi-field schema, create the collection with the Milvus SDK or console first and use the other tasks against it directly.
- `Upsert`: Inserts or updates rows in configurable batch sizes (`batchSize`). Each row is a flat map with the collection's vector field, primary key field (unless auto-generated), and any scalar fields. Reads from an inline list or a Kestra internal storage URI.
- `Search`: Runs an approximate nearest-neighbor vector similarity search (`vector`, `limit`), optionally narrowed with a scalar filter expression (`filter`). Outputs results inline (`FETCH`), as a single record (`FETCH_ONE`), or to Kestra internal storage (`STORE`).
- `Delete`: Deletes rows by a list of primary key IDs (`ids`) or by a scalar filter expression (`filter`). Exactly one of the two must be set.
- `DropCollection`: Permanently deletes a collection and all of its rows. This cannot be undone.
