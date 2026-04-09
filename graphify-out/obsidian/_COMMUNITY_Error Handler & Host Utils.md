---
type: community
cohesion: 0.10
members: 21
---

# Error Handler & Host Utils

**Cohesion:** 0.10 - loosely connected
**Members:** 21 nodes

## Members
- [[becomeHost()]] - code - homestay-front\src\api\host.ts
- [[errorHandler.ts]] - code - homestay-front\src\utils\errorHandler.ts
- [[getHomestayHostInfo()]] - code - homestay-front\src\api\host.ts
- [[getHostEarnings()]] - code - homestay-front\src\api\host.ts
- [[getHostHomestayOptions()]] - code - homestay-front\src\api\host.ts
- [[getHostHomestays()]] - code - homestay-front\src\api\host.ts
- [[getHostInfo()]] - code - homestay-front\src\api\host.ts
- [[getHostMonthlyEarnings()]] - code - homestay-front\src\api\host.ts
- [[getHostOrders()]] - code - homestay-front\src\api\host.ts
- [[getHostProfile()]] - code - homestay-front\src\api\host.ts
- [[getHostRecentOrders()]] - code - homestay-front\src\api\host.ts
- [[getHostReviews()]] - code - homestay-front\src\api\host.ts
- [[getHostStatistics()]] - code - homestay-front\src\api\host.ts
- [[handleApiError()]] - code - homestay-front\src\utils\errorHandler.ts
- [[host.ts]] - code - homestay-front\src\types\host.ts
- [[hostUtils.ts]] - code - homestay-front\src\utils\hostUtils.ts
- [[normalizeApiError()]] - code - homestay-front\src\utils\errorHandler.ts
- [[updateHostInfo()]] - code - homestay-front\src\api\host.ts
- [[updateHostProfile()]] - code - homestay-front\src\api\host.ts
- [[uploadHostAvatar()]] - code - homestay-front\src\api\host.ts
- [[uploadHostDocument()]] - code - homestay-front\src\api\host.ts

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/Error_Handler_&_Host_Utils
SORT file.name ASC
```

## Connections to other communities
- 2 edges to [[_COMMUNITY_Homestay Batch Operations]]
- 1 edge to [[_COMMUNITY_Order Lifecycle]]
- 1 edge to [[_COMMUNITY_Host Display Utilities]]

## Top bridge nodes
- [[host.ts]] - degree 20, connects to 1 community
- [[errorHandler.ts]] - degree 4, connects to 1 community
- [[hostUtils.ts]] - degree 2, connects to 1 community