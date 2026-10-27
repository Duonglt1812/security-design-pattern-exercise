# BTVN: Thiet Ke Cho Bao Mat - Security Design Patterns

## De bai

Xay dung he thong gom 2 backend service User, Order va 1 API Gateway:

* **Routing:** Dinh tuyen /users va /orders toi dung service.
* **Offloading:** Kiem tra JWT, gioi han 10 request moi giay tai gateway.
* **Aggregation:** Endpoint /dashboard gop thong tin user va don hang.
* **Valet Key:** Upload anh dai dien len MinIO bang link presigned het han sau 5 phut.
* **Benchmark:** Do do tre khi di qua gateway so voi goi thang service.

---

## Huong dan chay

1. Khoi dong he thong:
```bash
docker compose up -d
```

2. Chay kiem thu va do do tre:
```bash
java benchmark/LatencyTest.java
```
