create table if not exists outbox (
    outbox_id bigint not null primary key,
    event_type varchar(100) not null,
    payload varchar(5000) not null,
    service_name varchar(100) not null,
    created_at datetime not null,
    index idx_outbox_service_created (service_name, created_at)
);

create table if not exists shedlock (
    name varchar(64) not null,
    lock_until timestamp(3) not null,
    locked_at timestamp(3) not null default current_timestamp(3),
    locked_by varchar(255) not null,
    primary key (name)
);