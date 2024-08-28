{{/*
Define the init container which will create the DATABASE object for the microservice in the database

The required helm values for this template:
1. .Values.database.host - the hostname of the database instance
2. .Values.database.port - the port of the database instance
3. .Values.database.dbName - the name of the database
4. .Values.database.secret - the name of the secret which contains the database credentials
5. .Values.database.dbaUserkey - the key of the item in the secret which contains the DB admin user username
6. .Values.database.dbaPasswdkey - the key of the item in the secret which contains the DB admin user password
7. .Values.database.sslEnabled - whether ssl is enabled or not
8. .Values.database.userkey - the key of the item in the secret which contains the DB application username
The optional helm values for this template:
1. .Values.database.creationTime - how long retry for before failing the init container, defaults to 30
2. .Values.database.pathToServerCert - server CA certificate secret mount point if ssl is enabled, defaults to "/mnt/ssl/server/"
*/}}
{{- define "eric-esoa-dr-service.create-database-init-container" -}}
{{- if .Values.global.createDB }}
- name: {{ .Chart.Name }}-create-database
  image: {{ template "eric-esoa-so-library-chart.imagePath" (dict "imageId" "pgInitContainer" "values" .Values "files" .Files) }}
  env:
    - name: "POSTGRES_HOST"
      value: {{ .Values.database.host | quote }}
    - name: "POSTGRES_USER"
      valueFrom:
        secretKeyRef:
          name: {{ .Values.database.secret }}
          key: {{ .Values.database.dbaUserkey }}
    - name: "PGPASSWORD"
      valueFrom:
        secretKeyRef:
          name: {{ .Values.database.secret }}
          key: {{ .Values.database.dbaPasswdkey }}
    - name: "CUSTOM_DB_USER"
      valueFrom:
        secretKeyRef:
          name: {{ .Values.database.secret }}
          key: {{ .Values.database.userkey }}
    - name: "POSTGRES_DB"
      value: {{ .Values.database.dbName | quote}}
    - name: "POSTGRES_SCHEMA"
      value: {{ .Values.database.schemaName | quote}}
    - name: "POSTGRES_PORT"
      value: {{ .Values.database.port | quote}}
    - name: SSL_PARAMETERS
      value: {{ include "eric-esoa-so-library-chart.sslParameters" . | quote }}
    - name: "STARTUP_WAIT"
      value: {{ default 30 .Values.database.creationTime | quote}}
    - name: TZ
      value: {{ .Values.global.timezone }}
  {{- if ( include "eric-esoa-so-library-chart.ssl-enabled" . ) }}
  volumeMounts:
{{ include "eric-esoa-so-library-chart.edb-server-cert-volume-mount" . | indent 2 }}
{{ include "eric-esoa-dr-service.pg-dba-client-cert-volume-mount" . | indent 2 }}
  {{- end }}
{{- end -}}
{{- end -}}

{{- define "eric-esoa-dr-service.pg-dba-client-cert-volume-mount" -}}
- name: pg-dba-client-cert-volume
  mountPath: {{ include "eric-esoa-so-library-chart._value-path-to-client-cert" . | quote }}
{{- end -}}

{{- define "eric-esoa-dr-service.pg-dba-client-cert-volume" -}}
{{- if .Values.global.createDB }}
- name: pg-dba-client-cert-volume
  secret:
    items:
    - key: {{ include "eric-esoa-so-library-chart._value-client-cert-secret-item-cert-key" . | quote }}
      path: {{ include "eric-esoa-so-library-chart._value-relative-path-to-client-cert" . | quote }}
    - key: {{ include "eric-esoa-so-library-chart._value-client-cert-secret-item-key-key" . | quote }}
      path: {{ include "eric-esoa-so-library-chart._value-relative-path-to-client-key" . | quote }}
    secretName: {{ default (printf "%s-%s" .Values.database.host "postgres-cert") .Values.database.dbaClientCertSecret | quote }}
{{- end -}}
{{- end -}}
