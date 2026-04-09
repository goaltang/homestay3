---
type: community
cohesion: 0.06
members: 38
---

# System Config Admin

**Cohesion:** 0.06 - loosely connected
**Members:** 38 nodes

## Members
- [[.createConfig()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.deleteConfig()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.findByOperateTimeBetween()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.findByOperationType()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.findByOperator()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.findByResource()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.findTop50ByOrderByOperateTimeDesc()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.getAllConfigs()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getConfigByKey()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getConfigsByCategory()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getConfigsByKeys()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getLogs()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getLogs()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.getLogs()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.getLogsByOperator()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.getLogsByOperator()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.getLogsByTimeRange()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.getLogsByTimeRange()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.getRecentLogs()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.getRecentLogs()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.getRecentLogs()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.initDefaultConfigs()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[.log()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.log()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.searchLogs()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[.searchLogs()_3]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[.searchLogs()_5]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[.updateConfig()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[AdminSystemConfigController]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[AdminSystemConfigController.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AdminSystemConfigController.java
- [[OperationLog]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\OperationLog.java
- [[OperationLog.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\OperationLog.java
- [[OperationLogRepository]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[OperationLogRepository.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\OperationLogRepository.java
- [[OperationLogService]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[OperationLogService.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\OperationLogService.java
- [[OperationLogServiceImpl]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java
- [[OperationLogServiceImpl.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\OperationLogServiceImpl.java

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/System_Config_Admin
SORT file.name ASC
```

## Connections to other communities
- 1 edge to [[_COMMUNITY_Announcement Management]]
- 1 edge to [[_COMMUNITY_System Configuration]]
- 1 edge to [[_COMMUNITY_Admin Controller Layer]]

## Top bridge nodes
- [[AdminSystemConfigController.java]] - degree 5, connects to 2 communities
- [[OperationLogService.java]] - degree 5, connects to 1 community