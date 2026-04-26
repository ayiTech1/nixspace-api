-- Docker MySQL init: runs once on first container start
-- The database and user are created via MYSQL_* env vars in docker-compose.
-- This script sets up any additional grants or settings.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Ensure the nixspace user has full privileges on the schema
GRANT ALL PRIVILEGES ON nixspace.* TO 'nixspace'@'%';
FLUSH PRIVILEGES;
