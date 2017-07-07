for file in $(find silk-workbench -name '*.js'); do
mkdir -p ../$(dirname $file)
node_modules/.bin/babel "$file" --out-file="../$file"
done
