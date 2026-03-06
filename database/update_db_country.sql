set @exist := (select count(*) from information_schema.columns where table_schema = 'overseas_purchase' and table_name = 'user' and column_name = 'country');
set @sql := if(@exist > 0, 'select ''Column already exists''', 'alter table user add column country varchar(20) default "CNH"');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;