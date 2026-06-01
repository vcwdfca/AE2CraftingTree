# Legacy Tree Refactor Design

## Goal

Refactor the current branch so the crafting tree logic stays on the current AE2 and Minecraft API surface, while the internal tree semantics, missing-only behavior, layout rules, and navigation track AE2CT-Legacy as closely as practical.

The implementation should not port the 1.12.2 GUI framework wholesale. Current integration points, resources, screens, and rendering APIs remain current-version code.

## References

- NovaEngineering-Source/AE2CT-Legacy: `LiteCraftTreeNode`, `LiteCraftTreeProc`, and `CraftingTree`.
- Circulate233/Applied-Energistics-2-Supergiant commit `2812a9d6dbe02161ec87dd63a90d0633e98a4159`: AE2-integrated Legacy tree extraction.

The Supergiant commit changes the data-source priority. It preserves the real AE2 crafting tree from the calculation phase, carries it through the plan, then converts `CraftingTreeNode` and `CraftingTreeProcess` into lightweight Legacy-style data for the client. This branch should follow that strategy where possible.

## Scope

In scope:

- Add a Legacy-style lightweight tree model using current AE2 types.
- Prefer converting the real AE2 crafting tree over rebuilding from recipe summaries.
- Keep a fallback builder based on the current `RecipeHelper` and `CraftingPlanSummaryEntry` data.
- Replace the current incremental coordinate layout with a Legacy-style row layout and placeholder expansion.
- Preserve current API integration, GUI rendering APIs, search, cache, screenshot entry points, and config switches where they still fit.
- Add tests for data conversion, missing-only filtering, sorting, row layout, and navigation.

Out of scope:

- Porting AE2CT-Legacy's 1.12.2 widget framework.
- Replacing current mod metadata, mixin registration, or resource namespace beyond what the refactor requires.
- Reimplementing AE2's crafting simulation algorithm.

## Architecture

### Lightweight Tree Model

Introduce Legacy-style model classes in `com.vcwdfca.ae2ct.tree`, for example:

- `LegacyTreeNode`
- `LegacyTreeProcess`
- `LegacyTreeData`
- `LegacyTreeRowLayout`
- `LegacyTreePosition`

`LegacyTreeNode` represents one produced/requested stack and contains:

- Parent process reference.
- `GenericStack output`.
- List of `LegacyTreeProcess` inputs.
- Missing amount.
- Optional amounts needed by current tooltips: stored and craft, if available.
- Cached missing-state fields.

`LegacyTreeProcess` represents one pattern/process branch and contains an ordered list of input nodes.

The model should mirror Legacy behavior:

- `sort()` sorts child processes and child nodes by descending recursive depth.
- `isMissing(node)` returns true when the node or any descendant has missing amount.
- `withMissingOnly()` returns a copy that keeps only missing paths and their ancestors.
- `totalNodes()`, `totalProcessors()`, `getRenderExpandNodes()`, and `getLastNodeRenderExpandNodes()` use the same recursive shape as Legacy.

### Real Tree Data Source

The primary path should expose the real AE2 crafting tree from the crafting calculation/plan layer.

Design target:

1. Extend the existing mixin/API surface so the current plan summary can provide the real tree root or a lightweight tree derived from it.
2. Convert the tree using Supergiant's approach:
   - Root output amount is the requested final output amount.
   - For each process, calculate process times by matching the parent node key against the process outputs.
   - `processTimes = ceil(parentAmount / craftedPerPattern)` when the process has a matching output.
   - Child amount is `nodeTemplateAmount * inputMultiplier * processTimes`.
   - Missing amount comes from the calculation's missing counter or node-level accessor.
3. Preserve deterministic input ordering from the underlying AE2 process map when available.

This path is preferred because it reflects AE2's actual selected patterns, substitutions, recursion handling, missing branches, and container-item behavior more accurately than recipe-list reconstruction.

### Fallback Data Source

If the real crafting tree is unavailable, retain a fallback builder based on the current `RecipeHelper` and `CraftingPlanSummaryEntry` inputs.

The fallback should build the same `LegacyTreeData` shape so the GUI, search, missing-only filter, and screenshot code do not care which source produced the data.

Fallback limitations should be explicit in code comments or tests: it estimates tree shape from known recipes and summary entries, so it cannot fully reproduce AE2's selected process tree.

## Layout

Replace the current `DisplayNode`/`LayoutEngine` expansion behavior for the main tree view with Legacy row layout.

Layout rules:

- Recursively add each node to a row based on depth.
- A row contains renderable node entries and placeholder entries.
- When a subtree needs horizontal space, add placeholders to lower rows using the Legacy `getRenderExpandNodes()` rule.
- Track previous and next renderable nodes within each row for horizontal navigation.
- Store node-to-position mappings for tooltips, search, screenshots, and hit detection.

The initial implementation can build the full layout. Viewport clipping can happen during render rather than by lazy expansion. This is closer to Legacy and simpler to verify.

## GUI Behavior

`CraftingTreeWidget` keeps current rendering APIs:

- `GuiGraphics`
- `PoseStack`
- `AEKeyRendering`
- Current tooltip helpers
- Current XEI recipe lookup hooks

Behavior should move toward Legacy:

- Drag pans the tree inside bounded offsets.
- Mouse wheel scales within a Legacy-like range, approximately `0.25F` to `1.0F`.
- Middle click or existing reset action restores offset and scale.
- Missing-only toggles rebuild the active tree from `withMissingOnly()`.
- Left and right navigation move to previous/next renderable node in the row.
- Ctrl+left and Ctrl+right skip non-missing nodes.
- Up and down navigation move to the nearest renderable node at the same or previous column index in adjacent rows.
- Search remains available and expands/focuses the row-layout entry for the current match.

Rendering should keep the current item icon and amount drawing, but line rendering should use the row-layout parent/child positions rather than the old coordinate expansion model.

## Caching And Search

Cache the resulting `LegacyTreeData`, row layout, and search index under the existing plan key where possible.

Search should index `LegacyTreeNode.output().what()` display names. Search results should map directly to layout entries, not to old `GraphNode` instances.

When missing-only mode changes, rebuild the active layout and search index from either the base tree or the missing-only copy.

## Screenshot

Keep the current screenshot entry point, but adapt screenshot traversal to the new row-layout tree. If the existing screenshot helper depends on `DisplayNode`, either provide a small adapter from `LegacyTreeNode` layout entries or refactor the helper to read the new row layout directly.

## Error Handling

If real tree extraction fails:

- Log or surface a concise diagnostic in development-friendly paths.
- Fall back to the recipe-summary builder when enough data is available.
- If both paths fail or the active tree is empty, render the current "not ready" behavior and avoid crashes.

Missing-only mode should no-op when the active tree has no missing paths.

## Testing

Add or update unit tests around the new tree package:

- Conversion computes process times and child amounts like Supergiant.
- Missing-only keeps ancestors of missing descendants and removes non-missing-only branches.
- Sorting puts deeper branches first, matching Legacy's reverse-depth sort.
- Row layout adds placeholders so later rows keep enough horizontal width for expanded subtrees.
- Row navigation moves left/right/up/down to expected nodes and Ctrl horizontal navigation skips non-missing nodes.
- Fallback builder still creates a valid Legacy tree from `RecipeHelper` and summary entries.

Run the Gradle test suite after implementation.

## Migration Plan

1. Introduce the Legacy-style model and tests without changing GUI behavior.
2. Add real-tree extraction/conversion through the existing mixin/API layer, with fallback builder retained.
3. Replace current tree layout with row layout and update rendering/hit detection.
4. Adapt search, cache, missing-only, screenshots, and keyboard navigation.
5. Remove or deprecate old `GraphNode`, `DisplayNode`, `LayoutEngine`, and `TreeBuilder` code only after all consumers are migrated.

## Open Assumptions

- Current AE2 version still has enough internal tree data to expose via mixin/accessor similarly to the Supergiant commit.
- The requested final output amount is available at the point where the lightweight tree is built.
- It is acceptable for the first implementation to build the full row layout before rendering.
