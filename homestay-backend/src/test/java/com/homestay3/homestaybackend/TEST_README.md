# 民宿预订平台测试文档

## 测试覆盖率现状

### 已完成的测试

#### 1. 单元测试 (Service Layer)

##### OrderServiceImplTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/service/impl/OrderServiceImplTest.java`
- **覆盖功能**:
  - ✅ 订单查询 (getOrderById, getOrderByOrderNumber)
  - ✅ 订单状态更新 (updateOrderStatus)
  - ✅ 订单取消 (cancelOrder)
  - ✅ 订单支付 (payOrder)
  - ✅ 订单预览 (createOrderPreview)
  - ✅ 系统取消订单 (systemCancelOrder)
  - ✅ 异常情况处理 (订单不存在、日期无效等)

##### AuthServiceImplTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/service/impl/AuthServiceImplTest.java`
- **覆盖功能**:
  - ✅ 用户注册 (register) - 成功与失败场景
  - ✅ 用户登录 (login) - 成功与失败场景
  - ✅ 用户信息获取 (getUserInfo)
  - ✅ 用户名/邮箱存在性检查
  - ✅ 头像更新

##### PaymentServiceImplTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/service/impl/PaymentServiceImplTest.java`
- **覆盖功能**:
  - ✅ 支付二维码生成
  - ✅ 支付状态检查
  - ✅ 模拟支付成功
  - ✅ 支付回调处理
  - ✅ 支付失败处理

#### 2. Controller 测试

##### AuthControllerTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/controller/AuthControllerTest.java`
- **覆盖功能**:
  - ✅ 登录端点测试
  - ✅ 注册端点测试
  - ✅ 用户认证测试

#### 3. 集成测试

##### ApplicationIntegrationTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/integration/ApplicationIntegrationTest.java`
- **覆盖功能**:
  - ✅ 应用上下文加载
  - ✅ 健康检查端点
  - ✅ API 根路径访问

## 测试工具类

### TestDataFactory
- **位置**: `src/test/java/com/homestay3/homestaybackend/utils/TestDataFactory.java`
- **用途**: 提供创建测试数据的便捷方法
- **方法**:
  - `createUser()` - 创建测试用户
  - `createHomestay()` - 创建测试房源
  - `createOrder()` - 创建测试订单
  - `createOrderDTO()` - 创建订单 DTO

### TestConfig
- **位置**: `src/test/java/com/homestay3/homestaybackend/config/TestConfig.java`
- **用途**: 测试配置类，提供 PasswordEncoder 等 Bean

### BaseTest
- **位置**: `src/test/java/com/homestay3/homestaybackend/BaseTest.java`
- **用途**: 测试基类，提供通用的设置和清理逻辑

## 运行测试

### 运行所有测试
```bash
cd homestay-backend
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=OrderServiceImplTest
mvn test -Dtest=AuthServiceImplTest
mvn test -Dtest=PaymentServiceImplTest
```

### 运行测试并生成报告
```bash
mvn test jacoco:report
```

### 跳过测试
```bash
mvn clean install -DskipTests
```

## 测试最佳实践

1. **单元测试**: 使用 Mockito 隔离被测组件，不依赖真实数据库
2. **集成测试**: 使用 `@SpringBootTest` 和内存数据库 (H2) 进行端到端测试
3. **命名规范**: 测试方法名应清晰表达测试场景，如 `methodName_Scenario_ExpectedResult`
4. **Given-When-Then**: 测试结构遵循准备-执行-验证的模式
5. **独立测试**: 每个测试应独立运行，不依赖其他测试的执行顺序

## 待完善项

### 建议增加的测试

#### 高优先级
1. **HomestayService 测试** - 房源管理核心逻辑
2. **UserService 测试** - 用户管理核心逻辑
3. **ReviewService 测试** - 评价系统核心逻辑
4. **EarningService 测试** - 收益计算核心逻辑

#### 中优先级
1. **所有 Controller 的 WebMvcTest**
2. **Repository 层的 DataJpaTest**
3. **安全性测试** - JWT 认证、权限控制
4. **并发测试** - 订单并发处理、库存扣减

#### 低优先级
1. **性能测试** - API 响应时间、数据库查询优化
2. **端到端测试** - 使用 Selenium 或 Cypress

## 测试覆盖率目标

- **当前覆盖率**: ~15% (核心业务逻辑)
- **短期目标**: 50% (核心业务逻辑全覆盖)
- **中期目标**: 70% (包含 Controller 和 Repository)
- **长期目标**: 80%+ (包含集成测试和异常场景)

## 注意事项

1. 测试数据应与生产数据隔离
2. 敏感信息（如密码）不应硬编码在测试中
3. 测试应能快速执行，避免长时间等待
4. 使用 `@Transactional` 确保测试数据不会污染数据库
