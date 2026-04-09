---
type: community
cohesion: 0.10
members: 21
---

# Homestay Audit Workflow

**Cohesion:** 0.10 - loosely connected
**Members:** 21 nodes

## Members
- [[activateHomestay()]] - code - homestay-front\src\api\homestay\core.ts
- [[audit.ts]] - code - homestay-front\src\api\homestay\audit.ts
- [[checkHomestayOwnership()]] - code - homestay-front\src\api\homestay\core.ts
- [[checkHomestayReadyForReview()]] - code - homestay-front\src\api\homestay\audit.ts
- [[core.ts]] - code - homestay-front\src\api\homestay\core.ts
- [[createHomestay()]] - code - homestay-front\src\api\homestay\core.ts
- [[deactivateHomestay()]] - code - homestay-front\src\api\homestay\core.ts
- [[deleteHomestay()]] - code - homestay-front\src\api\homestay\core.ts
- [[getActiveHomestays()]] - code - homestay-front\src\api\homestay\core.ts
- [[getHomestayAuditHistory()]] - code - homestay-front\src\api\homestay\audit.ts
- [[getHomestayById()]] - code - homestay-front\src\api\homestay\core.ts
- [[getHomestayUnavailableDates()]] - code - homestay-front\src\api\homestay\core.ts
- [[getHomestays()]] - code - homestay-front\src\api\homestay\core.ts
- [[getHomestaysByIds()]] - code - homestay-front\src\api\homestay\core.ts
- [[getOwnerAuditStats()]] - code - homestay-front\src\api\homestay\audit.ts
- [[getOwnerHomestays()]] - code - homestay-front\src\api\homestay\core.ts
- [[requestReactivation()]] - code - homestay-front\src\api\homestay\audit.ts
- [[saveHomestayDraft()]] - code - homestay-front\src\api\homestay\audit.ts
- [[submitHomestayForReview()]] - code - homestay-front\src\api\homestay\audit.ts
- [[updateHomestay()]] - code - homestay-front\src\api\homestay\core.ts
- [[withdrawHomestayReview()]] - code - homestay-front\src\api\homestay\audit.ts

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/Homestay_Audit_Workflow
SORT file.name ASC
```

## Connections to other communities
- 2 edges to [[_COMMUNITY_Homestay Group Management]]
- 2 edges to [[_COMMUNITY_Homestay Batch Operations]]

## Top bridge nodes
- [[core.ts]] - degree 15, connects to 2 communities
- [[audit.ts]] - degree 10, connects to 2 communities