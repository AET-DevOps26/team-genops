{{/*
=============================================================================
_helpers.tpl — reusable template snippets (partials)
=============================================================================
Files that start with _ are NEVER rendered as Kubernetes manifests.
They exist purely to define named templates that other files can call.
*/}}

{{- define "jobready.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}


{{- define "jobready.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{- define "jobready.image" -}}
{{ .registry }}/{{ .image }}:{{ .tag }}
{{- end }}
