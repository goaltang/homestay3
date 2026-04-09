# Graph Report - D:/homestay3  (2026-04-08)

## Corpus Check
- Large corpus: 423 files ¡¤ ~150,916 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder, or use --no-semantic to run AST-only.

## Summary
- 3127 nodes ¡¤ 4419 edges ¡¤ 156 communities detected
- Extraction: 85% EXTRACTED ¡¤ 15% INFERRED ¡¤ 0% AMBIGUOUS ¡¤ INFERRED: 642 edges (avg confidence: 0.5)
- Token cost: 0 input ¡¤ 0 output

## God Nodes (most connected - your core abstractions)
1. `OrderRepository` - 48 edges
2. `User` - 47 edges
3. `OrderServiceImpl` - 42 edges
4. `AmenityServiceImpl` - 38 edges
5. `HomestayRepository` - 36 edges
6. `AmenityService` - 34 edges
7. `HomestayRecommendationServiceImpl` - 34 edges
8. `ReviewRepository` - 32 edges
9. `HomestayFeatureAnalysisServiceImpl` - 30 edges
10. `HomestayServiceImpl` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Homestay Booking Platform` --semantically_similar_to--> `Admin Frontend`  [INFERRED] [semantically similar]
  QWEN.md ¡ú README.md
- `Vue.js Framework Logo` --represents--> `Vue 3`  [EXTRACTED]
  homestay-admin/src/assets/vue.svg ¡ú README.md
- `Login Background Image` --used_by--> `Admin Frontend`  [EXTRACTED]
  homestay-admin/src/assets/images/login-bg.jpg ¡ú README.md
- `Vite Build Tool Logo` --represents--> `Vite Build Tool`  [EXTRACTED]
  homestay-admin/public/vite.svg ¡ú homestay-admin è¯¦ç»†ç»“æž„.md
- `Homestay Booking Platform` --uses--> `Java 17`  [EXTRACTED]
  QWEN.md ¡ú README.md

## Hyperedges (group relationships)
- **Payment Gateway Adapter Pattern** ¡ª paymentgateway, alipaygateway, wechatgateway [EXTRACTED 1.00]
- **Payment Callback Handler Pattern** ¡ª alipaygateway, alipaycallbackcontroller, wechatgateway, wechatcallbackcontroller [EXTRACTED 1.00]
- **Payment Service Test Coverage** ¡ª paymentserviceimpltest, paymentserviceimpl, paymentrecord, order [EXTRACTED 0.85]
- **Frontend Technology Stack** ¡ª vue_3, element_plus, pinia, axios, vite_build_tool, typescript, echarts [EXTRACTED 1.00]
- **Backend Technology Stack** ¡ª java_17, spring_boot_302, mysql_80, redis, spring_security, jwt, websocket, alipay_sdk, mapstruct, flyway [EXTRACTED 1.00]
- **Backend Layered Architecture** ¡ª api_response, global_exception_handler, mapstruct [EXTRACTED 1.00]

## Communities

### Community 0 - "Admin Controllers"
Cohesion: 0.02
Nodes (54): AccessDeniedException, AlipayCallbackController, AuthResponse, CustomUserDetailsService, DailyEarningDTO, DisputeService, Earning, EarningDTO (+46 more)

### Community 1 - "Amenities Frontend API"
Cohesion: 0.01
Nodes (2): addTimestampToUrl(), getAvatarUrl()

### Community 2 - "Admin Homestay Management"
Cohesion: 0.02
Nodes (26): AdminHomestayController, Amenity, AmenityCategory, AmenityCategoryDTO, AmenityCategoryRepository, AmenityDTO, AmenityRepository, DataInitializer (+18 more)

### Community 3 - "Authentication & Auth Manager"
Cohesion: 0.02
Nodes (22): alertLogin(), AuthManager, checkAuthentication(), ensureUserLoggedIn(), getCurrentUser(), getToken(), isAdmin(), isAuthenticated() (+14 more)

### Community 4 - "Payment & Chat Integration"
Cohesion: 0.03
Nodes (43): Alipay SDK, API Interface Layer, ApiResponse Wrapper, Audit/Review Module, Auto Import Feature, ChatMessageDTO, Vue Composition API, ECharts (+35 more)

### Community 5 - "API Response & Chat Controller"
Cohesion: 0.03
Nodes (13): ApiResponse, ChatController, ChatService, ChatServiceImpl, Conversation, ConversationDTO, ConversationRepository, CustomUserDetails (+5 more)

### Community 6 - "Admin Auth Controller"
Cohesion: 0.03
Nodes (14): Admin, AdminAuthController, AdminLoginRequest, AdminLoginResponse, AdminRepository, AdminService, AdminServiceImpl, AuthController (+6 more)

### Community 7 - "Homestay Type Management"
Cohesion: 0.03
Nodes (9): HomestayType, HomestayTypeController, HomestayTypeDTO, HomestayTypeRepository, HomestayTypeService, HomestayTypeServiceImpl, TypeCategory, TypeCategoryDTO (+1 more)

### Community 8 - "File Management"
Cohesion: 0.04
Nodes (9): FileController, FileService, FileServiceImpl, FileStorageConfig, HomestayImage, HomestayImageController, HomestayImageRepository, HomestayImageService (+1 more)

### Community 9 - "Recommendation Engine"
Cohesion: 0.09
Nodes (3): HomestayRecommendationServiceImpl, SimilarityScore, UserPreference

### Community 10 - "Announcement Management"
Cohesion: 0.04
Nodes (6): AdminAnnouncementController, Announcement, AnnouncementController, AnnouncementRepository, AnnouncementService, AnnouncementServiceImpl

### Community 11 - "System Configuration"
Cohesion: 0.05
Nodes (4): SystemConfig, SystemConfigRepository, SystemConfigServiceImpl, SystemConfigServiceImplTest

### Community 12 - "Statistics & Analytics"
Cohesion: 0.05
Nodes (8): AdminStatisticsController, AuditLogDTO, HomestayAuditController, HomestayAuditLog, HomestayAuditService, IpUtil, ReviewRequest, StatisticsService

### Community 13 - "Order Repository"
Cohesion: 0.04
Nodes (1): OrderRepository

### Community 14 - "User Entity Model"
Cohesion: 0.04
Nodes (1): User

### Community 15 - "Order Service Impl"
Cohesion: 0.06
Nodes (1): OrderServiceImpl

### Community 16 - "Check-In Controller"
Cohesion: 0.05
Nodes (5): CheckInController, CheckInDTO, CheckInService, CheckOutDTO, CheckOutService

### Community 17 - "Review Management Frontend"
Cohesion: 0.05
Nodes (7): createReview(), Review, submitReview(), ReviewImage, ReviewImageDTO, ReviewImageRepository, ReviewImageService

### Community 18 - "System Config Admin"
Cohesion: 0.06
Nodes (5): AdminSystemConfigController, OperationLog, OperationLogRepository, OperationLogService, OperationLogServiceImpl

### Community 19 - "Amenity Service"
Cohesion: 0.1
Nodes (1): AmenityServiceImpl

### Community 20 - "Violation Management"
Cohesion: 0.06
Nodes (5): ViolationAction, ViolationController, ViolationReport, ViolationReportDTO, ViolationService

### Community 21 - "Homestay Repository"
Cohesion: 0.06
Nodes (1): HomestayRepository

### Community 22 - "Dispute Management"
Cohesion: 0.08
Nodes (7): AdminDisputeController, DisputeRecord, DisputeRecordDTO, DisputeRecordRepository, DisputeRecordService, DisputeRecordServiceImpl, HostDisputeController

### Community 23 - "Host Controller & DTOs"
Cohesion: 0.06
Nodes (5): HomestayOptionDTO, HostController, HostDTO, HostService, HostStatisticsDTO

### Community 24 - "Amenity Service Interface"
Cohesion: 0.06
Nodes (1): AmenityService

### Community 25 - "Feature Analysis & Rules"
Cohesion: 0.11
Nodes (2): CombinationRule, HomestayFeatureAnalysisServiceImpl

### Community 26 - "Review Repository"
Cohesion: 0.06
Nodes (1): ReviewRepository

### Community 27 - "Homestay Service"
Cohesion: 0.12
Nodes (1): HomestayServiceImpl

### Community 28 - "Order Lifecycle Management"
Cohesion: 0.21
Nodes (1): OrderLifecycleServiceImpl

### Community 29 - "Order Controller"
Cohesion: 0.12
Nodes (1): OrderController

### Community 30 - "Availability Management"
Cohesion: 0.08
Nodes (3): HomestayAvailability, HomestayAvailabilityRepository, HomestayAvailabilityService

### Community 31 - "Order Service Interface"
Cohesion: 0.08
Nodes (1): OrderService

### Community 32 - "Price Analysis Service"
Cohesion: 0.13
Nodes (4): PriceAnalysisService, PriceAnalysisServiceImpl, PriceStatistics, PriceCompetitivenessDTO

### Community 33 - "Order Service Tests"
Cohesion: 0.13
Nodes (1): OrderServiceImplTest

### Community 34 - "Host Order Frontend"
Cohesion: 0.08
Nodes (0): 

### Community 35 - "Homestay Controller"
Cohesion: 0.12
Nodes (1): HomestayController

### Community 36 - "Check-Out Service"
Cohesion: 0.24
Nodes (1): CheckOutServiceImpl

### Community 37 - "Login Log Management"
Cohesion: 0.1
Nodes (5): AdminLoginLogController, LoginLog, LoginLogRepository, LoginLogService, LoginLogServiceImpl

### Community 38 - "Order Notification Service"
Cohesion: 0.17
Nodes (1): OrderNotificationServiceImpl

### Community 39 - "Amenities Controller"
Cohesion: 0.11
Nodes (1): AmenitiesController

### Community 40 - "Check-In Service"
Cohesion: 0.23
Nodes (1): CheckInServiceImpl

### Community 41 - "Violation Service Tests"
Cohesion: 0.1
Nodes (1): ViolationServiceImplTest

### Community 42 - "Map API & Geocoding"
Cohesion: 0.13
Nodes (20): Amap (¸ßµÂµØÍ¼) API, API Whitelist Mechanism, Order Count Badge, Element China Area Data, Frontend-based Filtering, Geocoding Service, Interactive Map Feature, Map Service Module (+12 more)

### Community 43 - "Homestay Service Interface"
Cohesion: 0.11
Nodes (1): HomestayService

### Community 44 - "Review Service Impl"
Cohesion: 0.13
Nodes (1): ReviewServiceImpl

### Community 45 - "Review Service Interface"
Cohesion: 0.11
Nodes (1): ReviewService

### Community 46 - "Host Service Impl"
Cohesion: 0.17
Nodes (1): HostServiceImpl

### Community 47 - "User Service Impl"
Cohesion: 0.18
Nodes (1): UserServiceImpl

### Community 48 - "Homestay Group Tests"
Cohesion: 0.12
Nodes (1): HomestayGroupServiceTest

### Community 49 - "Auth Service Tests"
Cohesion: 0.12
Nodes (1): AuthServiceImplTest

### Community 50 - "Earning Repository"
Cohesion: 0.12
Nodes (1): EarningRepository

### Community 51 - "Order Lifecycle Service Interface"
Cohesion: 0.12
Nodes (1): OrderLifecycleService

### Community 52 - "Order Notification Service Interface"
Cohesion: 0.12
Nodes (1): OrderNotificationService

### Community 53 - "User Service Interface"
Cohesion: 0.12
Nodes (1): UserService

### Community 54 - "Homestay Audit Service"
Cohesion: 0.23
Nodes (1): HomestayAuditServiceImpl

### Community 55 - "User Favorite Entity"
Cohesion: 0.13
Nodes (1): UserFavorite

### Community 56 - "Host Utils"
Cohesion: 0.26
Nodes (1): HostUtils

### Community 57 - "Review Controller"
Cohesion: 0.14
Nodes (1): ReviewController

### Community 58 - "Homestay Group Repository"
Cohesion: 0.14
Nodes (1): HomestayGroupRepository

### Community 59 - "User Repository"
Cohesion: 0.14
Nodes (1): UserRepository

### Community 60 - "Homestay Group Service Interface"
Cohesion: 0.2
Nodes (1): HomestayGroupService

### Community 61 - "Auth Service Impl"
Cohesion: 0.2
Nodes (1): AuthServiceImpl

### Community 62 - "Homestay Status Utility"
Cohesion: 0.15
Nodes (1): HomestayStatusUtil

### Community 63 - "Admin Order Management"
Cohesion: 0.15
Nodes (1): AdminOrderController

### Community 64 - "Violation Report Repository"
Cohesion: 0.15
Nodes (1): ViolationReportRepository

### Community 65 - "Notification Service Impl"
Cohesion: 0.22
Nodes (1): NotificationServiceImpl

### Community 66 - "Order Auto Status Service"
Cohesion: 0.26
Nodes (1): OrderAutoStatusService

### Community 67 - "Payment Processing Service"
Cohesion: 0.33
Nodes (1): PaymentProcessingServiceImpl

### Community 68 - "Host Information Module"
Cohesion: 0.18
Nodes (13): Achievement Badges System, Host Brief Card Component, HostDTO Interface, Host Information Module, Host Profile Display, HostUtils Utility Class, Language Mapping System, Professional Level Assessment (+5 more)

### Community 69 - "User Favorite Controller"
Cohesion: 0.33
Nodes (1): UserFavoriteController

### Community 70 - "User Favorite DTO"
Cohesion: 0.17
Nodes (1): UserFavoriteDTO

### Community 71 - "Homestay Audit Log Repository"
Cohesion: 0.17
Nodes (1): HomestayAuditLogRepository

### Community 72 - "JWT Authentication Filter"
Cohesion: 0.24
Nodes (2): JwtAuthenticationFilter, SecurityConfig

### Community 73 - "Alipay Gateway"
Cohesion: 0.26
Nodes (1): AlipayGateway

### Community 74 - "User Favorite Service Impl"
Cohesion: 0.17
Nodes (1): UserFavoriteServiceImpl

### Community 75 - "Violation Service Impl"
Cohesion: 0.17
Nodes (1): ViolationServiceImpl

### Community 76 - "Dispute Service Tests"
Cohesion: 0.18
Nodes (1): DisputeServiceImplTest

### Community 77 - "Test Data Factory"
Cohesion: 0.29
Nodes (1): TestDataFactory

### Community 78 - "System Config Service"
Cohesion: 0.2
Nodes (1): SystemConfigService

### Community 79 - "Order Timeout Service"
Cohesion: 0.33
Nodes (1): OrderTimeoutService

### Community 80 - "Payment Service Impl"
Cohesion: 0.33
Nodes (1): PaymentServiceImpl

### Community 81 - "Homestay Group Controller"
Cohesion: 0.22
Nodes (1): HomestayGroupController

### Community 82 - "Notification Controller"
Cohesion: 0.39
Nodes (1): NotificationController

### Community 83 - "Order Auto Status Controller"
Cohesion: 0.22
Nodes (1): OrderAutoStatusController

### Community 84 - "Check-In Record"
Cohesion: 0.22
Nodes (2): CheckInRecord, CheckInRecordRepository

### Community 85 - "Notification Service Interface"
Cohesion: 0.22
Nodes (1): NotificationService

### Community 86 - "Earning Service Impl"
Cohesion: 0.25
Nodes (1): EarningServiceImpl

### Community 87 - "Homestay Service Tests"
Cohesion: 0.33
Nodes (1): HomestayServiceImplTest

### Community 88 - "Order Notification Dispute Tests"
Cohesion: 0.22
Nodes (1): OrderNotificationServiceImplDisputeTest

### Community 89 - "Order Permission Guards"
Cohesion: 0.5
Nodes (7): canAccessOrder(), canPerformActionOnStatus(), checkOrderPermission(), getAvailableActions(), isAdmin(), isOrderHost(), isOrderOwner()

### Community 90 - "Admin Review Controller"
Cohesion: 0.25
Nodes (1): AdminReviewController

### Community 91 - "Admin User Management"
Cohesion: 0.25
Nodes (1): AdminUserController

### Community 92 - "Earning Controller"
Cohesion: 0.25
Nodes (1): EarningController

### Community 93 - "Check-Out Record"
Cohesion: 0.25
Nodes (2): CheckOutRecord, CheckOutRecordRepository

### Community 94 - "Earning Service Interface"
Cohesion: 0.25
Nodes (1): EarningService

### Community 95 - "Statistics Service Impl"
Cohesion: 0.39
Nodes (1): StatisticsServiceImpl

### Community 96 - "Application Integration Tests"
Cohesion: 0.25
Nodes (2): ApplicationIntegrationTest, HomestayBackendApplication

### Community 97 - "Payment QR Code Tests"
Cohesion: 0.25
Nodes (1): GeneratePaymentQRCodeTests

### Community 98 - "API Adapter Utilities"
Cohesion: 0.33
Nodes (2): adaptOrderItem(), adaptOrderStatus()

### Community 99 - "Cache Configuration"
Cohesion: 0.38
Nodes (1): CacheConfig

### Community 100 - "Handle Payment Notify Tests"
Cohesion: 0.67
Nodes (1): HandlePaymentNotifyTests

### Community 101 - "Payment Processing Service Interface"
Cohesion: 0.29
Nodes (1): PaymentProcessingService

### Community 102 - "Payment Status Tests"
Cohesion: 0.29
Nodes (1): CheckPaymentStatusTests

### Community 103 - "Process Refund Tests"
Cohesion: 0.29
Nodes (1): ProcessRefundTests

### Community 104 - "Admin Group Controller"
Cohesion: 0.33
Nodes (1): AdminGroupController

### Community 105 - "Admin Verification Management"
Cohesion: 0.4
Nodes (1): AdminVerificationController

### Community 106 - "Cache Controller"
Cohesion: 0.33
Nodes (1): CacheController

### Community 107 - "Location Controller"
Cohesion: 0.53
Nodes (1): LocationController

### Community 108 - "Refund Record Repository"
Cohesion: 0.33
Nodes (1): RefundRecordRepository

### Community 109 - "Content Filter Service"
Cohesion: 0.4
Nodes (1): ContentFilterService

### Community 110 - "Payment Service Interface"
Cohesion: 0.33
Nodes (1): PaymentService

### Community 111 - "Payment Gateway Interface"
Cohesion: 0.33
Nodes (1): PaymentGateway

### Community 112 - "Image URL Utility"
Cohesion: 0.53
Nodes (1): ImageUrlUtil

### Community 113 - "Booking Workflow Integration Tests"
Cohesion: 0.53
Nodes (1): BookingWorkflowIntegrationTest

### Community 114 - "Mock Payment Tests"
Cohesion: 0.33
Nodes (1): MockSuccessPaymentTests

### Community 115 - "Payment Controller"
Cohesion: 0.4
Nodes (1): PaymentController

### Community 116 - "User Mapper"
Cohesion: 0.4
Nodes (1): UserMapper

### Community 117 - "Payment Record Repository"
Cohesion: 0.4
Nodes (1): PaymentRecordRepository

### Community 118 - "Dispute Service Impl"
Cohesion: 0.8
Nodes (1): DisputeServiceImpl

### Community 119 - "Database Health Checker"
Cohesion: 0.6
Nodes (1): DatabaseHealthChecker

### Community 120 - "Redis Distributed Lock"
Cohesion: 0.4
Nodes (1): RedisLock

### Community 121 - "Base Test Class"
Cohesion: 0.4
Nodes (1): BaseTest

### Community 122 - "Concurrent Booking Tests"
Cohesion: 0.7
Nodes (1): ConcurrentBookingTest

### Community 123 - "Format Utilities"
Cohesion: 0.4
Nodes (0): 

### Community 124 - "Redis Health Indicator"
Cohesion: 0.5
Nodes (1): RedisHealthIndicator

### Community 125 - "WebSocket Configuration"
Cohesion: 0.5
Nodes (1): WebSocketConfig

### Community 126 - "System Controller"
Cohesion: 0.5
Nodes (1): SystemController

### Community 127 - "Email Service Impl"
Cohesion: 0.5
Nodes (1): EmailServiceImpl

### Community 128 - "Notification Cleanup Service"
Cohesion: 0.5
Nodes (1): NotificationCleanupService

### Community 129 - "WebSocket Notification Service"
Cohesion: 0.5
Nodes (1): WebSocketNotificationServiceImpl

### Community 130 - "Test Configuration"
Cohesion: 0.5
Nodes (1): TestConfig

### Community 131 - "Date Utilities"
Cohesion: 0.5
Nodes (0): 

### Community 132 - "Platform Branding Assets"
Cohesion: 0.5
Nodes (4): Homestay Platform Logo, Homestay Platform Frontend, Vite Framework Logo, Vue 3 + TypeScript + Vite

### Community 133 - "Database Initializer"
Cohesion: 0.67
Nodes (1): DatabaseInitializer

### Community 134 - "HTTP Client Configuration"
Cohesion: 0.67
Nodes (1): HttpClientConfig

### Community 135 - "JPA Auditing Configuration"
Cohesion: 0.67
Nodes (1): JpaAuditingConfig

### Community 136 - "Mail Configuration"
Cohesion: 0.67
Nodes (1): MailConfig

### Community 137 - "Network Configuration"
Cohesion: 0.67
Nodes (1): NetworkConfig

### Community 138 - "Redisson Configuration"
Cohesion: 0.67
Nodes (1): RedissonConfig

### Community 139 - "Web Configuration"
Cohesion: 0.67
Nodes (1): WebConfig

### Community 140 - "Web MVC Configuration"
Cohesion: 0.67
Nodes (1): WebMvcConfig

### Community 141 - "Booking Conflict Service"
Cohesion: 1.0
Nodes (1): BookingConflictService

### Community 142 - "Backend Application Tests"
Cohesion: 0.67
Nodes (1): HomestayBackendApplicationTests

### Community 143 - "Debounce Utilities"
Cohesion: 0.67
Nodes (0): 

### Community 144 - "Login Request DTO"
Cohesion: 1.0
Nodes (1): LoginRequest

### Community 145 - "Search Params Hook"
Cohesion: 1.0
Nodes (0): 

### Community 146 - "Search Suggestions Hook"
Cohesion: 1.0
Nodes (0): 

### Community 147 - "Menu Module"
Cohesion: 1.0
Nodes (0): 

### Community 148 - "Community 148"
Cohesion: 1.0
Nodes (0): 

### Community 149 - "Community 149"
Cohesion: 1.0
Nodes (0): 

### Community 150 - "Community 150"
Cohesion: 1.0
Nodes (0): 

### Community 151 - "Community 151"
Cohesion: 1.0
Nodes (0): 

### Community 152 - "Community 152"
Cohesion: 1.0
Nodes (0): 

### Community 153 - "Community 153"
Cohesion: 1.0
Nodes (0): 

### Community 154 - "Community 154"
Cohesion: 1.0
Nodes (0): 

### Community 155 - "Community 155"
Cohesion: 1.0
Nodes (1): Obsidian API Configuration

## Knowledge Gaps
- **115 isolated node(s):** `PaymentConfig`, `AlipayConfig`, `WechatConfig`, `AdminLoginRequest`, `AdminLoginResponse` (+110 more)
  These have ¡Ü1 connection - possible missing edges or undocumented components.
- **Thin community `Login Request DTO`** (2 nodes): `LoginRequest.java`, `LoginRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Search Params Hook`** (2 nodes): `useSearchParams.ts`, `useSearchParams()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Search Suggestions Hook`** (2 nodes): `useSearchSuggestions.ts`, `useSearchSuggestions()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Menu Module`** (1 nodes): `menu.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 148`** (1 nodes): `auto-imports.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 149`** (1 nodes): `components.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 150`** (1 nodes): `vite.config.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 151`** (1 nodes): `vite.config.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 152`** (1 nodes): `vite-env.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 153`** (1 nodes): `china.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 154`** (1 nodes): `element-plus.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 155`** (1 nodes): `Obsidian API Configuration`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `OrderRepository` connect `Order Repository` to `Admin Controllers`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `User` connect `User Entity Model` to `Admin Controllers`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `OrderServiceImpl` connect `Order Service Impl` to `Admin Controllers`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **What connects `PaymentConfig`, `AlipayConfig`, `WechatConfig` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Admin Controllers` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Should `Amenities Frontend API` be split into smaller, more focused modules?**
  _Cohesion score 0.01 - nodes in this community are weakly interconnected._
- **Should `Admin Homestay Management` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._