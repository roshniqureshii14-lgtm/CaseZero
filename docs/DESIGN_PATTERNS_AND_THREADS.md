# CASEZERO: design patterns and multithreading

## Design pattern 1 — Repository
Spring Data JPA repository interfaces such as `UserAccountRepository`, `CaseRecordRepository`, `ContactMessageRepository` and `InvestigationDraftRepository` separate persistence operations from controller logic. The application calls repository methods rather than placing SQL statements inside page controllers.

## Design pattern 2 — Strategy
`ResearchArticleMatchingStrategy` defines a common contract for deciding whether a research article matches a search. `KeywordResearchArticleMatchingStrategy` is the concrete keyword-based strategy. `NewsResearchService` depends on the interface, so a different matching approach can be introduced without rewriting the publisher-feed collection workflow.

## Multithreading
`ResearchThreadPoolConfig` configures a bounded pool of four worker threads. `NewsResearchService` submits independent publisher RSS requests using `CompletableFuture.supplyAsync`. The publisher requests can therefore run concurrently instead of waiting for each feed sequentially. The bounded pool limits resource use, while each connection retains its connect and read timeouts.

## Important distinction
Research feed results are public reporting leads, not automatically verified CASEZERO cases. Source checking and administrative review remain necessary before information becomes an approved public case.
