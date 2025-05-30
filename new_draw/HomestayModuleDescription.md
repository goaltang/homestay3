# 民宿模块数据流图说明

## 1. 民宿模块 0 层数据流图

民宿模块 0 层数据流图描述了民宿管理系统的整体数据流动，如图 4.1 所示。该图展示了系统与外部实体（普通用户/客户、房东和管理员）之间的数据交互，以及系统与数据存储（民宿表和评价表）之间的数据流动。

### 1.1 外部实体

- **普通用户/客户**：系统的主要使用者，可以搜索和查看民宿信息，提交对已入住民宿的评价。
- **房东**：民宿的拥有者，负责创建和管理民宿信息，回复用户评价。
- **管理员**：系统管理者，负责审核民宿信息，管理评价内容，维护系统正常运行。

### 1.2 主要数据流

- **普通用户/客户 → 民宿管理系统**：用户发送搜索和查看民宿的请求，提交对民宿的评价。
- **房东 → 民宿管理系统**：房东创建和更新民宿信息，管理民宿状态，回复用户评价。
- **管理员 → 民宿管理系统**：管理员审核民宿信息，管理评价内容，设置推荐民宿。
- **民宿管理系统 → 民宿表**：系统存储和更新民宿信息。
- **民宿表 → 民宿管理系统**：系统读取民宿数据以响应用户查询。
- **民宿管理系统 → 评价表**：系统存储用户提交的评价和房东的回复。
- **评价表 → 民宿管理系统**：系统读取评价数据以展示给用户和房东。

## 2. 民宿模块 1 层数据流图

民宿模块 1 层数据流图（图 4.2）展示了民宿管理系统内部的五个主要处理过程，以及它们与外部实体和数据存储之间的详细数据流动。

### 2.1 处理过程

- **1.1 民宿查询与搜索**：处理用户的搜索和查询请求，从数据库获取符合条件的民宿信息。
- **1.2 民宿创建与编辑**：处理房东创建和编辑民宿信息的请求，验证数据有效性并更新数据库。
- **1.3 民宿状态管理**：处理民宿状态变更，如上线、下线、审核等操作。
- **1.4 评价管理**：管理用户评价和房东回复，包括创建、审核和展示评价。
- **1.5 民宿统计与推荐**：分析民宿数据，生成统计信息，设置推荐民宿。

### 2.2 数据存储

- **民宿表(homestays)**：存储民宿的基本信息，如 ID、标题、描述、地址、价格、状态等。
- **评价表(reviews)**：存储用户对民宿的评价信息，包括评分、评价内容、房东回复等。
- **设施表(amenities)**：存储民宿可能具有的各类设施信息。
- **用户表(users)**：存储用户基本信息，关联普通用户和房东信息。

### 2.3 主要数据流

- **用户搜索请求 → 1.1 民宿查询与搜索**：用户提交搜索条件，如位置、价格范围、日期等。
- **1.1 民宿查询与搜索 → 民宿表**：系统查询符合条件的民宿数据。
- **民宿表 → 1.1 民宿查询与搜索**：返回查询结果。
- **1.1 民宿查询与搜索 → 用户**：向用户展示民宿列表或详情。
- **房东 → 1.2 民宿创建与编辑**：房东提交民宿信息，如标题、描述、价格、地址、设施等。
- **1.2 民宿创建与编辑 → 民宿表**：系统将验证后的民宿信息存入数据库。
- **1.2 民宿创建与编辑 → 设施表**：管理民宿关联的设施信息。

- **房东/管理员 → 1.3 民宿状态管理**：提交状态变更请求，如上线、下线、审核等。
- **1.3 民宿状态管理 → 民宿表**：更新民宿状态信息。

- **用户/房东 → 1.4 评价管理**：用户提交评价，房东回复评价。
- **1.4 评价管理 → 评价表**：存储评价和回复数据。
- **评价表 → 1.4 评价管理**：读取评价数据。

- **管理员 → 1.5 民宿统计与推荐**：管理推荐民宿设置。
- **1.5 民宿统计与推荐 → 民宿表**：更新民宿推荐状态。

## 3. 小结

民宿模块的数据流图展示了系统中数据如何在用户、房东、管理员以及各个数据存储之间流动。0 层图提供了系统整体视图，而 1 层图则展示了系统内部更详细的处理过程和数据流。通过这些图，可以清晰地了解民宿信息的创建、管理、搜索和评价等核心功能的数据流动过程。
