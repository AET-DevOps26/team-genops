{{/*
=============================================================================
_helpers.tpl — reusable template snippets (partials)
=============================================================================
Files that start with _ are NEVER rendered as Kubernetes manifests.
They exist purely to define named templates that other files can call.

HOW TO CALL A HELPER from another template:
  labels:
    {{- include "jobready.labels" . | nindent 4 }}

  include    = call the named template
  .          = pass the current context (so the template can read .Values etc.)
  nindent 4  = add a newline then indent by 4 spaces (matches YAML structure)

NOTE: this header is a {{` / `}} template comment, not a # YAML comment, because
Helm parses template delimiters even inside # lines.
=============================================================================
*/}}

{{/*
Common labels applied to every resource in the chart.
Kubernetes recommends these standard labels for tooling compatibility
(Helm, kubectl, dashboards all understand them).
*/}}
{{- define "jobready.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}

{{/*
Selector labels — used in Service selectors and Deployment matchLabels.
These MUST be stable (never change after first deploy) because Kubernetes
uses them to find existing pods. If you change them, Kubernetes thinks
the old pods are orphaned.
*/}}
{{- define "jobready.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
The full image reference for a service.
Usage: {{ include "jobready.image" (dict "registry" .Values.global.imageRegistry "image" .Values.auth.image "tag" .Values.auth.tag) }}
*/}}
{{- define "jobready.image" -}}
{{ .registry }}/{{ .image }}:{{ .tag }}
{{- end }}
