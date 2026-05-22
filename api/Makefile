SPEC         := ./openapi.yaml
SERVICES     := ../services
WEB_CLIENT   := ../web-client/src/api.ts
GENAI_CLIENT := $(SERVICES)/genai/client

.PHONY: generate lint check clean help

help:
	@echo "Targets:"
	@echo "  generate  — Regenerate all Spring stubs, Python client, and TS types from $(SPEC)"
	@echo "  lint      — Lint $(SPEC) with Redocly"
	@echo "  check     — CI: regenerate and fail if anything drifted"
	@echo "  clean     — Remove all generated artefacts"

generate:
	./scripts/codegen.sh

lint:
	npx --yes --prefix ./scripts @redocly/cli lint $(SPEC) --config ./scripts/redocly.yaml

check: generate
	@git diff --exit-code -- \
	  '$(SERVICES)/*/src/main/java/*/generated' \
	  $(GENAI_CLIENT) \
	  $(WEB_CLIENT) \
	  || { echo ""; \
	       echo "ERROR: generated code drifted from $(SPEC)."; \
	       echo "       Run 'make -C api generate' and commit the result."; \
	       exit 1; }

clean:
	find $(SERVICES) -type d -name generated -prune -exec rm -rf {} +
	rm -rf $(GENAI_CLIENT)
	rm -f $(WEB_CLIENT)
