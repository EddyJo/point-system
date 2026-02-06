MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('MAX_GRANT_PER_TRANSACTION_CONFIG', '100000', NOW());

MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('MAX_BALANCE_PER_USER_CONFIG', '1000000', NOW());

MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('DEFAULT_EXPIRE_DAYS_CONFIG', '365', NOW());