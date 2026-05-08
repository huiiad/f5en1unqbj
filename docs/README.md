# SSP SDK Android Documentation

本目录包含 SSP SDK 的官方文档，用于托管在 GitHub Pages 上。

## 目录结构

```
docs/
├── index.html              # 首页
├── index.md                # 文档首页
├── _config.yml             # Jekyll 配置
├── Gemfile                # Ruby 依赖
├── privacy/
│   ├── index.md           # 隐私权政策
│   └── developer-guide.md  # 开发者隐私指南
└── README.md               # 文档说明
```

## GitHub Pages 部署

本项目配置为使用 GitHub Pages 托管文档。文档将发布在：

**https://ssp-sdk.github.io/ssp-sdk-android/**

## 本地预览

如需在本地预览文档：

```bash
cd docs
bundle install
bundle exec jekyll serve
```

然后访问 http://localhost:4000

## 隐私政策

- [隐私权政策](./privacy/index.md)
- [开发者隐私合规指南](./privacy/developer-guide.md)

## 联系我厜

- 技术支持：support@ssp-sdk.com
- 隐私咨询：privacy@ssp-sdk.com
