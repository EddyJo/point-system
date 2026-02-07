MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('MAX_GRANT_PER_TRANSACTION', '100000', NOW());

MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('MAX_BALANCE_PER_USER', '1000000', NOW());

MERGE INTO point_policy (policy_key, policy_value, updated_at) KEY (policy_key)
    VALUES ('DEFAULT_EXPIRE_DAYS', '365', NOW());