output "resource_group_name" {
  description = "Resource group holding the cluster and the ingress IP."
  value       = azurerm_resource_group.main.name
}

output "ingress_ip" {
  description = "Reserved static public IP for the ingress controller (set as loadBalancerIP)."
  value       = azurerm_public_ip.ingress.ip_address
}

output "ingress_fqdn" {
  description = "Stable dev hostname -> use as ingress.host in values-dev.yaml."
  value       = azurerm_public_ip.ingress.fqdn
}

output "kube_config" {
  description = "Raw kubeconfig for the AKS cluster (feed to Ansible)."
  value       = azurerm_kubernetes_cluster.aks.kube_config_raw
  sensitive   = true
}
