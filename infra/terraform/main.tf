locals {
  buckets = {
    products = "${var.project_prefix}-product-media-${var.environment}"
    avatars  = "${var.project_prefix}-avatars-${var.environment}"
    reviews  = "${var.project_prefix}-review-media-${var.environment}"
  }
}

# 1. Tạo 3 S3 Buckets cho 3 microservices
resource "aws_s3_bucket" "media" {
  for_each = local.buckets
  bucket   = each.value
}

# 2. Chặn public access trực tiếp, chỉ cho phép thông qua IAM / Pre-signed URL hoặc CloudFront
resource "aws_s3_bucket_public_access_block" "media_pab" {
  for_each = aws_s3_bucket.media
  bucket   = each.value.id

  block_public_acls       = true
  block_public_policy     = false
  ignore_public_acls      = true
  restrict_public_buckets = false
}

# 3. Cấu hình CORS để frontend upload trực tiếp qua Pre-signed URL
resource "aws_s3_bucket_cors_configuration" "media_cors" {
  for_each = aws_s3_bucket.media
  bucket   = each.value.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "HEAD"]
    allowed_origins = var.cors_allowed_origins
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# 4. IAM User riêng cho các service của CalmSkin
resource "aws_iam_user" "calmskin_s3_user" {
  name = "${var.project_prefix}-s3-app-${var.environment}"
}

# 5. IAM Policy cấp quyền tối thiểu (Least Privilege)
resource "aws_iam_user_policy" "calmskin_s3_policy" {
  name = "${var.project_prefix}-s3-access-policy"
  user = aws_iam_user.calmskin_s3_user.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = [
          for b in aws_s3_bucket.media : "${b.arn}/*"
        ]
      }
    ]
  })
}

# 6. Tạo Access Key để nạp vào .env hoặc K8s Secret
resource "aws_iam_access_key" "calmskin_s3_key" {
  user = aws_iam_user.calmskin_s3_user.name
}
