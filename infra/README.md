# CalmSkin Infrastructure Guide (Terraform & Kubernetes)

Tài liệu hướng dẫn vận hành và triển khai thực tế cho CalmSkin với **Terraform** và **Kubernetes (K8s)**.

---

## 1. Terraform (`infra/terraform/`)

Quản lý hạ tầng AWS S3 & IAM cho các dịch vụ tạo Pre-signed URL (`product-service`, `user-service`, `review-service`).

### Cấu trúc:
- `versions.tf`: Cấu hình Terraform >= 1.5.0 và AWS Provider ~> 5.0.
- `variables.tf`: Khai báo region (`ap-southeast-1`), môi trường (`dev`), tiền tố bucket và CORS.
- `main.tf`:
  - Tạo 3 S3 buckets: `calmskin-app-product-media-dev`, `calmskin-app-avatars-dev`, `calmskin-app-review-media-dev`.
  - Cấu hình CORS cho phép Frontend upload trực tiếp qua Pre-signed URL.
  - Tạo IAM User và Policy với quyền tối thiểu (`s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`).
- `outputs.tf`: Xuất `bucket_names`, `s3_access_key_id`, `s3_secret_access_key`.

### Cách chạy:
```bash
cd infra/terraform

# 1. Khởi tạo provider
terraform init

# 2. Kiểm tra cú pháp
terraform validate

# 3. Xem trước tài nguyên sẽ được tạo
terraform plan

# 4. Áp dụng tạo thật lên AWS (khi đã cấu hình aws configure)
terraform apply

# 5. Lấy thông tin S3 credentials để nạp vào K8s secrets/integrations.env
terraform output s3_access_key_id
terraform output -raw s3_secret_access_key
```

---

## 2. Kubernetes (`infra/k8s/base/`)

Quản lý triển khai toàn bộ hệ thống (5 backing services + 16 microservices) bằng **Kustomize**.

### Yêu cầu trước khi triển khai (Cluster Prerequisites):
- Default **StorageClass** (cho PersistentVolumeClaim của `postgres`, `kafka`, `elasticsearch`).
- **NGINX Ingress Controller** (cho `ingress.yaml`).
- **metrics-server** (cho HorizontalPodAutoscaler của `product-service` và `order-service`).

### Cấu trúc:
- `00-namespace.yaml`: Tạo Namespace `calmskin` cô lập toàn bộ ứng dụng.
- `01-configmap.yaml`: Lưu cấu hình chung (`EUREKA_URI`, Redis, Kafka, Elasticsearch, Zipkin, region S3/AWS và tên 12 databases `POSTGRES_DB_*`).
- `secrets/`:
  - `common.env.example` → copy thành `common.env` (git-ignored): `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `INTERNAL_SECRET` (sinh `Secret/calmskin-secrets`).
  - `integrations.env.example` → copy thành `integrations.env` (git-ignored): `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `GEMINI_API_KEY`, `CLOUDINARY_URL`, SMTP, VAPID, webhook secrets (sinh `Secret/calmskin-integrations`).
- `data/`: Backing services chạy trong cụm:
  - `postgres.yaml` (StatefulSet `pgvector/pgvector:pg16` + `postgres-init/01-create-databases.sh` tự động tạo 12 DB khi khởi tạo).
  - `redis.yaml` (Deployment `redis:7-alpine`).
  - `kafka.yaml` (StatefulSet `apache/kafka:3.7.0` chế độ KRaft, không cần ZooKeeper).
  - `elasticsearch.yaml` (StatefulSet `elasticsearch:8.13.0`).
  - `zipkin.yaml` (Deployment `openzipkin/zipkin:3`).
- `apps/`: Toàn bộ 16 microservices:
  - Core & Discovery: `discovery-service` (8761), `api-gateway` (8080).
  - Domain services: `user-service` (8081), `product-service` (8082, HPA), `inventory-service` (8083), `order-service` (8084, HPA), `payment-service` (8085), `review-service` (8086), `promotion-service` (8087), `notification-service` (8088), `shipping-service` (8089), `subscription-service` (8092), `search-service` (8093).
  - AI services: `ai-chatbot-service` (8091), `ai-skin-analysis-service` (8095), `ai-recommendation-service` (8096).
- `ingress.yaml`: NGINX Ingress Controller routing traffic vào `api-gateway`.
- `kustomization.yaml`: Ghép toàn bộ manifests, `configMapGenerator` (postgres-init) và `secretGenerator` (`calmskin-secrets`, `calmskin-integrations`).

### Cách triển khai lên cụm K8s (Minikube / Kind / K3s / EKS):
```bash
# 1. Tạo file biến môi trường bí mật từ file mẫu (git-ignored, không bao giờ commit)
cp infra/k8s/base/secrets/common.env.example infra/k8s/base/secrets/common.env
cp infra/k8s/base/secrets/integrations.env.example infra/k8s/base/secrets/integrations.env
# -> Chỉnh sửa common.env và integrations.env với giá trị thật

# 2. Xem toàn bộ YAML đã được Kustomize ghép lại
kubectl kustomize infra/k8s/base

# 3. Deploy toàn bộ hệ thống vào cụm K8s chỉ bằng 1 lệnh:
kubectl apply -k infra/k8s/base

# 4. Kiểm tra trạng thái pods, statefulsets, services và HPA
kubectl get pods,sts,svc,pvc,hpa -n calmskin

# 5. Xem log của một service bất kỳ (ví dụ: order-service)
kubectl logs -f deployment/order-service -n calmskin

# 6. Xóa toàn bộ ứng dụng khi cần
kubectl delete -k infra/k8s/base
```
