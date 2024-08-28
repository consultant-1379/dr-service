alter table job
add column version BIGINT not null default 0,
add column locked BOOLEAN default false,
add column lock_time BIGINT,
add column modified_date TIMESTAMP,
add column reconcile_request JSONB;
alter table job alter column version drop default;
