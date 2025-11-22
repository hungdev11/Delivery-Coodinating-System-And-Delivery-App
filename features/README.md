## Features Index

This folder documents end-to-end features for every user persona. Each sub-folder keeps a focused README that lists user journeys, required APIs/services, and an Activity UML that can be regenerated from code.

- `admin/` – Web management console flows (operations, monitoring, confirmation).
- `shipper/` – Android Delivery App flows (sessions, scanning, navigation, chat).
- `client/` – Customer-facing flows (parcel create, tracking, confirmation & disputes).

### How to maintain these docs

1. **Sync with code**: when a feature is implemented, update the matching persona README with the latest API entry points, store modules, and screen/view references.
2. **Link to specs**: reference the source spec or report page (`reports/` folder) so every Activity diagram has narrative context.
3. **Version tags**: use `v0`, `v1`, `v2` prefixes inside each README to show which releases support the flow.

### Activity UML workflow

All diagrams in this folder use Mermaid Activity diagrams for quick editing, but you can switch to PlantUML or Structurizr if you prefer code-to-UML pipelines.

- **Mermaid CLI**:  
  ```bash
  npx @mermaid-js/mermaid-cli -i features/admin/README.md -o features/admin/diagram.svg
  ```
- **PlantUML** (IntelliJ / VSCode plugins) – copy the `@startuml` snippets from the persona READMEs and render locally or via [PlantUML server](https://www.plantuml.com/plantuml/uml/).
- **Structurizr DSL**: for architecture diagrams that reuse code packages, export the DSL from `reports/overall_package_uml.md` and keep the diagram IDs in sync.

### Recommended code-to-UML tooling

- **IntelliJ IDEA Ultimate**: `Diagram` → `Show Diagram…` on a package/class, then export to PlantUML.
- **VSCode + PlantUML**: leverage the `PlantUML: Export Current Diagram` command and store outputs in `reports/diagrams/`.
- **Mermaid Live Editor**: paste snippets to [mermaid.live](https://mermaid.live/) for quick sharing.

> Each persona README doubles as an index: keep the Activity diagram near the top, then document API tables, store/actions, and related backend services so onboarding new contributors is trivial.
