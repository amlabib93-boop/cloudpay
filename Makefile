-include .env
export

.DEFAULT_GOAL := help

.PHONY: dev-up dev-down dev-logs dev-ps dev-psql dev-reset help

dev-up: ## Start infrastructure containers
	docker compose up -d

dev-down: ## Stop infrastructure containers
	docker compose down

dev-logs: ## Tail container logs (use s=<service> to filter)
	docker compose logs -f $(s)

dev-ps: ## Show running containers
	docker compose ps

dev-psql: ## Open psql shell on wallet database
	docker compose exec postgres-wallet psql -U $${POSTGRES_USER} -d wallet_db

dev-reset: ## Stop containers and destroy volumes (data loss!)
	@echo "This will destroy all local data volumes. Press Ctrl+C to abort."
	@read -p "Continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
