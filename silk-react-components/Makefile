
SILK_LIB_ROOT=../silk-workbench/silk-workbench-core/public/libs

SILK_DIST_ROOT=../silk-workbench/silk-workbench-core/public/libs/silk-react-components

# List of vendor libs we maintain in the package.json
VENDOR = dialog-polyfill jquery jsplumb jstree material-design-lite mdl-selectfield

default: help

# interactive license consolidation
consolidate-licenses: install
	./node_modules/.bin/ecc-license-checker report --output=.tmp/license-report-currently-installed.yml
	./node_modules/.bin/ecc-license-checker consolidate --inputFile .tmp/license-report-currently-installed.yml --outputFile license-report.yml
	rm -f .tmp/license-report-currently-installed.yml

## Build react components and copy it to silk-workbench-core
build: install
	yarn run deploy
	rm -rf $(SILK_DIST_ROOT)
	mkdir -p $(SILK_DIST_ROOT)
	./node_modules/uglify-js/bin/uglifyjs --compress dead_code,sequences=false --beautify -- ./dist/main.js > $(SILK_DIST_ROOT)/main.js
	cp ./dist/style.css $(SILK_DIST_ROOT)/style.css
	cp -rf ./dist/fonts $(SILK_DIST_ROOT)

## Copy vendor libs to silk-workbench-core
vendors: install
	$(foreach var,$(VENDOR), PACKAGE=$(var) make copy-vendor;)

install:
	yarn

copy-vendor:
	rm -rf $(SILK_LIB_ROOT)/$(PACKAGE)
	cp -r node_modules/$(PACKAGE) $(SILK_LIB_ROOT)/

## Show this help screen
help:
	@printf "Available targets\n\n"
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "%-22s %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)
