CREATE TABLE IF NOT EXISTS `customer`
(
    `customer_id`
    int
    AUTO_INCREMENT
    PRIMARY
    KEY,
    `name`
    varchar
(
    100
) NOT NULL,
    `email` varchar
(
    100
) NOT NULL,
    `mobile_number` varchar
(
    20
) NOT NULL,
    `created_at` date NOT NULL,
    `created_by` varchar
(
    20
) NOT NULL,
    `updated_at` date DEFAULT NULL,
    `updated_by` varchar
(
    20
) DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS `accounts`
(
    `customer_id`
    int
    NOT
    NULL,
    `account_number`
    int
    AUTO_INCREMENT
    PRIMARY
    KEY,
    `account_type`
    varchar
(
    100
) NOT NULL,
    `branch_address` varchar
(
    200
) NOT NULL,
    `communication_sw` BOOLEAN,
    `balance` DECIMAL(19, 2) NOT NULL DEFAULT 0,
    `created_at` date NOT NULL,
    `created_by` varchar
(
    20
) NOT NULL,
    `updated_at` date DEFAULT NULL,
    `updated_by` varchar
(
    20
) DEFAULT NULL
    );

-- Completed transfers. idempotency_key is UNIQUE: a replayed request can never debit twice.
CREATE TABLE IF NOT EXISTS `transfers`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `from_account`    BIGINT         NOT NULL,
    `to_account`      BIGINT         NOT NULL,
    `amount`          DECIMAL(19, 2) NOT NULL,
    `status`          VARCHAR(20)    NOT NULL,
    `idempotency_key` VARCHAR(100)   NOT NULL,
    `created_at`      DATETIME(6)    NOT NULL,
    CONSTRAINT `uk_transfers_idempotency_key` UNIQUE (`idempotency_key`),
    INDEX `idx_transfers_from_account` (`from_account`),
    INDEX `idx_transfers_to_account` (`to_account`)
);
