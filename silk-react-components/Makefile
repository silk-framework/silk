
SILK_DIST_ROOT="../silk-workbench/silk-workbench-core/public/libs/silk-react-components"

build:
	yarn
	yarn run deploy
	./node_modules/uglify-js/bin/uglifyjs --compress dead_code,sequences=false --beautify -- ./dist/main.js > $(SILK_DIST_ROOT)/main.js
	cp ./dist/style.css $(SILK_DIST_ROOT)/style.css
	mkdir -p $(SILK_DIST_ROOT)/fonts
	cp -rf ./dist/fonts $(SILK_DIST_ROOT)
