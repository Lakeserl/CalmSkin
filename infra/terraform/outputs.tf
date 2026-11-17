output "bucket_names" {
  description = "Tên S3 buckets để nạp vào cấu hình service"
  value = {
    for k, b in aws_s3_bucket.media : k => b.bucket
  }
}

output "s3_access_key_id" {
  description = "S3_ACCESS_KEY cho các microservices"
  value       = aws_iam_access_key.calmskin_s3_key.id
}

output "s3_secret_access_key" {
  description = "S3_SECRET_KEY cho các microservices"
  value       = aws_iam_access_key.calmskin_s3_key.secret
  sensitive   = true
}
