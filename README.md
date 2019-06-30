# Turms

Turms是一款完全免费开源、易于集群部署、健壮的、方便拓展且业务独立的Java即时通信服务端程序。

## 配套组合

| 范围        | 类别               | 名称                                                         |
| ----------- | ------------------ | ------------------------------------------------------------ |
| Turms服务端 | 自定义插件实现接口 | [turms-plugin](https://github.com/turms-im/turms-plugin)     |
| Turms服务端 | 集群监控           | [turms-admin](https://github.com/turms-im/turms-admin)       |
| Turms客户端 | 通讯能力库         | [turms-client-js](https://github.com/turms-im/turms-client-js) |
| Turms客户端 | 通讯能力库         | [turms-client-dart](https://github.com/turms-im/turms-client-dart) |
| Turms客户端 | UI组件             | [turms-client-ui-flutter](https://github.com/turms-im/turms-client-ui-flutter) |
| Turms客户端 | UI组件             | [turms-client-flutter-demo]()                                |

## 主要特性

1. 功能完善性。Turms支持几乎所有商用即时通信产品所支持的即时通信相关功能，并且无任何增值功能收费，无需提交工单以申请功能，无业务功能限制。
2. 功能拓展性。Turms同时支持两种拓展模式。配置参数；自定义Plugin插件。当然您也完全可以对源码进行修改。未来接入ElasticSearch集群部分就将基于Plugin实现。
3. 配置灵活性。Turms提供了上百个配置参数供用户定制，以满足各种即时通信场景。
4. 部署简易性。Turms集群中仅有Turms服务端节点，默认无需为第三方服务端部署服务器。
5. 集群健壮性。Turms服务端通过Hash Slot与选举算法来保证在部分集群节点宕机的情况下，Turms集群仍能正常运作。
6. 存储可靠性。在每个Turms节点服务器中，都搭配了CockRoachDB用于消息存储与容灾，即高效又可靠。
7. 运作高效性。Turms服务端与Turms客户端间的所有通信数据均为Protobuf二进制数据。同时充分利用内存，通过本地缓存高效运作。
8. 业务可插拔性。Turms服务端集群独立于您的业务逻辑服务端。

## 业务功能列表（非完全）

1. 此功能列表参考了：网易云信、环信、融云、LeanCloud、腾讯云通讯等商用即时通信服务。Turms提供了几乎所有这些商业服务所提供的业务功能，并在一些方面更上一层楼。
2. 本功能列表仅为功能概述。如果您需要了解具体实现的参数配置、完整方案与原理，请查阅对应的Wiki内容。
3. 注意：Turms的功能配置参数极其自由，您甚至可以配置一个群组上限成员数量为10,000，单个消息上限100MB，关闭大部分业务功能等等的配置，拓展将消息转发给所有的用户等等的功能，Turms服务端不会干涉您满足任何的业务场景。
   Turms只是为您提供了最通用的配置，如默认一个群的上限人数为500，单个消息最大可为1MB等等。
4. 如果您未在此列表中找到您所需要的功能，请先检查是否您的需求仅需配置Turms参数即可实现。确认无法通过Turms配置参数实现后，请再在Issue区域提出。Turms会根据“性价比”进行评估，并尽可能满足您的需求。

### 多端登录类型

| **类型**             | **功能描述**                                                 | **相关配置** | **实现版本** |
| :------------------- | ------------------------------------------------------------ | ------------ | :----------- |
| 单端登录             | 仅允许 Windows、Web、Android 或 iOS 单端登录                 |              | 0.8.0        |
| 双端登录（默认状态） | 允许 Windows、Android 或 iOS 单端登录，同时允许与 Web 端同时在线 |              | 0.8.0        |
| 三端登录             | 允许 Android 或 iOS 单端登录，同时允许与 Windows 和 Web 端同时在线 |              | 0.8.0        |
| 多端同时在线         | 允许 Windows、Web、Android 或 iOS 多端或全端同时在线登录     |              | 0.8.0        |

注意：

- 任何多端登陆类型都不允许一位用户在一种设备上有多个同时登陆的设备
- 当用户登陆设备的类型有Unkown或Others情况时，需进行额外配置：允许Unkown/Others设备与其他已知设备同时登陆、不允许Unkown/Others设备与其他已知设备同时登陆

### 多端登陆冲突解决策略

| **类型**           | **功能描述**                                       | **相关配置** | **实现版本** |
| :----------------- | -------------------------------------------------- | ------------ | :----------- |
| 已上线设备掉线     | 已上线的设备掉线                                   |              | 0.8.0        |
| 请求已上线设备确认 | 已上线的一方获得通知，同意或拒绝预上线设备上线请求 |              | 0.8.0        |
| 预上线设备上线失败 | 准备上线的一方直接上线失败                         |              | 0.8.0        |

### 消息类型

提醒：虽然Turms服务端默认支持传递图片、视频、文件等数据的功能，但并不推荐使用此实现方案。
推荐的实现方案是使用CDN技术，客户端将文件上传到CDN后，再将从CDN那获得的文件URL传递给Turms服务端，由Turms保存这个URL文本，而不保留文件的二进制数据。或采用未来Turms提供的小文件管理插件来部署与管理文件服务集群。

| **消息类型** | **功能描述**                                                 | **相关配置** | **实现版本** |
| :----------- | :----------------------------------------------------------- | ------------ | :----------- |
| 文本消息     | 消息内容为普通文本                                           |              | 0.8.0        |
| 富文本消息   | 消息内容为普通文本与其他任何类型（除了系统消息类型）的消息   |              | 0.8.0        |
| 图片消息     | 消息内容为图片 URL 地址、尺寸、图片大小、图片具体数据等信息  |              | 0.8.0        |
| 语音消息     | 消息内容为语音文件的 URL 地址、时长、大小、格式、语音数据等信息； 语音消息最大时长默认为 120 秒 |              | 0.8.0        |
| 视频消息     | 消息内容为语音文件的 URL 地址、时长、大小、格式、视频数据等信息 |              | 0.8.0        |
| 文件消息     | 消息内容为文件的 URL 地址、大小、格式、文件数据等信息，格式不限，默认最大支持 50 MB |              | 0.8.0        |
| 地理位置消息 | 消息内容为地理位置标题、地址、经度、纬度信息                 |              | 0.8.0        |
| 通知消息     | 主要用于群事件的通知和群组事件的通知，可选有无推送和通知栏提醒（此部分为客户端内容） |              | 0.8.0        |
| 自定义消息   | 开发者自定义的消息类型，例如红包消息、石头剪子布等形式的消息 |              | 0.8.0        |
| 系统消息     | 以上所有消息类型均可作为系统消息使用                         |              | 0.8.0        |

### 消息功能

| **消息功能** | **功能描述**                                                 | **相关配置** | **实现版本** |
| :----------- | :----------------------------------------------------------- | ------------ | :----------- |
| 离线消息     | 用户不在线时收到的消息，会在用户下次登录时，Turms服务端会自动将用户离线期间收取（懒加载）的消息下发到客户端。 默认情况下，Turms服务端<不会>定时删除任何用户发送过的任何类型的消息 |              | 0.8.0        |
| 漫游消息     | 在新设备登录时，将服务器记录的漫游消息同步下来。默认同步最近 7 天的所有消息（懒加载） |              | 0.8.0        |
| 多端同步     | 多客户端同时在线时，消息实时下发到多端                       |              | 0.8.0        |
| 历史消息     | 支持本地历史消息和云端历史消息。默认存储最近 3 年的云端历史消息 |              | 0.8.0        |
| 消息备份     | 支持备份本地历史消息和云端历史消息                           |              | 0.8.0        |
| 消息撤回     | 撤回投递成功的消息，默认允许发信人撤回距投递成功时间 2 分钟内的消息 |              | 0.8.0        |
| 阅后即焚     | 收信人接收到发信人的消息后，收信人客户端会根据发信人预先设定（或默认）的时间按时自动销毁 |              | 0.8.0        |
| 已读回执     | 查看私聊、群组会话中对方的已读/未读状态                      |              | 0.8.0        |
| 消息转发     | 将消息转发给其他用户或群组                                   |              | 0.8.0        |
| @某人        | 用于特别提醒某人的意思，被@的人会收到特别提醒的通知。群内 @ 消息与普通消息没有本质区别，仅是在被 @ 的人在收到消息时，需要做特殊处理 |              | 0.8.0        |
| 正在输入     | 当通信中的一方正在键入文本时，告知收信人（一名或多名用户），该用户正在输入消息 |              | 0.8.0        |

### 用户资料功能

| 功能类型     | 功能描述                       |
| :----------- | :----------------------------- |
| 设置用户资料 | 用户设置自己的昵称、拓展字段   |
| 获取用户资料 | 用户查看自己、好友及陌生人资料 |

### 用户关系托管

注意：在Turms中，某用户的联系人可以是被其加入黑名单的用户。联系人仅用于表示该用户所拥有的“联系人(Contacts)”这一概念，而不指代其间的其他关系。

| **功能**                 | **功能描述**                                                 | **相关配置** | **实现版本**     |
| :----------------------- | :----------------------------------------------------------- | ------------ | :--------------- |
| 查找联系人               | 可通过用户帐号 ID 查找联系人<br />（补充：由于通过昵称来查找联系人功能的实现较复杂，因此放到0.9.0版本实现，预计采用AC自动机算法） |              | 0.8.0<br />0.9.0 |
| 添加联系人               | 支持直接添加为联系人或发起联系人验证请求。成功添加联系人后，默认允许双方发送消息 |              | 0.8.0            |
| 通过/拒绝联系人请求      | 收到请求添加联系人请求的系统通知后，可以通过或者拒绝         |              | 0.8.0            |
| 删除联系人               | 默认情况下，删除联系人后，双方依然可以发送消息               |              | 0.8.0            |
| 获取联系人列表           | 获取用户的通讯录                                             |              | 0.8.0            |
| 加入黑名单               | 将用户加入黑名单后，将不再收到对方发来的任何消息或者请求。将某用户加入黑名单时，默认其仍在联系人列表中 |              | 0.8.0            |
| 移出黑名单               | 将用户移出黑名单                                             |              | 0.8.0            |
| 黑名单列表               | 获取黑名单列表                                               |              | 0.8.0            |
| 判断用户是否被拉进黑名单 | 判断用户是否在自己的黑名单内                                 |              | 0.8.0            |
| 创建好友分组             | 创建分组时，可以同时指定添加的用户，同一用户可以添加到多个分组 |              | 0.8.0            |
| 删除好友分组             | 删除好友分组                                                 |              | 0.8.0            |
| 添加好友到某分组         | 将好友添加到好友分组                                         |              | 0.8.0            |
| 从某分组删除好友         | 将好友从好友分组中删除                                       |              | 0.8.0            |
| 重命名好友分组           | 重命名好友分组                                               |              | 0.8.0            |
| 获取指定好友分组信息     | 获取指定的好友分组                                           |              | 0.8.0            |
| 获取所有好友分组         | 获取所有分组信息                                             |              | 0.8.0            |

### 群组功能

在Turms中，不对群组、聊天室等概念做区分，Turms统一使用“群组”这一概念。用户可在创建群组时，配置各种各样的群组业务功能，以定制各种类型的“群组”。

| **功能**           | **功能描述**                                                 | **相关配置** | **实现版本**     |
| :----------------- | :----------------------------------------------------------- | ------------ | :--------------- |
| 群容量             | 默认群组上线人数为 500 人/群，无上限                         |              | 0.8.0            |
| 群资料属性         | 群名，群头像，群简介，群公告，群扩展字段                     |              | 0.8.0            |
| 查找群组           | 可通过群组 ID 查找群组<br />（补充：由于通过昵称来查找群组功能的实现较复杂，因此放到0.9.0版本实现，预计采用AC自动机算法） |              | 0.8.0<br />0.9.0 |
| 邀请入群           | 支持配置：仅群主可邀请、仅群主与管理员可邀请、群主+管理员与群成员可邀请、所有人可邀请 |              | 0.8.0            |
| 被邀请人同意模式   | 支持配置：需要被邀请人同意、不需要被邀请人同意               |              | 0.8.0            |
| 申请入群           | 支持                                                         |              | 0.8.0            |
| 入群验证           | 支持配置：群主与管理员审批入群请求、入群请求者回答问题正确后加入、允许任何人加入、不允许任何人加入 |              | 0.8.0            |
| 群成员类型         | 群主、管理员、普通成员、游客、匿名游客                       |              | 0.8.0            |
| 管理员设置         | 群主可以增减管理员                                           |              | 0.8.0            |
| 群组资料修改权限   | 支持配置：仅群主可修改、仅群主与管理员可修改、仅群主+管理员与群成员可修改、所有人可修改 |              | 0.8.0            |
| 群扩展字段修改权限 | 支持配置：仅群主可修改、仅群主与管理员可修改、仅群主+管理员与群成员可修改、所有人可修改 |              | 0.8.0            |
| 修改群成员昵称     | 群主可以修改所有人的群昵称。管理员只能修改普通群成员的群昵称。 |              | 0.8.0            |
| 修改自己的群昵称   | 支持                                                         |              | 0.8.0            |
| 消息类型支持       | 同基础消息类型                                               |              | 0.8.0            |
| 消息提醒           | 接收提醒、只接收管理员消息提醒、不接收提醒                   |              | 0.8.0            |
| 历史消息           | 支持云端历史消息，默认服务端存储最近 1 年的历史记录          |              | 0.8.0            |
| 离线消息           | 每个群聊会话最多下发 500 条离线消息（懒加载）                |              | 0.8.0            |
| 消息漫游           | 默认同步最近 7 天的所有消息（懒加载）                        |              | 0.8.0            |
| 指定成员强制推送   | 支持                                                         |              | 0.8.0            |
| 群消息已读回执     | 支持                                                         |              | 0.8.0            |
| 踢人               | 群主和管理员可以踢人，且管理员不能踢群主和其他管理员         |              | 0.8.0            |
| 主动退群           | 除群主外，其他用户均可以主动退群。群主需先将群转让给其他群成员才可以进行退群操作 |              | 0.8.0            |
| 转让群             | 群主可以将群的拥有者权限转给群内的其他成员，转移后， 被转让者变为新的群主，原群主变为普通成员。群主还可以选择在转让的同时，直接退出该群。 |              | 0.8.0            |
| 解散群             | 群主可以解散群                                               |              | 0.8.0            |
| 修改群组开/关状态  | 修改群组开/关闭状态，仅支持从服务端修改                      |              | 0.8.0            |
| 群组黑名单         | 用户被拉黑后，将无法再进入群组。如果被拉黑用户在被拉黑之前是当前群组成员，则在拉黑后该用户会自动在群组成员列表中移除 |              | 0.8.0            |
| 群组成员禁言       | 禁言用户可以在群组内，但无法发送消息                         |              | 0.8.0            |
| 群组临时禁言       | 群组支持设置临时禁言时长，禁言时长时间到了，自动取消禁言。   |              | 0.8.0            |
| 群组永久禁言       | 设置群组整体禁言状态，仅创建者和管理员能发言                 |              | 0.8.0            |

## 管理功能列表（非完全）

此表所述功能并不包括turms-plugin与turms-admin（可视化）所提供的功能，此表功能仅包括Turms服务端自身通过RESTful API（JSON通信）所提供的功能。具体API接口文档，请查阅[Turms服务端集成开发文档]()

**注意：此表所述接口仅供管理员使用，而不应该被用户使用。因为这些操作不在业务层上做权限判断，如场景：用户A请求添加B为联系人。在业务层中，是需要判断用户A是否有权限添加用户B的。但在下表所述接口中，它们不会做业务权限判断，而直接执行添加联系人操作（因为无需对管理员进行业务层权限判断）**

### 管理员权限管理

| **功能**       | **功能说明** | **URL**        | **实现版本** |
| :------------- | :----------- | -------------- | ------------ |
| 管理员登陆     |              | GET /admins    | 0.8.0        |
| 新建管理员     |              | POST /admins   | 0.8.0        |
| 删除管理员     |              | DELETE /admins | 0.8.0        |
| 管理员权限控制 |              |                | 0.9.0        |

### 消息管理

| **功能**     | **功能说明**                                 | **URL**          | **实现版本** |
| :----------- | :------------------------------------------- | ---------------- | ------------ |
| 消息下载     | 下载指定时间段的所有单发或群组消息记录       | GET /messages    | 0.8.0        |
| 消息全文检索 |                                              | GET /messages    | 1.x          |
| 发送消息     | 可发送的消息类型同“业务功能列表”中的消息类型 | POST             | 0.9.0        |
| 删除消息     |                                              | DELETE /messages | 0.8.0        |
| 修改消息     |                                              | PUT /messages    | 0.9.0        |

### 用户管理

| **功能**         | **功能说明**                                                 | **URL**             | **实现版本** |
| :--------------- | :----------------------------------------------------------- | ------------------- | ------------ |
| 获取用户信息     |                                                              | GET /users          | 0.8.0        |
| 获取用户在线状态 |                                                              | GET /users/statuses |              |
| 增加用户账号     |                                                              | POST /users         | 0.8.0        |
| 删除用户账号     |                                                              | DELETE /users       | 0.8.0        |
| 修改用户信息     | 修改用户信息。包括禁用/解禁用户                              | PUT /users          | 0.8.0        |
| 修改用户在线状态 | 修改用户在线状态，可用于强制用户下线。不可修改下线用户的在线状态 | PUT /users/statuses | 0.8.0        |

### 用户关系管理

| **功能**     | **描述**               | **URL**                  | **实现版本** |
| :----------- | :--------------------- | :----------------------- | ------------ |
| 获取好友列表 | 获取好友列表以及信息   | GET /users/contacts      | 0.8.0        |
| 获取黑名单   | 获取黑名单以及信息     | GET /users/blacklists    | 0.8.0        |
| 添加好友     | 添加为好友             | POST /users/contacts     | 0.8.0        |
| 移除好友     | 移除好友列表中的用户   | DELETE /users/contacts   | 0.8.0        |
| 添加黑名单   | 拉黑用户，添加至黑名单 | POST /users/blacklists   | 0.8.0        |
| 移除黑名单   | 除黑名单中的用户       | DELETE /users/blacklists | 0.8.0        |

### 群组管理

| **功能**                   | **描述**                               | **URL**                   | **实现版本** |
| :------------------------- | :------------------------------------- | :------------------------ | ------------ |
| 获取所有的群组（可分页）   | 获取全部的群组信息                     | GET /groups               | 0.8.0        |
| 获取一个用户参与的所有群组 | 根据用户 ID 获取此用户加入的全部群组   | GET /users/groups         | 0.8.0        |
| 获取群组信息               | 根据群组 ID 获取此群组的信息           | GET /groups               | 0.8.0        |
| 创建一个群组               | 创建一个新群组                         | POST /groups              | 0.8.0        |
| 修改群组信息               | 修改群组的信息。包括转让群主身份等操作 | PUT /groups               | 0.8.0        |
| 删除群组                   | 删除一个群组                           | DELETE /groups/{group_id} | 0.8.0        |
| 添加群组禁言               | 使整个群组进入禁言状态                 | PUT /groups               | 0.8.0        |
| 移除群组禁言               | 使整个群组解除禁言状态                 | PUT /groups               | 0.8.0        |

### 群组成员管理

| **功能**           | **描述**                                       | **URL**                | **实现版本** |
| :----------------- | :--------------------------------------------- | :--------------------- | ------------ |
| 获取群组成员       | 获取一个群组的群成员列表。可获取指定身份的成员 | GET /groups/members    | 0.8.0        |
| 获取被禁言成员列表 | 获取群组的被禁言成员列表                       | GET /groups/members    | 0.8.0        |
| 添加群组成员       | 添加用户至群组成员列表。可添加指定身份的成员   | POST /groups/members   | 0.8.0        |
| 移除群组成员       | 从群组成员列表中移除用户。可删除指定身份的成员 | DELETE /groups/members | 0.8.0        |
| 修改群组成员信息   | 修改群组成员信息                               | PUT /groups/members    | 0.8.0        |
| 添加禁言           | 添加用户至群组的禁言列表                       | PUT /groups/members    | 0.8.0        |
| 移除禁言           | 从群组的禁言列表中移除用户                     | PUT /groups/members    | 0.8.0        |

### 群组黑名单管理

| **功能**                 | **描述**                     | **URL**                   | **实现版本** |
| ------------------------ | ---------------------------- | ------------------------- | ------------ |
| 查询群组黑名单           | 查看群组的黑名单列表         | GET /groups/blacklists    | 0.8.0        |
| 添加单个用户至群组黑名单 | 将用户添加至群组的黑名单列表 | POST /groups/blacklists   | 0.8.0        |
| 批量从群组黑名单移除用户 | 将用户从黑名单列表中移除     | DELETE /groups/blacklists | 0.8.0        |

## RoadMap：

| **功能**           | **实现概要**                                                 | **说明**                                       | **实现版本** |
| :----------------- | ------------------------------------------------------------ | :--------------------------------------------- | :----------- |
| 支持插件           | 用户实现TurmsPlugin抽象类的子类，并生成jar包。Turms读取用户自定义jar包以使插件生效 | 支持插件                                       | 0.9.0        |
| 附近的人           | Turms服务端利用优化后的Geohash算法（待定）在本地内存中实现   | 附近的人                                       | 1.x          |
| 服务端用户消息查询 | 基于TurmsClientMessagePlugin类，引入ElasticSearch集群实现    | 开发者通过这套接口来快速查询数据库中的用户消息 | 1.x          |
| 敏感词过滤         |                                                              | 敏感词过滤                                     |              |
| 分布式动态配置     | 基于Raft算法实现                                             | 分布式动态配置                                 | 1.x          |
| 机器人             |                                                              | 机器人                                         | 2.x          |
| 文件集群管理       | 基于TurmsClientMessagePlugin类，引入seaweedfs（待定）集群实现 | 文件集群管理                                   | 3.x          |
| 音频通话           | 引入SRS集群（待定）实现                                      | 音频通话                                       | 4.x          |
| 视频通话           | 引入SRS集群（待定）实现                                      | 视频通话                                       | 4.x          |
| 语音识别           |                                                              | 语音识别                                       | 5.x          |
| 跨集群通信         |                                                              | 跨集群通信                                     | x.x          |

### 其他：

- 提供更完善的开发者文档
- 完善状态码
- 为各种即时通信业务场景提供默认配置
- 提高单元测试覆盖率到80%

## 使用方法

## 其他Wiki文档资料

### Turms服务端配置参数

### Turms服务端状态码

### Turms服务端集成开发文档（RESTful API接口）
