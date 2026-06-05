# Knowledge Tree

Build and maintain a persistent knowledge tree across all projects.
All knowledge is stored under ~/git/llm_knowledge/.

## Structure

```
~/git/llm_knowledge/
├── index.md              # Routes to each domain folder
├── _shared/              # Cross-project knowledge (performance, security, etc.)
│   ├── performance/
│   │   ├── knowledge.md
│   │   ├── hypotheses.md
│   │   └── rules.md
│   ├── security/
│   │   ├── knowledge.md
│   │   ├── hypotheses.md
│   │   └── rules.md
│   └── <topic>/          # Add new shared topics as needed
│       ├── knowledge.md
│       ├── hypotheses.md
│       └── rules.md
└── <project-name>/       # One folder per project (domain = project name)
    ├── knowledge.md      # Confirmed facts, patterns, architecture notes
    ├── hypotheses.md     # Observations that need more data
    └── rules.md          # Confirmed patterns — apply by default
```

Create the structure if it doesn't exist yet.
Create new shared topics under _shared/ when an insight applies across projects
(e.g., performance, security, testing, API design, concurrency).

## Before starting work

1. Identify the domain from the project name (directory name of the repo root).
2. If the domain folder doesn't exist yet, create it and add it to index.md.
3. Read the domain's rules.md and hypotheses.md if they exist.
4. Read relevant _shared/ topic rules.md files (e.g., _shared/performance/rules.md if doing performance-sensitive work).
4. Apply all applicable rules by default.
5. Check if any hypothesis (project or shared) can be tested with today's work.

## After completing a task

Extract insights only when the work revealed something non-obvious:
- A surprising bug cause or fix
- A pattern in the codebase that isn't documented
- A build/test/deploy quirk
- A dependency behavior or API gotcha
- An approach that worked (or failed) unexpectedly

Do NOT extract insights for routine/trivial work (formatting, simple renames, obvious fixes).

When deciding where to store an insight:
- **Project-specific** (e.g., "this project's CI needs X flag") → `<project-name>/`
- **Applies across projects** (e.g., "JVM GC tuning for batch workloads") → `_shared/<topic>/`
- If unsure, store in the project domain. Promote to _shared/ if it recurs in a different project.

## File formats

### knowledge.md
```markdown
## <topic>
<what you learned, with enough context to be useful months later>
Observed: <date>
```

### hypotheses.md
```markdown
## <observation>
<what you noticed and why it might matter>
Status: unconfirmed
Confirmations: 0
First observed: <date>
Last tested: <date>
```

### rules.md
```markdown
## <rule name>
<what to do and when>
Promoted from hypothesis: <date>
Confirmations: <count>
```

## Lifecycle

- **New insight** → add to knowledge.md or hypotheses.md depending on confidence.
- **Hypothesis confirmed** → increment its confirmation count and update last tested date.
- **Hypothesis reaches 3+ confirmations** → promote to rules.md, remove from hypotheses.md.
- **Rule contradicted by new evidence** → demote back to hypotheses.md, reset confirmation count to 0, note the contradiction.
- **Knowledge proven wrong** → remove or correct the entry.

## index.md

Maintain ~/git/llm_knowledge/index.md as a directory of all domains:
```markdown
# Knowledge Tree Index

## Shared
- [performance](./_shared/performance/) — Cross-project performance patterns
- [security](./_shared/security/) — Cross-project security patterns

## Projects
- [<project-name>](./<project-name>/) — <one-line description of the project>
```
