# Solera Interview Memory

## Core Intent of the Practical

- Build a meaningful MVP in 1 hour using AI-assisted development, while demonstrating engineering judgment.
- Deliver a custom implementation process (not an off-the-shelf solution).
- Provide live commentary during development: identify problems, explain compromises, and describe remediations.
- Prioritize a robust architecture and understandable code over feature volume.
- Explicitly call out what was intentionally omitted due to time and what should be fixed next.

## Expected Stack and Environment

- Backend: Java (Spring Boot, Java 17+)
- Frontend: React (TypeScript preferred)
- Database: SQL Server
- Dataset to preload: `df_VEH0120_GB.csv`
- Optional stretch: LLM integration (have provider URLs/credentials ready)

## What Interviewers Are Assessing

- Problem decomposition into clear layers:
  - Controller (REST)
  - Service (business logic)
  - Repository/data access
  - React UI components
  - End-to-end data flow
- Debugging and recovery under time pressure (API, serialization, SQL, wiring).
- Data contract thinking:
  - DTO design
  - entity vs DTO mapping strategy
  - pagination/filtering/response shape
- Trade-off communication:
  - speed vs maintainability
  - JPA vs raw SQL
  - synchronous vs async patterns
  - over-engineering vs shipping a demo

## Scoring Risk to Avoid

- Unidentified issues count against score.
- AI-generated code without understanding or critique is a negative signal.
- "Vibe coding" without reasoning, ownership, and corrections is explicitly discouraged.

## Practical Execution Priorities

1. Establish thin vertical slice quickly (DB -> API -> UI).
2. Keep boundaries clean (controllers thin, services purposeful, repositories focused).
3. Add visible value with minimal but coherent UI interactions.
4. Narrate compromises in real time.
5. Reserve time for final walkthrough of known limitations and next fixes.

## Suggested Interview Narrative

- Start: state assumptions and MVP scope.
- During build: explain each architectural choice and trade-off.
- During issues: describe diagnosis path and why chosen fix is safest/fastest.
- End: summarize delivered capabilities, known gaps, and production-hardening steps.

## Testing Expectation

- Full automated tests are not required in-session.
- Still mention how tests would be added after MVP:
  - backend unit/integration tests
  - API contract tests
  - frontend component/integration tests
  - smoke tests for critical flows

