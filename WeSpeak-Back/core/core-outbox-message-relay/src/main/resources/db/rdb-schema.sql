create table outbox (
    outbox_id bigint not null primary key,
    event_type varchar(100) not null,
    payload varchar(5000) not null,
    created_at datetime not null,
    document_id BIGINT not null
);

create index idx_shard_key_created_at on outbox(shard_key asc, created_at asc);