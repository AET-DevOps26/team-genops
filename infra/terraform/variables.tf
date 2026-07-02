variable "subscription_id" {
  description = "Azure subscription ID. Prefer the ARM_SUBSCRIPTION_ID env var in CI; leave empty to use it."
  type        = string
  default     = ""
}

variable "prefix" {
  description = "Name prefix for all resources."
  type        = string
  default     = "jobready-dev"
}

variable "location" {
  description = "Azure region. The dev FQDN becomes <dns_label>.<location>.cloudapp.azure.com."
  type        = string
  default     = "germanywestcentral"
}

variable "dns_label" {
  description = "DNS name label for the reserved ingress IP -> stable cloudapp.azure.com hostname."
  type        = string
  default     = "jobready-dev"
}

variable "kubernetes_version" {
  description = "AKS Kubernetes version. null = AKS default."
  type        = string
  default     = null
}

variable "node_count" {
  description = "Number of nodes in the default pool."
  type        = number
  default     = 2
}

variable "node_vm_size" {
  description = "VM size for the default node pool."
  type        = string
  default     = "Standard_B2s"
}

variable "tags" {
  description = "Tags applied to all resources."
  type        = map(string)
  default = {
    project = "jobready"
    env     = "dev"
    managed = "terraform"
  }
}
