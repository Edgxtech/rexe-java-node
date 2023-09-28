create database if not exists userdb;

use userdb;

CREATE TABLE if not exists `users` (
	`user_id` int(11) NOT NULL AUTO_INCREMENT,
	`uname` varchar(45) NOT NULL,
	`password` varchar(45) NOT NULL,
	`fullname` varchar(45) NOT NULL,
	`email` varchar(45) NOT NULL,
    `pubkey` varchar(255) NOT NULL,
	PRIMARY KEY (`user_id`)
);

GRANT ALL PRIVILEGES ON userdb.* TO 'dp'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;