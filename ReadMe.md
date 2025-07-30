### Swagger доступен по адресу, при запущенном приложении:
http://localhost:8080/swagger-ui/index.html


### Работа с Docker
Запусти базу данных:
```shell
docker compose -f docker-compose.db.yml up -d
```
Затем приложение:
```shell
docker compose -f docker-compose.app.yml up -d
```

Данные для Postman:
POST http://localhost:8080/api/v1/wallets
```json
{
  "wallet_id": "11111111-1111-1111-1111-111111111112",
  "operation_type": "DEPOSIT",
  "amount": 1000.50
}
```
POST http://localhost:8080/api/v1/wallets/transfer
```json
{
  "from_wallet_id": "11111111-1111-1111-1111-111111111112",
  "to_wallet_id": "11111111-1111-1111-1111-111111111113",
  "amount": 1000.50
}
```
GET http://localhost:8080/api/v1/wallets/11111111-1111-1111-1111-111111111112