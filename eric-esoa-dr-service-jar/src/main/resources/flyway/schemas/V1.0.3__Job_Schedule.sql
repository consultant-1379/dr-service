create table job_specification(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
application_id BIGINT NOT NULL,
application_name VARCHAR(255) NOT NULL,
application_job_name VARCHAR(255) NOT NULL,
feature_pack_id BIGINT NOT NULL,
feature_pack_name VARCHAR(255) NOT NULL,
execution_options JSONB,
inputs JSONB,
api_property_names JSONB,
job_id_tmp BIGINT);

create table job_schedule(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
expression VARCHAR(255) NOT NULL,
job_specification_id BIGINT NOT NULL,
enabled BOOLEAN default true,
creation_date TIMESTAMP NOT NULL,
version BIGINT not null,
FOREIGN KEY (job_specification_id) REFERENCES job_specification,
CONSTRAINT js_name_unique UNIQUE (name));

-- in case of existing data, copy job parameters from job to job_specification table.
insert into job_specification
            (id,
             job_id_tmp,
             NAME,
             description,
             application_id,
             application_name,
             application_job_name,
             feature_pack_id,
             feature_pack_name,
             execution_options,
             inputs,
             api_property_names)
select nextval('job_specification_id_seq'),
       id,
       NAME,
       description,
       application_id,
       application_name,
       application_job_name,
       feature_pack_id,
       feature_pack_name,
       execution_options,
       inputs,
       api_property_names
FROM job;

-- drop columns from job table which have moved to job_specification.
-- add reference to job_schedule and job_specification tables.
alter table job drop column name,
    drop column description,
    drop column application_id,
    drop column application_name,
    drop column application_job_name,
    drop column feature_pack_id,
    drop column feature_pack_name,
    drop column execution_options,
    drop column inputs,
    drop column api_property_names,
    add column due_date TIMESTAMP,
    add column job_schedule_id BIGINT,
    add column job_specification_id BIGINT references job_specification;
create index job_schedule_id_idx on job(job_schedule_id);

-- in case of existing data, set the reference to the copied job_specification for each job in the table.
update job set job_specification_id = job_specification.id from job_specification where job.id = job_specification.job_id_tmp;
alter table job_specification drop column job_id_tmp;
alter table job alter column job_specification_id set NOT NULL;