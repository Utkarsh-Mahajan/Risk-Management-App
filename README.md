# Risk Register

A small full-stack Risk Register — Spring Boot API with a React/TypeScript frontend.
---

## Setup and run

**Prerequisites:** Java 21+, Maven 3.9+, Node 18+.

**Backend**

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. An H2 file-based database is created at `./data/riskregister` on first run and seeded with eight sample risks covering all four severity bands, including one closed unmitigated risk so the interesting case is visible immediately. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/riskregister`, username `sa`, no password).

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

The UI starts on `http://localhost:5173` and proxies `/api` requests to the backend.

---

## Running the tests

**Backend** (unit + integration, ~4 seconds):

```bash
cd backend && mvn test
```

**Frontend** (Vitest, pure logic):

```bash
cd frontend && npm test
```

---

## The residual-risk formula and why

I model mitigating controls as **layered defenses**: each one reduces what the previous ones left behind, not the original inherent score.

```
reduction(effectiveness):  1 → 10%,  2 → 20%,  3 → 30%,  4 → 40%,  5 → 50%

factor   = Π (1 − reduction(eᵢ))   for every mitigation attached to the risk
factor   = max(factor, 0.01)        so total reduction never exceeds 99%
residual = max(1, ⌈inherent × factor⌉)
```

A control with effectiveness 5 passes 50% of the risk through. A second eff-5 control takes 50% of *that* — 25% of original. The diminishing returns are structural, not bolted on: a second control acts on the residual the first one left, not on the full original.

Worked example — inherent 20 (L4 × I5, Critical):

| Controls | Factor | Residual | Band |
|---|---|---|---|
| none | 1.00 | 20 | Critical |
| eff 5 | 0.50 | 10 | Medium |
| eff 5 × 2 | 0.25 | 5 | Low |
| eff 5 × 5 | 0.031 | **1** (floor) | Low |

I chose this formula for a few reasons. A "max effectiveness wins" approach would silently do nothing when you add a second control, breaking the requirement that residual updates after a mitigation is added. An average-of-effectiveness approach would *raise* residual if you attached a weak control to a risk that already had a strong one, which would create a perverse incentive to hide real controls. The multiplicative model has neither problem: adding any control can only hold or lower residual, and the order you add them in doesn't matter because multiplication commutes.

Rounding happens once — at the final `⌈inherent × factor⌉` step — not after each control. Rounding per-control makes the result depend on insertion order; rounding once at the end is what makes order-independence hold.

I use `BigDecimal` arithmetic throughout the scoring service. Products of multiples of 0.1 are exact in `BigDecimal` (not in `double`), so results are deterministic and the unit tests can assert exact integers without floating-point fuzz.

The floor of 1 is load-bearing, not decorative. A risk register that can display a residual of 0 would imply a risk was eliminated, which no realistic control set can claim. The 99% cap (factor floor 0.01) is the explicit guard behind it; the `max(1, …)` is what actually binds in practice on the 1–25 scale.

---

## Risk status flow

Three statuses, each a claim about the risk that the system enforces rather than just displays:

- **Open** — identified, no controls attached yet. Nobody has started addressing it.
- **Mitigating** — at least one control attached, risk not yet closed. Work is in progress.
- **Closed** — no longer active. Either mitigated to an accepted level, or explicitly accepted/transferred/retired via a closure reason.

Status is never a scoring input — inherent and residual are always `likelihood × impact` and the mitigation-effectiveness formula, regardless of status. Closing or reopening a risk never changes its score.

Rules enforced by `RiskStatusTransitionRule`, all returning a 4xx with a `code` field the frontend reads:

| Transition / action | Rule | Error code |
|---|---|---|
| Add a mitigation to an `Open` risk | auto-promotes to `Mitigating` | — |
| Add a mitigation to a `Mitigating` or `Closed` risk | `Mitigating` stays as-is; `Closed` is rejected outright (see below) | — |
| Set status to `Open` while mitigations exist | rejected — `Open` means zero controls, so this would be a lie | 409 `mitigations_exist` |
| Add a mitigation to a `Closed` risk | rejected — a closed record is settled; reopen first | 409 `risk_closed` |
| Delete the last mitigation of a `MITIGATED`-closed risk | rejected — would leave the closure claim unbacked | 409 `closure_basis_removed` |
| Close a risk with zero mitigations, no `closureReason` | rejected — must say why | 422 `closure_reason_required` |
| Close a risk with zero mitigations, reason `MITIGATED` | rejected — can't claim mitigated with nothing on record | 422 `closure_reason_invalid` |
| Close a risk with zero mitigations, no `closureJustification` | rejected — must explain the reason | 422 `closure_justification_required` |
| Close a risk that already has mitigations, no reason given | defaults `closureReason` to `MITIGATED` | — |
| Reopen a `Closed` risk | clears `closureReason`/`closureJustification`/`closedAt`, lands on whatever status was requested (`Open` or `Mitigating`) — subject to the `Open`-requires-no-mitigations rule above | — |

---

## Closing a risk with no mitigations

I allow it, but refuse an unexplained close. A hard block is the wrong call. Legitimate unmitigated closures happen — a risk transferred to an insurer, a system decommissioned, a duplicate entry — and blocking those would push users to invent placeholder mitigations just to escape the validator. Fake effectiveness data poisons the entire residual calculation, which is a worse integrity problem than the one the block was meant to prevent.

The rule I landed on: if a risk has no mitigations and you want to close it, you must supply a `closureReason` (one of `RISK_ACCEPTED`, `TRANSFERRED`, `NO_LONGER_APPLICABLE`, `DUPLICATE`) and a written `closureJustification`. You cannot use `MITIGATED` as the reason — that claim requires at least one control on the record. If the risk already has mitigations, the reason defaults to `MITIGATED` and justification is optional.

The other half of this rule is what happens *after* closure: **closing a risk never changes its score**. Status is not a scoring input. A Critical 20 closed as `RISK_ACCEPTED` stays at residual 20 and Critical. This is the anti-gaming property — if closure zeroed the score, the fastest path to a clean risk register would be to close everything. Instead the register shows an auditor the truth: this organization consciously accepted a Critical risk, here is who said so and why. The frontend makes this visible: closed unmitigated risks at High or Critical display a warning banner and are tagged "Accepted — unmitigated" in the list.

There is also a guard on deleting mitigations: you cannot delete the last control from a risk that is closed as `MITIGATED`. That would leave a risk in a state that claims to be mitigated with nothing backing that claim. The API returns 409 `closure_basis_removed` and asks you to reopen or change the closure reason first.

The same closed-record protection applies in the other direction: you cannot add a mitigation to a closed risk at all — the API returns 409 `risk_closed` and asks you to reopen first. A closed risk is a settled record: its closure reason and residual score are what the organization is standing behind. Silently letting a new control land on a closed record would either recompute the score of something marked "resolved" with no re-review, or — worse — contradict a non-`MITIGATED` closure reason like `RISK_ACCEPTED` (which asserts "we accepted this without controls") the moment a control appears on it. Reopening first forces a deliberate decision: the risk becomes `Open`, gets its new control (which promotes it to `Mitigating`), and a human has to actively re-close it and re-affirm the closure reason.

---

## Assumptions and trade-offs

**H2 file-based database instead of PostgreSQL.** The spec says in-memory or SQLite is fine with a note; I used H2 file-based so a reviewer's data survives a server restart mid-demo. Schema is managed by Hibernate `create-drop`, which means the database is reset each time the server starts. For a real product this would be Flyway or Liquibase against Postgres.

**Residual sorted in memory, not in the database.** Residual is derived on read and never stored, so the database can't sort by it. At risk-register scale this is fine. At 100k rows it would need either a materialized column or a more sophisticated approach.

**No N+1 query.** The list query uses `@EntityGraph` to fetch mitigations in a single join, so loading 100 risks doesn't fire 101 queries. The trade-off is that all mitigations are loaded even for the list view where only the count is displayed.

**No pagination.** Out of scope at this scale.

**No authentication.** Explicitly out of scope per the assignment.

**`Open → Mitigating` auto-transitions on the first control added.** Originally I left this manual, reasoning that some teams use `Mitigating` to mean "controls in progress" and others never use it. In practice that made status feel disconnected from the register — a risk with an active control still read as untouched `Open`. The rule now is: adding a mitigation to an `Open` risk promotes it to `Mitigating`; a risk already `Mitigating` or `Closed` is unaffected (adding a control to a closed risk doesn't reopen it). The reverse — removing all mitigations — does not automatically demote back to `Open`; that direction stays a manual, deliberate action, since "no controls left" can mean several things (removed by mistake, superseded, or simply not re-classified yet) and auto-demoting risks the same "silent status change nobody asked for" problem this rule is fixing.

**Closure fields instead of an audit-log table.** A production implementation would append immutable events rather than overwriting fields. The current model loses history when a risk is reopened and re-closed with a different reason.

**Frontend duplicates only the inherent calculation.** The `likelihood × impact` live preview in the risk form is computed client-side because it's just two numbers multiplying. Residual always comes from the API — there is one source of truth for the scoring logic, which is `RiskScoringService` on the server.

---

## What I'd do with more time

The thing I'd prioritize first is proper audit logging — an append-only event table that records every status transition, score change, and closure with a timestamp and actor. The current model rewrites fields, which means you can't reconstruct the history of a risk.

I'd also add pagination and proper database-level sorting for residual (a generated or maintained column). The in-memory sort works fine for dozens of risks but becomes a problem at scale.

On the frontend, I'd add loading skeletons and proper error boundaries instead of the bare `isLoading` checks, and a confirmation modal for destructive actions rather than the browser's default `confirm()`. I'm intentionally *not* planning optimistic updates for anything that affects score or status: the residual formula is a `BigDecimal` chain with a floor and a cap, and reimplementing it client-side to predict the post-mutation number would mean two implementations of compliance-critical logic that can drift — a transient wrong severity badge is a worse failure mode here than a slightly slower UI. I'd reconsider optimism only for fields that don't affect scoring (title, owner, description edits), where echoing back needs no server computation to be correct.
