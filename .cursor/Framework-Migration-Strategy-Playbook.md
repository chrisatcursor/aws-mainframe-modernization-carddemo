# Framework Migration Strategy Playbook

**Discovery, Architecture, and Agent-Assisted Automation**

Prepared for Customer Engagement · Cursor Field Engineering · March 2026

---

## Contents

1. [Discovery Questions](#1-discovery-questions)
2. [High-Level Principles](#2-high-level-principles)
3. [Recommended Patterns: Strangler Fig Migration](#3-recommended-patterns-strangler-fig-migration)
4. [Full Rewrite Guidance](#4-full-rewrite-guidance)
5. [Automating with Cursor](#5-automating-with-cursor)

---

## 1. Discovery Questions

Before recommending an approach, you need to understand the full picture. These questions apply to any framework migration — frontend or backend, monolith to microservices, legacy to modern. Adapt the specifics to the customer's stack, but the categories are universal. Field Engineering is here to help provide guidance to our customers performing these migrations, not deliver the migration. We help them answer questions and give them a process to follow.

### Understanding the Codebase

- **How large is the codebase?** Get a module count, lines of code, and number of entry points or routes. A larger codebase will require more decomposition in planning.

- **How many source frameworks or languages are involved?** Is it a single legacy framework or a mix? Are they cleanly separated (different apps, modules, or repos) or intermingled in a shared build pipeline? Intermingling increases extraction difficulty.

- **What does the dependency graph look like?** How many shared services, utilities, and state stores exist? Deep coupling will require deeper and more specific scoping of tasks.

- **What is the state management and data layer story?** Global stores, service layers, ORMs, caching — how many, and how tightly are modules coupled to them? This is often the hardest part of any migration.

- **Is there an existing test suite?** What coverage looks like, what frameworks are in use, and how much of it is behavioral vs. implementation-detail testing? If coverage is near zero, the "accumulated knowledge in the codebase" argument weakens significantly.

> **Protip: Use Subagents**
>
> Inventorying and understanding a large codebase is many tokens. Define subagents that specialize in exploring and recording inventory, and noting architecture. When used, they pass their findings back to the parent agent, without creating context noise in the parent agent's context window.

### Understanding the Team

- **Who is doing the migration?** What is the team's experience with the target framework? Are they currently maintaining the legacy system, or is this a separate team? A team with no legacy expertise will struggle with a strangler pattern.

- **What is the team's capacity?** Can they afford a feature freeze during a rewrite, or does the business require continuous feature delivery? This is often the single biggest constraint.

- **Is there institutional knowledge of the legacy codebase?** If the original authors have left and there is no documentation, reverse-engineering behavior is required regardless of strategy.

### Understanding the Business Context

- **What is the timeline pressure?** Is there a hard deadline (compliance, EOL dependency, contract)? Rewrites are high-variance on timeline; strangler patterns are more predictable.

- **What is the tolerance for risk?** A failed rewrite means months of work with nothing to show. A strangler pattern always has a working product. How does leadership feel about that tradeoff?

- **Are product requirements changing?** If the product roadmap looks significantly different from the current system, you may not be "migrating" so much as building a new product. That changes the calculus.

- **What is the deployment and infrastructure story?** Infrastructure that supports independent deployments (microfrontends, microservices, service mesh) makes a strangler pattern easier. A monolithic deploy pipeline makes it harder.

> **Key Signal to Listen For**
>
> If the customer says they want to "start clean" but can't articulate specific technical reasons why incremental migration won't work, that's an emotional decision, not a technical one. Push back gently. If they have concrete reasons (deeply coupled codebases, no tests, codebase bloat, fundamentally different product requirements), the rewrite may be the right call.

---

## 2. High-Level Principles

Regardless of whether the team chooses a strangler pattern or a full rewrite, these principles apply to every framework migration.

### Tests Are the Migration Harness

Behavioral tests written before the migration serve two purposes: they document current behavior so you know the migration is correct, and they become the regression suite for the new codebase. Without tests, you are flying blind on whether the new system is feature-complete. This is non-negotiable for either approach.

### Agents Accelerate Mechanical Work, Not Architectural Decisions

AI agents excel at bounded, repetitive tasks: converting templates between frameworks, mapping lifecycle patterns, porting test syntax, renaming APIs. They struggle with cross-cutting state redesign, security-sensitive flows, and UX semantics. Encode your architectural decisions in rules and plan files and let agents execute within those guardrails.

### The State and Data Layer Is the Hard Part

Global stores, service layers, data access patterns, and shared state are where the real complexity lives. Module conversion is relatively mechanical. Data architecture decisions (what patterns to use in the target framework, how to decompose monolithic state, where to draw boundaries) require human judgment and should be made before any agent work begins.

### Encode Conventions Early and Explicitly

Define your target architecture before converting a single module: approved patterns, state management approach, styling conventions, type strictness, accessibility standards. Put these in a rules file (`.cursorrules` or `.cursor/rules`) so every agent run and every developer follows the same patterns. This is the single highest-leverage thing the team can do.

### Incremental Value Beats Big-Bang Delivery

Whether you strangle or rewrite, structure the work so you can demonstrate progress and catch problems early. Batch by module, migrate leaf units before complex containers, and validate each batch before moving on. The teams that get into trouble are the ones that try to do everything at once and only discover problems at integration time.

---

## 3. Recommended Patterns: Strangler Fig Migration

The strangler fig approach is the lower-risk path for most teams. You incrementally replace pieces of the old system with modules written in the target framework while keeping the existing system running. You ship value continuously, course-correct as you learn, and never have a big-bang cutover moment.

### Phase 1: Discovery and Classification

Run a discovery agent across the codebase to build a complete inventory: every module, route, service, store, and integration point. Classify each item into migration buckets:

- **Leaf units** — presentational components, pure utility functions, stateless modules with few dependencies. Ideal starting point for automated conversion.

- **Simple containers** — modules with local state and a few external connections. Graduate to these after leaf units are validated.

- **Complex workflows** — business-critical flows, deeply coupled state, security-sensitive paths. Require human-led migration.

- **Shared infrastructure** — state stores, routing, auth, API layers, data access. Migrate deliberately with human architecture decisions.

The output is a prioritized migration backlog with t-shirt-sized complexity scores and dependency ordering.

### Phase 2: Coexistence Setup

Before migrating any modules, set up the integration layer that allows old and new to coexist. The mechanism depends on your stack: microfrontend wrappers, module federation, adapter patterns, API gateways, or service mesh routing. Feature flags or beta routes let you shift traffic between implementations gradually.

### Phase 3: TDD-Driven Migration Loop

For each unit in the backlog:

1. **Write behavioral tests first.** Describe what the system does from the outside: inputs, outputs, side effects, user-visible behavior. Write them in the target framework's test tooling.

2. **Run the migration agent.** It takes the existing code, the tests, and the rules file, and produces the target-framework equivalent. It iterates until tests pass.

3. **Modernization pass.** A second agent reviews the output: are patterns idiomatic? Are there opportunities to improve on the legacy implementation?

4. **Open a PR.** Each migrated unit is a separate, reviewable PR. No feature changes mixed in.

### Phase 4: Batch Orchestration via Cloud Agent API

Once success rates are consistent and patterns are stable, then scale the Phase 3 loop using the cloud agent API. Each unit becomes an independent, parallelizable piece of work. Batch by dependency layer. The team's job shifts from writing code to reviewing PRs and calibrating agent quality.

Track metrics per batch: regression rate, review time, defect density. Use these to adjust which complexity buckets you trust the agent with.

### Phase 5: Progressive Rollout

Use feature flags to gradually shift traffic to new implementations. Monitor performance, error rates, and user behavior. Roll back instantly if issues surface. Once validated, remove legacy code and the coexistence wrapper.

---

## 4. Full Rewrite Guidance

If the customer is committed to a ground-up rewrite, the goal shifts from "how to migrate incrementally" to "how to make the rewrite as safe as possible."

### When a Rewrite Genuinely Makes Sense

A full rewrite is defensible when one or more of these conditions hold:

- The existing codebase is small enough that the rewrite is an S or M-sized effort.
- Legacy frameworks are so deeply intermingled that incremental extraction is genuinely harder than starting fresh.
- Product requirements have changed so dramatically that you are building a different product, not migrating an existing one.
- The team has zero legacy-framework expertise and maintaining the old system during a strangler migration would be its own burden.
- The existing system has no tests and minimal documentation — you are reverse-engineering behavior either way.

If none of these resonate with the customer, push back. If one or more hit home, the rewrite may be the right call.

### Critical Recommendations for a Full Rewrite

**Do not skip the discovery phase.** The inventory becomes your requirements spec. You need to know every route, every module, every integration point — because you will not have the old code to fall back on.

**Write behavioral tests against the existing system before writing a single line in the target framework.** These become your acceptance criteria. Without them, you have no objective measure of feature parity.

**Timebox the rewrite aggressively.** Define MVP parity. Every feature not in the existing system goes on a post-migration backlog. The second-system effect has killed more rewrites than technical complexity.

**Plan for the feature freeze tax.** If the existing system stays live, someone has to maintain it. If features ship on the old system, someone has to port them. Budget for this explicitly.

**Use agents for mechanical translation, keep humans on architecture.** Discovery agents build the inventory, migration agents convert in batch, TDD agents generate tests. But system design decisions must be human choices encoded in rules files.

**Define a hard cutover plan.** Plan it, rehearse it, have a rollback path. Parallel-run both systems with traffic mirroring if possible.

> **The Bottom Line for Full Rewrites**
>
> A rewrite is a bet that you can rebuild faster than you can untangle. Make sure the team understands they are trading incremental safety for speed-to-clean-slate. Agents make that bet more favorable by compressing mechanical conversion, but they don't eliminate architectural risk. The discovery phase, behavioral test suite, and aggressive timeboxing are what keep the rewrite from becoming the long project nobody wanted.

Cursor's value proposition in either scenario is the same: shift developer time from mechanical conversion to architectural decisions and code review.

---

## 5. Automating with Cursor

This section defines a concrete automation strategy using Cursor's cloud agents and subagent architecture. The idea is to decompose the migration into a master plan, break it into subplans, and then execute those subplans in parallel using specialized subagents — each with a focused role, its own context, and clear input/output contracts.

> **Prerequisite: Cloud Agent Environment**
>
> Before any of this works, the team needs to set up a Cloud Agent environment for this repository. That means enabling cloud agents in Cursor settings, connecting the repo so cloud sessions can access the codebase, and configuring any required secrets or environment variables (API keys, test database credentials, CI tokens) that agents will need to run tests and open PRs. Get this infrastructure in place first — it's a one-time setup, but nothing downstream runs without it. See the Cursor documentation for the full setup guide.

### The Master Plan / Subplan Architecture

Start in Cursor's Plan Mode to create a high-level master plan for the entire migration. This plan covers every phase and defines sequencing constraints. The master plan is not what agents execute directly — it's the orchestration layer that gets decomposed into subplans.

Each subplan is a self-contained unit of work for a cloud agent session: clear scope, rules file references, and acceptance criteria. Subplans may need to be broken down further into child subplans. The goal is to decompose work until each plan is scoped to what an agent can handle in a single session. A good rule of thumb: if a developer could complete the task in roughly a day, it's the right size. Use t-shirt sizing (S/M/L/XL) to estimate complexity — an L-sized subplan should probably be split into M-sized child subplans before handing it to an agent.

> **Why Subagents Matter**
>
> A single agent session trying to inventory a large codebase will run into context window limits and lose coherence. Subagents specialize: one explores the auth module, another inventories the dashboard, a third catalogs shared services. Each passes structured findings back to the parent without polluting its context. This is how you scale agent work to large codebases.

### Subagent Definitions

Define subagents in `.cursor/agents/` as markdown files with YAML frontmatter. Each file gives the agent a name, a specific description, and a system prompt with instructions. See the Cursor subagents documentation for the full syntax. Below are the recommended subagents for a migration project.

#### Inventory and Discovery Agent

Crawls a defined scope of the codebase and produces a structured inventory of every module, service, store, and integration point. Classifies each item by type (leaf unit, simple container, complex workflow, shared infra) and assigns a t-shirt size based on dependency count, coupling, and complexity. Flags risks like deprecated APIs, tight coupling to legacy patterns, or missing tests. Run multiple instances in parallel, one per top-level module, and merge findings into the master backlog.

*Example description: "Codebase inventory specialist. Use when you need to crawl a module and produce a classified inventory with t-shirt sizing."*

#### Test Generation Agent

For a given module, writes behavioral tests in the target framework's test tooling that describe current behavior: what the system does from the outside, what inputs drive outputs, and what interactions trigger effects. Tests assert on observable behavior, not implementation details. If the module has no existing tests and unclear behavior, the agent flags it for human review rather than guessing.

*Example description: "Behavioral test writer. Use when generating tests that capture current behavior before migration."*

#### Migration Agent

The workhorse. Converts a legacy module into its target-framework equivalent following the architecture defined in the project's rules files. Takes the original source, behavioral tests, and rules file as input. Runs tests and iterates until green. If unable to achieve green after a defined iteration limit, produces a failure report and flags for human review. Most migration volume runs through this agent.

*Example description: "Module migration workhorse. Use when converting a legacy module to the target framework. Iterates until tests pass."*

#### Modernization Agent

Reviews migrated code against a modernization checklist and refactors patterns that are technically correct but not idiomatic for the target framework. Checks for outdated patterns, missing accessibility or performance best practices, design system compliance, and improvement opportunities. Produces a changelog. Can also run standalone against existing code that predates current conventions.

*Example description: "Modernization reviewer. Use after migration to refactor non-idiomatic patterns into target-framework best practices."*

#### Security Scanning Agent

Audits migrated code for security concerns: injection vectors, auth flow integrity, access control enforcement, sensitive data exposure, and dependency vulnerabilities. Categorizes findings by severity with references and remediation suggestions. Flags but does not auto-fix — all findings require human review.

*Example description: "Security auditor. Use after migration and modernization to check for injection vectors, auth integrity, data exposure, and dependency vulnerabilities."*

#### Feature Flag Management Agent

Generates or updates feature flag configurations for toggling between legacy and new implementations. Handles the full flag lifecycle: creation, rollout stage definitions, integration code for the coexistence layer, and a cleanup checklist for removing flags post-migration. Also audits for stale flags from previous batches.

*Example description: "Feature flag lifecycle manager. Use when generating or updating flag configs for toggling between legacy and new implementations."*

### Skills: Reusable Knowledge for All Agents

Not everything needs a dedicated subagent. Skills are reusable instructions defined in `.cursor/rules/` or as `SKILL.md` files that any agent can access. Use a skill when the task is general enough that multiple agents benefit from following the same procedure, or when the work is repeatable but doesn't require isolated context. Skills keep agents consistent without burning a separate session on what is essentially a shared checklist.

**PR Composition Skill** is a good example. Every subagent that produces code needs to package it as a reviewable PR, but the format should be consistent. Define a skill that specifies: PR title format, required description sections (summary, modernization notes, security status, test results, backlog link), atomicity rules, and a completion checklist.

Other useful skills: a **migration cookbook** mapping source-framework idioms to target-framework equivalents, a **testing conventions** skill enforcing your test patterns and coverage expectations, and a **commit message standard** skill for readable git history across agent-generated commits.

### Orchestration Models

Once subagents are defined and the master plan is decomposed, you need a way to launch, track, and coordinate. Choose based on team maturity:

**Model A: Manual Launch from Cursor.** Developer picks a subplan, launches a cloud agent session. High-control, good for calibrating quality on the first batch.

**Model B: Task Tracker Integration (Linear, Jira, etc.).** Subplans as tickets with structured fields. Agent sessions reference ticket descriptions. Full traceability, integrates with sprint workflows.

**Model C: Batch Orchestration via API.** Launch sessions programmatically. A script iterates through the backlog, groups by dependency layer, fires off agent sessions in batches. Results flow back as PRs.

**Model D: Hybrid Pipeline.** Combine B and C. Backlog in Linear/Jira, automation layer launches agents via API on "ready" tickets, output creates draft PRs. Most production-grade approach.

> **Choosing Your Starting Point**
>
> Start with Model A for the first S and M-sized modules. Once success rates are consistent and patterns are stable, move to Model B or C. Teams can reach Model D if they invest in good rules files and structured subplans upfront.

### Execution Flow: End to End

A typical migration batch:

1. **Plan.** Use Plan Mode to select the next batch, decompose into S/M-sized subplans. Specify scope, subagent sequence (inventory → test gen → migrate → modernize → security scan), rules files, and acceptance criteria.

2. **Discover.** Launch Inventory Agents in parallel (one per module). Merge into the backlog. Flag modules needing reclassification.

3. **Generate tests.** Launch Test Generation Agents. Human spot-checks a sample before proceeding.

4. **Migrate and modernize.** Launch Migration Agents with tests. Follow with Modernization Agents. Failures route back for human attention.

5. **Scan and package.** Run Security Scanning Agents. Final agent invokes the PR Composition Skill to package reviewable PRs.

6. **Review and ship.** Team reviews PRs. Feature Flag Agents generate toggle config. Merge, deploy behind flags, monitor, roll out.

7. **Retrospect.** Track metrics. Feed learnings back into rules, subagent prompts, and the master plan. Adjust scope for the next batch.

> **The Feedback Loop Is Everything**
>
> Every batch should produce updates to three things: **rules** (new patterns, anti-patterns, edge cases), **subagent prompts** (sharpen instructions based on where agents went off track), and **plans** (re-scope based on actual vs. estimated complexity, update sequencing as dependencies become clearer). The team that treats rules, agents, and plans as living documents will see quality improve with every batch. The team that writes them once and forgets will hit a ceiling fast.

---

The end state is a team that spends its time on architectural decisions, code review, and product work — while mechanical conversion, test generation, modernization enforcement, and security scanning happen in parallel across cloud agent sessions. That's the Cursor value proposition for migrations at scale.
