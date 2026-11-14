# Contributing

Read [Architecture](docs/ARCHITECTURE.md) before changing a boundary. Keep domain pure Kotlin, inject ports into use cases/ViewModels, and render immutable UI state in Compose. Android entry points are compatibility contracts with installed devices; preserve names and persisted keys unless a migration is included.

Describe acceptance criteria in a specification before a material feature change. Keep fixes scoped and preserve cached data on failure. Add tests for behavior and regressions, not private implementation details. Run domain tests, relevant instrumentation, lint and formatting checks described in [Building](docs/BUILDING.md).

Do not commit credentials, keystores, personal HTML, emulator data, private endpoints or device reports. Use environment variables for optional build credentials. Demo pages must work without secrets. Include screenshots made with public example content only.
