-- 找回密码功能数据库表结构（分步执行版本）
-- 请按顺序执行每个语句，如果某个字段已存在，会报错但可以继续执行下一个

-- ========== 步骤1：创建密保问题表 ==========
CREATE TABLE IF NOT EXISTS `security_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '问题ID',
    `question` VARCHAR(255) NOT NULL COMMENT '问题内容',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密保问题表';

-- ========== 步骤2：添加用户表字段（分别执行，如果字段已存在会报错，可以忽略） ==========

-- 2.1 添加 use_security_questions 字段
ALTER TABLE `user`
ADD COLUMN `use_security_questions` TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用密保：0-不使用，1-使用' AFTER `status`;

-- 2.2 添加 question1_id 字段
ALTER TABLE `user`
ADD COLUMN `question1_id` BIGINT COMMENT '密保问题1ID' AFTER `use_security_questions`;

-- 2.3 添加 answer1 字段
ALTER TABLE `user`
ADD COLUMN `answer1` VARCHAR(255) COMMENT '密保答案1' AFTER `question1_id`;

-- 2.4 添加 question2_id 字段
ALTER TABLE `user`
ADD COLUMN `question2_id` BIGINT COMMENT '密保问题2ID' AFTER `answer1`;

-- 2.5 添加 answer2 字段
ALTER TABLE `user`
ADD COLUMN `answer2` VARCHAR(255) COMMENT '密保答案2' AFTER `question2_id`;

-- 2.6 添加 question3_id 字段
ALTER TABLE `user`
ADD COLUMN `question3_id` BIGINT COMMENT '密保问题3ID' AFTER `answer2`;

-- 2.7 添加 answer3 字段
ALTER TABLE `user`
ADD COLUMN `answer3` VARCHAR(255) COMMENT '密保答案3' AFTER `question3_id`;

-- ========== 步骤3：初始化密保问题（如果已存在会报错，可以忽略） ==========
INSERT INTO `security_question` (`question`, `sort_order`) VALUES
('您出生在哪里？', 1),
('您的小学名称是什么？', 2),
('您最喜欢的颜色是什么？', 3),
('您母亲的姓名是什么？', 4),
('您父亲的姓名是什么？', 5),
('您最喜欢的食物是什么？', 6),
('您最喜欢的电影是什么？', 7),
('您最喜欢的书籍是什么？', 8),
('您最好的朋友的名字是什么？', 9),
('您最喜欢的运动是什么？', 10),
('您最喜欢的音乐类型是什么？', 11),
('您最喜欢的城市是什么？', 12),
('您最喜欢的动物是什么？', 13),
('您最喜欢的季节是什么？', 14),
('您最喜欢的节日是什么？', 15);
