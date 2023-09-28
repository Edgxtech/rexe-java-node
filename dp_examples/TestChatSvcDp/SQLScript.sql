create database if not exists chatdb;

use chatdb;

CREATE TABLE IF NOT EXISTS `chat` (
	`chat_id` int(11) NOT NULL AUTO_INCREMENT,
	`date_created` datetime NOT NULL,
	PRIMARY KEY (`chat_id`)
);

CREATE TABLE IF NOT EXISTS `chat_user` (
	`user_id` int(11) NOT NULL AUTO_INCREMENT,
	`uname` varchar(45) NOT NULL,
	`chat_id` int NOT NULL,
	`pubkey` varchar(255) NOT NULL,
	PRIMARY KEY (`user_id`),
	KEY `UNAME_IDX` (`uname`),
	CONSTRAINT `FK8b71a8di9bu8jrssp98u8ka0s` FOREIGN KEY (`CHAT_ID`) REFERENCES `CHAT` (`CHAT_ID`)
);

CREATE TABLE IF NOT EXISTS `message` (
	`message_id` int(11) NOT NULL AUTO_INCREMENT,
	`content` varchar(5000) NOT NULL,
	`dtg` datetime NOT NULL,
	`creator_uname` varchar(45) NOT NULL,
    `chat_id` int NOT NULL,
	PRIMARY KEY (`message_id`),
	CONSTRAINT `FK8b71a8di9bu8jrssp98u8ka0r` FOREIGN KEY (`CHAT_ID`) REFERENCES `CHAT` (`CHAT_ID`),
    CONSTRAINT `FK8b71a8di9bu8jrssp98u8ka0q` FOREIGN KEY (`CREATOR_UNAME`) REFERENCES `CHAT_USER` (`UNAME`)
);


### CREATE USER 'dp'@'localhost' IDENTIFIED BY 'dp';
GRANT ALL PRIVILEGES ON chatdb.* TO 'dp'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;