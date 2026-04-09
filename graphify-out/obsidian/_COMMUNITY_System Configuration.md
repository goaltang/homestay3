---
type: community
cohesion: 0.07
members: 40
---

# System Configuration

**Cohesion:** 0.07 - loosely connected
**Members:** 40 nodes

## Members
- [[.clearConfigCache()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.deleteByConfigKey()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[.deleteConfig()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.existsByConfigKey()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[.findByCategory()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[.findByConfigKey()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[.getAllConfigs()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.getConfigByKey()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.getConfigValue()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.getConfigsByCategory()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.getConfigsByKeys()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.initConfigIfNotExists()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.initDefaultConfigs()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.migrateConfigIfNeeded()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.saveConfig()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[.setUp()_9]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testDeleteConfig()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetAllConfigs()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigByKey_Found()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigByKey_NotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigValue_Found()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigValue_NotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigValue_WithDefault()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigsByCategory()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigsByKeys()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testGetConfigsByKeys_PartialNotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testInitDefaultConfigs()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testInitDefaultConfigs_SkipExisting()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testSaveConfig()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testUpdateConfig_NotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.testUpdateConfig_Success()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[.updateConfig()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[SystemConfig]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\SystemConfig.java
- [[SystemConfig.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\SystemConfig.java
- [[SystemConfigRepository]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[SystemConfigRepository.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\SystemConfigRepository.java
- [[SystemConfigServiceImpl]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[SystemConfigServiceImpl.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImpl.java
- [[SystemConfigServiceImplTest]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java
- [[SystemConfigServiceImplTest.java]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\SystemConfigServiceImplTest.java

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/System_Configuration
SORT file.name ASC
```

## Connections to other communities
- 2 edges to [[_COMMUNITY_Admin Controller Layer]]
- 1 edge to [[_COMMUNITY_System Config Admin]]

## Top bridge nodes
- [[SystemConfig.java]] - degree 6, connects to 2 communities
- [[SystemConfigServiceImpl.java]] - degree 4, connects to 1 community