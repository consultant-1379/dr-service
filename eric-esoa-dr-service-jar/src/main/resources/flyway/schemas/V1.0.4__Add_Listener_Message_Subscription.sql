create table listener_message_subscription(
id BIGSERIAL PRIMARY KEY NOT NULL,
name VARCHAR(255) NOT NULL,
description VARCHAR(4000),
subsystem_name VARCHAR(255) NOT NULL,
message_broker_type VARCHAR(255) NOT NULL,
config JSONB NOT NULL,
listener_id BIGINT,
version BIGINT not null,
creation_date TIMESTAMP,
FOREIGN KEY (listener_id) REFERENCES listener ON DELETE CASCADE,
CONSTRAINT listener_message_subscription_name_unique UNIQUE (name,listener_id));