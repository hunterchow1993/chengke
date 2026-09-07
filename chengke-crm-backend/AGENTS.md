# Backend Development Instructions

## Document-first workflow

Before adding or changing backend production code, database migrations, API contracts, DTOs, VOs, Commands, Queries, Mapper methods, permissions, errors, caching, or tests:

1. Read `docs/README.md` and the affected module document under `docs/modules/`.
2. Update the module document first. Record method signatures, field contracts, behavior, errors, database impact, compatibility, and test cases.
3. Add or update the automated test and verify that it fails for the intended reason before changing production code.
4. Implement the smallest change that satisfies the documented contract and makes the test pass.
5. Run all relevant unit, Controller, Mapper/Testcontainers, integration, architecture, OpenAPI, and Flyway checks.
6. Re-read the document and implementation before completion. Resolve every mismatch and update the document change log.

Do not implement undocumented behavior. If the requested behavior conflicts with an existing module document, update and review the document before editing production code.

## Department management

The implementation contract is `docs/modules/department-management-backend-development-v0.1.md`. Department work must preserve its Controller/Application/Domain/Infrastructure boundaries, Request DTO → Command/Query → Domain/DO → Application Result → Response VO separation, stable error codes, tenant filtering, optimistic locking, and test matrix.

## Commenting rules

- All new or modified public Java classes and methods require Javadoc describing responsibility, parameters, return values, exceptions, and side effects.
- Controller methods must document permissions and HTTP behavior.
- Application command methods must document transaction, audit, cache, and authorization-version side effects.
- A private method may omit Javadoc only when it is fewer than 5 lines and completely self-explanatory.
