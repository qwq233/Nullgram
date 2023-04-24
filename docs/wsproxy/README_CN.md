# 部署教程

## 需求

1. 具有完整控制权的域名
2. [CloudFlare](https://cloudflare.com) 账号

## At CloudFlare

1. 将你的域名添加至 [CloudFlare](https://cloudflare.com).
2. 下载并修改 [records.txt](./records.txt), 将 `<Your domain>` 替换为你的域名
3. 从该文件导入DNS记录, 确认 "代理导入的DNS记录" 已勾选
4. 设置"SSL/TLS"选栏中的`SSL/TLS 加密模式`至灵活模式.
