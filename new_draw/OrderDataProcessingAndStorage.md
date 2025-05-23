# 订单模块数据存储和数据处理

## 2. 数据存储

订单模块数据存储如下表 3.5 所示：

### 表 3.5 订单模块数据存储表

| 序号 | 数据存储名称         | 描述             | 组成                                                         | 主键    |
| ---- | -------------------- | ---------------- | ------------------------------------------------------------ | ------- |
| 1    | D1 订单表(orders)    | 存储订单全部信息 | 订单 ID+用户 ID+民宿 ID+入住日期+退房日期+价格+状态+创建时间 | 订单 ID |
| 2    | D2 房源表(homestays) | 存储房源基本信息 | 房源 ID+房东 ID+房源描述+地址+价格+设施+状态                 | 房源 ID |
| 3    | D3 用户表(users)     | 存储用户基本信息 | 用户 ID+用户名+密码+电话+邮箱+角色+注册时间                  | 用户 ID |

该模块主要涉及三个核心数据存储：

- **D1 订单表(orders)**: 存储所有订单的详细信息，包括订单 ID、关联的用户和民宿、入住和退房日期、订单价格、当前状态以及创建时间等。主键为订单 ID。
- **D2 房源表(homestays)**: 存储民宿房源信息，在订单创建时需要验证房源可用性，并与订单关联。主键为房源 ID。
- **D3 用户表(users)**: 存储用户信息，包括普通用户和房东的信息，在订单查询和处理中需要关联用户数据。主键为用户 ID。

## 3. 数据处理

订单模块数据处理流程如下表 3.6 所示：

### 表 3.6 订单模块处理过程表

| 序号 | 处理过程名称       | 输入数据                               | 输出数据               | 处理逻辑                                                                                                 |
| ---- | ------------------ | -------------------------------------- | ---------------------- | -------------------------------------------------------------------------------------------------------- |
| 1    | 1.1 订单创建与预览 | 用户 ID、民宿 ID、入住日期、退房日期   | 订单 ID、创建状态      | 用户提交预订请求，系统验证房源可用性，创建新订单，将订单数据保存到订单表，并向用户返回订单信息。         |
| 2    | 1.2 订单支付处理   | 订单 ID、支付方式、支付金额            | 支付状态               | 用户提交支付信息，系统向支付系统发送支付请求，处理支付结果，并更新订单状态。                             |
| 3    | 1.3 订单状态管理   | 订单 ID、操作类型(确认/拒绝/取消)      | 状态更新结果           | 处理订单状态的变更，如房东确认或拒绝订单、用户取消订单等，更新订单表中的状态信息。                       |
| 4    | 1.4 订单查询       | 查询条件(用户 ID/房东 ID/订单 ID/状态) | 订单列表/详情          | 根据不同用户角色(用户/房东/管理员)的查询请求，从订单表获取数据，并关联用户表和房源表的信息返回完整结果。 |
| 5    | 1.5 退款处理       | 订单 ID、退款原因                      | 退款状态、订单状态更新 | 处理订单退款请求，更新订单退款状态，调用支付系统的退款接口，并更新订单表中的相关信息。                   |

根据数据流图，订单模块的核心处理流程如下：

- **1.1 订单创建与预览**: 普通用户/客户提交预订请求，系统接收用户 ID、民宿 ID 及入住退房日期等信息，验证房源可用性(查询房源表)，然后创建订单并保存到订单表。流程包括从用户接收数据、验证房源、保存订单数据等步骤。

- **1.2 订单支付处理**: 用户提交支付信息，系统将支付请求发送给支付系统，接收支付结果回调，并更新订单状态。支付结果会影响订单状态管理过程。

- **1.3 订单状态管理**: 处理订单状态的变更，包括:

  - 支付系统回调的支付结果导致的状态更新
  - 房东确认或拒绝订单的状态更新
  - 用户取消订单的状态更新
    所有状态变更都会更新订单表中的数据。

- **1.4 订单查询**: 不同角色(普通用户/房东/管理员)可以查询订单信息:

  - 普通用户查询自己的订单
  - 房东查询与自己房源相关的订单
  - 管理员查询系统中的订单
    查询过程需要从订单表获取数据，并可能关联用户表和房源表获取完整信息。

- **1.5 退款处理**: 处理订单退款请求，主要由管理员操作，包括处理退款申请、调用支付系统退款接口、更新订单退款状态等步骤。退款处理会更新订单表中的状态，并可能触发通知流程。

这些处理过程通过数据流相互关联，共同实现订单管理的完整业务流程。比如订单创建后可能触发支付流程，支付完成后会更新订单状态，状态变更可能触发通知等。
