
build:
	yarn
	yarn run deploy
	./node_modules/uglify-js/bin/uglifyjs --compress dead_code,sequences=false --beautify -- dist/main.js > ../silk-workbench/silk-workbench-core/public/libs/silk-react-components/main.js
