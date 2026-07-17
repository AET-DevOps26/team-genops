# =============================================================================
# JobReady dev environment — Azure AKS.
# Terraform OWNS the cloud only. Cluster config + app deploy is Ansible's job.
# =============================================================================

resource "azurerm_resource_group" "main" {
  name     = "rg-${var.prefix}"
  location = var.location
  tags     = var.tags
}

resource "azurerm_kubernetes_cluster" "aks" {
  name                = "aks-${var.prefix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = var.prefix
  kubernetes_version  = var.kubernetes_version

  # Explicit node resource group name
  node_resource_group = "rg-${var.prefix}-nodes"

  default_node_pool {
    name       = "system"
    node_count = var.node_count
    vm_size    = var.node_vm_size

    # Lets Azure change vm_size (and similar recreate-forcing pool settings)
    # by rotating through a temporary pool instead of destroying the cluster:
    # temp pool up → workloads drained over → system pool rebuilt → temp deleted.
    # NOTE: pool names must match ^[a-z][a-z0-9]{0,11}$ (≤12 chars, no _ or -).
    temporary_name_for_rotation = "systemtmp"
  }

  identity {
    type = "SystemAssigned"
  }

  tags = var.tags
}

# Reserved, STABLE public IP for the ingress. Lives in the main Resource Group
#(NOT the node RG that gets deleted with the cluster) and is protected from destroy.
# domain_name_label gives a free, stable FQDN: <label>.<region>.cloudapp.azure.com
resource "azurerm_public_ip" "ingress" {
  name                = "pip-${var.prefix}-ingress"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = var.dns_label
  tags                = var.tags

  lifecycle {
    prevent_destroy = true
  }
}

# Allow the cluster's identity to attach the public IP above to its
# load balancer (the IP is outside the node resource group).
resource "azurerm_role_assignment" "aks_network_contributor" {
  scope                = azurerm_resource_group.main.id
  role_definition_name = "Network Contributor"
  principal_id         = azurerm_kubernetes_cluster.aks.identity[0].principal_id
}
