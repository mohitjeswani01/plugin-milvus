<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

# Kestra Milvus Plugin

## Why

- Teams building RAG pipelines and semantic search workflows need to manage [Milvus](https://milvus.io/) vector collections and run vector operations as part of a Kestra flow, alongside the rest of their data pipeline.
- Without a dedicated plugin, this requires custom HTTP or script tasks to call the Milvus SDK directly, duplicated across every flow that needs it.
- This plugin lets a flow create collections, upsert embeddings, run similarity search, and delete rows against a self-hosted Milvus instance or Zilliz Cloud, using the same `Property<T>` and `FetchType` conventions as the rest of Kestra's plugin ecosystem.

## What

Provides plugin components under `io.kestra.plugin.milvus`:

- `CreateCollection` — creates a collection with a single vector field and a primary key field, using Milvus's quick-setup schema.
- `Upsert` — inserts or updates rows in configurable batch sizes, from an inline list or a Kestra internal storage URI.
- `Search` — runs an approximate nearest-neighbor vector similarity search, optionally narrowed with a scalar filter expression.
- `Delete` — deletes rows by primary key IDs or by a scalar filter expression.
- `DropCollection` — permanently deletes a collection and all of its rows.

## Running Kestra locally with this plugin

1. Build the shadow JAR: `./gradlew shadowJar`. The output lands in `build/libs/`.
2. Run `docker compose up`. `docker-compose.yml` builds `kestra/kestra:latest` and mounts `build/libs/` to `/app/plugins/`, so Kestra picks up the jar on startup.
3. Kestra UI is available at [localhost:8080](http://localhost:8080).

### Plugins folder gotcha

Mounting a host folder onto `/app/plugins/` replaces the container's plugins directory rather than adding to it. Core plugins (the ones logged as `Registered N core plugins`) are compiled into Kestra itself and aren't affected, but any additional plugin normally bundled in the base image under `/app/plugins/` gets hidden once the mount is in place. If a flow you're testing depends on another plugin, copy its jar into `build/libs/` too before starting the container.

### JFR startup error

On some hosts, `command: server local` fails with:
```
Unable to create JFR repository directory using base location (/tmp)
```
`docker-compose.yml` works around this by mounting `/tmp` as `tmpfs`. If you build your own compose file or run Kestra via `docker run`, add the same workaround, e.g. `-v /tmp:/tmp` or `--tmpfs /tmp`. Tracked upstream in [kestra-io/kestra#17405](https://github.com/kestra-io/kestra/issues/17405).

## Running the tests

Tests use [Testcontainers](https://testcontainers.com/) to spin up a real `milvusdb/milvus` container — Docker must be running locally:

```bash
./gradlew test
```

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)


## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
