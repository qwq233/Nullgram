# Set-up a domain

## Requirements

1. A domain you have full control with.
2. A [CloudFlare](https://cloudflare.com) account.

## At CloudFlare

1. Add your domain to CloudFlare.
2. Download and modify [records.txt](./records.txt), replace all `<Your domain>` with your actual domain.
3. Import DNS records from this file, make sure "Proxy imported DNS records" is checked.
4. Set SSL/TLS encryption mode in "SSL/TLS" section to Flexible.
