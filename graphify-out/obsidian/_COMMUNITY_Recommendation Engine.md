---
type: community
cohesion: 0.20
members: 10
---

# Recommendation Engine

**Cohesion:** 0.20 - loosely connected
**Members:** 10 nodes

## Members
- [[getLocationBasedRecommendations()]] - code - homestay-front\src\api\recommendation.ts
- [[getPersonalizedRecommendations()]] - code - homestay-front\src\api\recommendation.ts
- [[getPopularHomestays()]] - code - homestay-front\src\api\recommendation.ts
- [[getPopularHomestaysPage()]] - code - homestay-front\src\api\recommendation.ts
- [[getRecommendationsByRequest()]] - code - homestay-front\src\api\recommendation.ts
- [[getRecommendedHomestays()]] - code - homestay-front\src\api\recommendation.ts
- [[getRecommendedHomestaysPage()]] - code - homestay-front\src\api\recommendation.ts
- [[getSimilarHomestays()]] - code - homestay-front\src\api\recommendation.ts
- [[recommendation.ts]] - code - homestay-front\src\api\recommendation.ts
- [[refreshRecommendationCache()]] - code - homestay-front\src\api\recommendation.ts

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/Recommendation_Engine
SORT file.name ASC
```

## Connections to other communities
- 1 edge to [[_COMMUNITY_Homestay Batch Operations]]

## Top bridge nodes
- [[recommendation.ts]] - degree 10, connects to 1 community