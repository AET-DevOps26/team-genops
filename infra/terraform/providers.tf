provider "azurerm" {
  features {}

  # Subscription can also be supplied via the ARM_SUBSCRIPTION_ID env var
  # (preferred in CI). When set there, leave var.subscription_id empty.
  subscription_id = var.subscription_id != "" ? var.subscription_id : null
}
