terraform {
  required_version = ">= 1.6"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Remote state in Azure Storage. Values are supplied at init time, e.g.:
  #   terraform init \
  #     -backend-config="resource_group_name=rg-tfstate" \
  #     -backend-config="storage_account_name=jobreadytfstate" \
  #     -backend-config="container_name=tfstate" \
  #     -backend-config="key=dev.tfstate"
  # (Run `terraform validate` with `-backend=false` to skip this locally.)
  backend "azurerm" {}
}
