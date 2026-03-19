-- 聊天相关表：买家-卖家会话与消息
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `buyer_id` BIGINT NOT NULL COMMENT '买家用户ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户ID',
  `last_message` VARCHAR(255) DEFAULT NULL COMMENT '最后一条消息内容概要',
  `last_time` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
  `unread_for_buyer` INT NOT NULL DEFAULT 0 COMMENT '买家未读数',
  `unread_for_seller` INT NOT NULL DEFAULT 0 COMMENT '卖家未读数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_buyer_seller` (`buyer_id`, `seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买家-卖家聊天会话';

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `from_user_id` BIGINT NOT NULL COMMENT '发送方用户ID',
  `to_user_id` BIGINT NOT NULL COMMENT '接收方用户ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `content_type` VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE等',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '已读标记：0-未读 1-已读',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`, `send_time`),
  CONSTRAINT `fk_chat_message_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息记录';

