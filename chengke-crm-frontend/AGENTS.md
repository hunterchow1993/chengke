# Frontend Development Instructions

## Component and page documentation gate

Before creating or modifying any `.tsx` page, component, layout, route boundary, or Provider:

1. Read `docs/README.md` and `docs/component-page-registry.md`.
2. Find the source file's development document. For a new file, add the registry entry and create its document record before implementation.
3. Update responsibilities, props/callbacks/children, state, side effects, API/cache/router/global-state behavior, permissions, accessibility, responsive behavior, and test cases first.
4. Update the document change log.
5. Add or update the automated test and verify it fails for the intended reason before changing production code.
6. Implement the smallest documented change, then run format, lint, typecheck, tests, build, and relevant Playwright/visual checks.
7. Re-read the document and source before completion; resolve every mismatch.

No undocumented `.tsx` file may be added. A module-level document may cover multiple related components only when every source file and exported component has a dedicated record. Test files do not need separate registry entries, but their cases must be listed in the component/page document.

## Architecture boundaries

- Pages compose features; reusable business logic belongs in hooks/services, not large page components.
- Server state belongs in TanStack Query; session and authorization snapshots belong in Redux; local interaction state stays local.
- API DTOs are adapted into page models. Generated OpenAPI files are never edited manually.
- Frontend permission gates improve UX and never replace backend authorization.
- Feature internals are imported through the feature public entry when crossing feature boundaries.

## Commenting rules

- All new or modified components, functions, and methods require comments unless they are fewer than 5 lines and completely self-explanatory.
- Component comments must explain responsibility, props/callbacks/children, important state, and side effects.
- Functions and methods use JSDoc describing parameters, return values, and side effects.
- API requests, router navigation, cache writes, storage access, and global state changes must be explicitly documented in code and in the development document.
