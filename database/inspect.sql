\conninfo
\d accounts
\d audit_records
SELECT id,owner_name,balance,version,status FROM accounts ORDER BY id LIMIT 10;
SELECT installed_rank,version,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank;
