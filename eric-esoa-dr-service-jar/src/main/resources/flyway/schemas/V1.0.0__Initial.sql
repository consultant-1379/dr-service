create table feature_pack(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
CONSTRAINT fp_name_unique UNIQUE (name));

create table application(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
filename VARCHAR(255) NOT NULL,
feature_pack_id BIGINT,
config JSONB NOT NULL,
contents BYTEA,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (feature_pack_id) REFERENCES feature_pack,
CONSTRAINT app_name_unique UNIQUE (name,feature_pack_id));

create table properties(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
feature_pack_id BIGINT,
filename VARCHAR(255) NOT NULL,
read_only BOOLEAN,
config JSONB,
contents BYTEA,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (feature_pack_id) REFERENCES feature_pack);

create table inputs(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
feature_pack_id BIGINT,
filename VARCHAR(255) NOT NULL,
read_only BOOLEAN,
config JSONB,
contents BYTEA,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (feature_pack_id) REFERENCES feature_pack,
CONSTRAINT inputs_name_unique UNIQUE (name,feature_pack_id));

create table listener(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
feature_pack_id BIGINT,
filename VARCHAR(255) NOT NULL,
config JSONB NOT NULL,
contents BYTEA,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (feature_pack_id) REFERENCES feature_pack,
CONSTRAINT listener_name_unique UNIQUE (name,feature_pack_id));

create table asset(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
filename VARCHAR(255) NOT NULL,
contents BYTEA NOT NULL,
version BIGINT not null,
feature_pack_id BIGINT,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (feature_pack_id) REFERENCES feature_pack,
CONSTRAINT asset_name_unique UNIQUE (name,feature_pack_id));

create table job(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
start_date TIMESTAMP,
end_date TIMESTAMP,
application_id BIGINT,
application_name VARCHAR(255) NOT NULL,
application_job_name VARCHAR(255) NOT NULL,
discovered_objects_count INT DEFAULT 0,
reconciled_objects_count INT DEFAULT 0,
reconciled_objects_error_count INT DEFAULT 0,
error_message TEXT DEFAULT NULL,
feature_pack_id BIGINT,
feature_pack_name VARCHAR(255) NOT NULL,
status VARCHAR(255) NOT NULL,
execution_options JSONB,
inputs JSONB,
api_property_names JSONB);

create table discovered_object(
id BIGSERIAL PRIMARY KEY NOT NULL,
job_id BIGINT NOT NULL,
source_properties JSONB,
target_properties JSONB,
status VARCHAR(255) NOT NULL,
error_message TEXT DEFAULT NULL,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (job_id) REFERENCES job ON DELETE CASCADE);

create table filter(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
status VARCHAR(255) NOT NULL,
discrepancy VARCHAR(255),
reconcile_action VARCHAR(255) NOT NULL,
command TEXT,
command_response TEXT,
error_msg TEXT,
discovered_object_id BIGINT,
version BIGINT not null,
creation_date TIMESTAMP,
modified_date TIMESTAMP,
FOREIGN KEY (discovered_object_id) REFERENCES discovered_object ON DELETE CASCADE);