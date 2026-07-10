terraform {
  required_version = ">= 1.6"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Remote state in Azure Storage. Values are supplied at init time.
  # Backend: where state lives.
  # backend "azurerm": keep it in an Azure Storage blob, shared and durable.
  # {} is empty because it will be supplied at init time in CI.
  # (Run `terraform validate` with `-backend=false` to skip this locally.)
  backend "azurerm" {}
}
