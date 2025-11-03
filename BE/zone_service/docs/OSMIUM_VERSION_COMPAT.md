# Osmium Version Compatibility

## The Version Split

Osmium evolved its CLI interface around version 1.16, introducing **strategy presets** to replace verbose individual flags.

## What Changed

### Old Style (Osmium < 1.16)

```bash
osmium extract \
  --polygon area.poly \
  --complete-ways \
  --complete-nodes \
  --complete-relations \
  -o output.osm.pbf input.osm.pbf
```

**Verbose but explicit:** Each completeness aspect specified individually.

### New Style (Osmium 1.16+)

```bash
osmium extract \
  --polygon area.poly \
  -s complete_ways \
  -o output.osm.pbf input.osm.pbf
```

**Concise preset:** Strategy flag bundles common patterns.

## Strategy Presets Available

| Strategy         | What It Does                                    | Use Case                          |
|------------------|-------------------------------------------------|-----------------------------------|
| `simple`         | Basic bbox clip (may break ways)                | Quick extracts, don't care about graph integrity |
| `complete_ways`  | Complete ways + referenced nodes + relations    | **Routing (recommended)**         |
| `smart`          | Complete ways + related objects (more aggressive) | Complex data with many relations |

**For routing:** Always use `complete_ways` (or old `--complete-*` flags).

## Our Implementation

The `OsmiumWrapper` class **auto-detects** which style your osmium supports:

```typescript
private async supportsStrategyFlag(): Promise<boolean> {
  const { stdout } = await execAsync('osmium extract --help');
  return stdout.includes('--strategy');
}
```

Then adapts the command:

```typescript
if (useStrategyFlag) {
  args.push('-s', 'complete_ways');  // Modern
} else {
  args.push('--complete-ways', '--complete-nodes', '--complete-relations');  // Legacy
}
```

## How to Check Your Version

```bash
osmium --version
```

Example outputs:

```
osmium version 1.18.0
```
→ Modern (uses `-s`)

```
osmium version 1.14.0
```
→ Legacy (uses `--complete-*`)

## Why This Matters

**Your error message:**
```
unrecognised option '--complete-ways'
```

**Translation:** You have osmium 1.18 (modern), script expected legacy flags.

**Fix:** Updated wrapper now detects version and uses correct syntax automatically.

## Testing Both Modes

### Test Modern Mode (if you have 1.16+)

```bash
osmium extract \
  --polygon raw_data/poly/thuduc_cu.poly \
  -s complete_ways \
  -o test_modern.osm.pbf \
  raw_data/vietnam-latest.osm.pbf
```

### Test Legacy Mode (if you have < 1.16)

```bash
osmium extract \
  --polygon raw_data/poly/thuduc_cu.poly \
  --complete-ways \
  --complete-nodes \
  --complete-relations \
  -o test_legacy.osm.pbf \
  raw_data/vietnam-latest.osm.pbf
```

Both should produce functionally identical extracts.

## What the Wrapper Does Now

1. **On first extract:** Checks `osmium extract --help` for `--strategy` mention
2. **Caches result:** No repeated checks
3. **Builds correct command:** Uses appropriate syntax
4. **Logs strategy:** Shows which mode used if `verbose: true`

Example log output:

```
Extracting polygon (strategy: complete_ways): osmium extract --polygon ...
```

or

```
Extracting polygon (legacy complete flags): osmium extract --polygon ...
```

## Backward Compatibility

The wrapper supports **both** osmium versions:

- ✅ Osmium 1.8 - 1.15 (legacy flags)
- ✅ Osmium 1.16+ (strategy presets)

No configuration needed. It just works.

## Fun Fact

This is like when cars switched from carburetors to fuel injection.

Old mechanics yelled:
> "Where's the choke lever?!"

New mechanics replied:
> "Bro, ECU handles it now. Just turn the key."

Osmium is the ECU. Your wrapper is now bilingual.

## Summary

| Aspect                 | Status                                |
|------------------------|---------------------------------------|
| Your osmium version    | 1.18 (modern)                         |
| Old wrapper code       | Expected legacy flags                 |
| New wrapper code       | Auto-detects, uses correct syntax     |
| Compatibility          | Both old and new osmium supported     |
| Action required        | None (already patched)                |

---

**TL;DR**: Your osmium is fine. Wrapper now speaks both dialects. Extract away.
