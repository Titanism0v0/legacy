-- Usage:
--   mysql -u root -p < scripts/bootstrap_db.sql
-- Purpose:
--   Rebuild test database with utf8mb4 and import both schema + seed data.

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

CREATE DATABASE IF NOT EXISTS `overseas_purchase`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

ALTER DATABASE `overseas_purchase`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `overseas_purchase`;

SOURCE ./src/main/resources/db/schema.sql;
SOURCE ./src/main/resources/db/data.sql;

