use sp;

create table equipment
(
    id                  char(36)    not null comment 'ID',
    terminal_id         varchar(10) not null comment 'サイトID',
    equipment_intensity double      not null comment '設備原単位',
    activity_unit       varchar(10) not null comment '活動量単位',
    primary key (id)
)
    comment '設備';

create table equipment_use
(
    id           char(36)                            not null comment 'ID',
    lot_id       char(36)                            not null comment 'ロットID',
    equipment_id char(36)                            not null comment '設備ID',
    activity     double                              not null comment '活動量',
    co2          double                              not null comment 'CO2',
    created_at   timestamp default CURRENT_TIMESTAMP not null comment '活動日時',
    primary key (id)
)
    comment '設備利用';

create index idx_lot_equipment
    on equipment_use (lot_id, equipment_id);

create table lot
(
    id                 char(36)                                                               not null comment 'ID',
    terminal_id        varchar(10)                                                            not null comment 'サイトID',
    status             enum ('in-progress', 'completed', 'shipped') default 'in-progress'     not null comment 'ステータス',
    completed_quantity double unsigned                                                        null comment '完成数',
    defective_quantity double unsigned                                                        null comment '不良数',
    unit               varchar(10)                                                            not null comment '単位',
    co2                double                                       default 0                 not null comment 'CO2',
    lot_intensity      double                                       default 0                 not null comment 'ロット原単位',
    created_at         timestamp                                    default CURRENT_TIMESTAMP not null comment '作成日時',
    primary key (id)
)
    comment 'ロット';

create table material_use
(
    id              char(36)                            not null comment 'ID',
    lot_id          char(36)                            not null comment 'ロットID',
    material_lot_id char(36)                            not null comment '資材ロットID',
    consumption     double                              not null comment '消費量',
    co2             double                              not null comment 'CO2',
    created_at      timestamp default CURRENT_TIMESTAMP not null comment '活動日時',
    primary key (id)
);
