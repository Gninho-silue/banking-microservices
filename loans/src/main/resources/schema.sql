CREATE TABLE IF NOT EXISTS `loans`
(
    `loan_id`
    int
    NOT
    NULL
    AUTO_INCREMENT,
    `mobile_number`
    varchar
(
    15
) NOT NULL,
    `loan_number` varchar
(
    100
) NOT NULL,
    `loan_type` varchar
(
    100
) NOT NULL,
    `total_loan` DECIMAL(19, 2) NOT NULL,
    `amount_paid` DECIMAL(19, 2) NOT NULL,
    `outstanding_amount` DECIMAL(19, 2) NOT NULL,
    `annual_interest_rate` DECIMAL(5, 4) NOT NULL DEFAULT 0.0500,
    `term_months` INT NOT NULL DEFAULT 60,
    `start_date` DATE NOT NULL,
    `created_at` date NOT NULL,
    `created_by` varchar
(
    20
) NOT NULL,
    `updated_at` date DEFAULT NULL,
    `updated_by` varchar
(
    20
) DEFAULT NULL,
    PRIMARY KEY
(
    `loan_id`
)
    );