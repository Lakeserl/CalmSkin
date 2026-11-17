variable "aws_region" {
  description = "AWS region for S3 buckets"
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "project_prefix" {
  description = "Unique prefix for globally unique S3 bucket naming"
  type        = string
  default     = "calmskin-app"
}

variable "cors_allowed_origins" {
  description = "Allowed origins for pre-signed direct S3 upload"
  type        = list(string)
  default     = ["http://localhost:3000", "http://localhost:8080"]
}
