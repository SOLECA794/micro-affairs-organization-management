# D 部分交付说明

本目录配套根目录 `08-系统需求分析报告-D部分.md`，对应分工：

- G1：4.2 系统用例图；
- G2：4.3 四个核心用例描述；
- G14（可选）：1.1 项目背景与目标正式化。

## 文件

- `diagrams/student-use-case.dot|svg|png`：学生端用例图源文件与渲染图；
- `diagrams/manager-use-case.dot|svg|png`：社团负责人端用例图源文件与渲染图；
- `diagrams/admin-use-case.dot|svg|png`：系统管理员端用例图源文件与渲染图。

图形源文件采用 Graphviz DOT，方便后续修改。重新生成命令：

```bash
dot -Tsvg student-use-case.dot -o student-use-case.svg
dot -Tpng -Gdpi=180 student-use-case.dot -o student-use-case.png
```

其余两个图同理。

## 口径说明

仓库的 02 架构文档包含 Go 重构方案描述，但当前仓库实际后端源码为 Java/Spring Boot。D 部分涉及用例与真实业务行为，因此本交付以 01/04 需求与接口基线、当前 Java 实现、数据库脚本为联合依据。未将当前代码中不存在的“审核历史表”“签到场次”“confirm_time”等设计态能力写入核心用例结果。
