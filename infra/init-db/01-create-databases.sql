-- Runs automatically on first container startup (mounted into /docker-entrypoint-initdb.d).
-- Each microservice owns its own database — this just creates the 3 empty ones;
-- Hibernate (ddl-auto: update) creates the actual tables on each service's first boot.

CREATE DATABASE wallet_users;
CREATE DATABASE wallet_walletService;
CREATE DATABASE wallet_notification;
