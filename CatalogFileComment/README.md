┌─────────────────────────────────────────┐
│           IntelliJ IDEA UI              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │ Project │  │ Comment │  │ Manager │ │
│  │  View   │  │ Dialog  │  │  Panel  │ │
│  └────┬────┘  └────┬────┘  └────┬────┘ │
│       └─────────────┴─────────────┘     │
│                   │                     │
│       ┌───────────┴───────────┐         │
│       │   FileCommentConfig   │         │
│       │       Service         │         │
│       └───────────┬───────────┘         │
│                   │                     │
│       ┌───────────┴───────────┐         │
│       │   CloudRuleService    │         │
│       │  (Memory→Disk→Cloud)  │         │
│       └───────────────────────┘         │
└─────────────────────────────────────────┘
复制

---

## 🛠️ 技术栈

- **Kotlin** - 主要开发语言
- **IntelliJ Platform SDK** - 插件开发框架
- **PSI (Program Structure Interface)** - 代码语法树分析
- **Gson** - JSON 解析
- **GitHub Gist** - 云端规则存储

---

## 📄 许可证

[MIT License](LICENSE)

---

<p align="center">
  <i>让每一个文件名都带上它的使命。Happy Coding! 🎉</i>
</p>