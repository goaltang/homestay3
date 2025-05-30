# 民宿模块数据存储和数据处理

## 1. 数据存储

民宿模块数据存储如下表 4.5 所示：

### 表 4.5 民宿模块数据存储表

| 序号 | 数据存储名称         | 描述                 | 组成                                                       | 主键    |
| ---- | -------------------- | -------------------- | ---------------------------------------------------------- | ------- |
| 1    | D1 民宿表(homestays) | 存储民宿房源全部信息 | 民宿 ID+房东 ID+标题+描述+地址+价格+状态+创建时间+设施信息 | 民宿 ID |
| 2    | D2 评价表(reviews)   | 存储评价相关信息     | 评价 ID+用户 ID+民宿 ID+评分+内容+回复+创建时间            | 评价 ID |
| 3    | D3 设施表(amenities) | 存储民宿设施信息     | 设施 ID+设施名称+设施描述+图标                             | 设施 ID |
| 4    | D4 用户表(users)     | 存储用户基本信息     | 用户 ID+用户名+密码+电话+邮箱+角色+注册时间                | 用户 ID |

该模块主要涉及四个核心数据存储：

- **D1 民宿表(homestays)**: 存储所有民宿房源的详细信息，包括民宿 ID、房东 ID、标题、描述、地址、价格、状态、创建时间和设施信息等。主键为民宿 ID。
- **D2 评价表(reviews)**: 存储用户对民宿的评价信息，包括评分、评价内容、房东回复等。主键为评价 ID。
- **D3 设施表(amenities)**: 存储民宿可能具有的各类设施，用于民宿描述和筛选。主键为设施 ID。
- **D4 用户表(users)**: 存储用户信息，用于关联普通用户和房东信息。主键为用户 ID。

## 2. 数据处理

民宿模块数据处理流程如下表 4.6 所示：

### 表 4.6 民宿模块处理过程表

| 序号 | 处理过程名称       | 输入数据                           | 输出数据              | 处理逻辑                                                                                   |
| ---- | ------------------ | ---------------------------------- | --------------------- | ------------------------------------------------------------------------------------------ |
| 1    | 1.1 民宿查询与搜索 | 搜索条件(位置/价格/日期/设施/人数) | 民宿列表/民宿详情     | 根据用户提供的搜索条件查询民宿表和设施表，返回符合条件的民宿列表或特定民宿的详细信息。     |
| 2    | 1.2 民宿创建与编辑 | 民宿信息(标题/描述/价格/地址/设施) | 民宿 ID/创建状态      | 房东提交民宿信息，系统验证有效性后创建新民宿记录，或更新已有民宿信息，存入民宿表。         |
| 3    | 1.3 民宿状态管理   | 民宿 ID、状态(上线/下线/审核中)    | 状态更新结果          | 处理民宿状态变更，如房东上线/下线房源、管理员审核房源等，更新民宿表中的状态信息。          |
| 4    | 1.4 评价管理       | 评价内容/评分/民宿 ID/用户 ID      | 评价创建结果/评价列表 | 管理用户对民宿的评价，包括创建新评价、房东回复评价、管理员审核评价等，更新评价表中的数据。 |
| 5    | 1.5 民宿统计与推荐 | 查询条件(热门/好评/新上线)         | 民宿推荐列表/统计数据 | 分析民宿数据和评价数据，生成民宿统计信息，推荐热门或符合特定条件的民宿给用户。             |

根据数据流图，民宿模块的核心处理流程如下：

- **1.1 民宿查询与搜索**: 普通用户提交搜索请求，系统根据提供的搜索条件从民宿表和设施表中查询数据，返回符合条件的民宿列表或特定民宿的详情。流程包括接收搜索条件、筛选数据、返回结果等步骤。

- **1.2 民宿创建与编辑**: 房东提交民宿信息，系统验证信息的有效性，将数据保存到民宿表中，同时设置相应的设施信息。这个过程包括数据验证、创建或更新民宿记录、关联设施信息等步骤。

- **1.3 民宿状态管理**: 房东和管理员可以管理民宿状态，如上线、下线、审核等。系统接收状态变更请求，更新民宿表中的状态字段，可能还会记录状态变更历史。

- **1.4 评价管理**: 用户可以对已入住过的民宿提交评价，房东可以回复评价，管理员可以审核评价。系统处理评价数据，存储到评价表中，并可能更新民宿表中的评分信息。

- **1.5 民宿统计与推荐**: 系统分析民宿和评价数据，生成统计信息，如热门民宿、好评民宿等。管理员可以基于这些数据设置推荐民宿，帮助用户更容易找到满意的住所。
