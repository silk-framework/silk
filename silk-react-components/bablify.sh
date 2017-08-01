for file in $(find silk-workbench -name '*.js'); do
  target=$(echo $file | sed -E 's#^.+?/silk-workbench/#silk-workbench/#g')

  mkdir -p ../$(dirname $target)
  echo "Converting $file to ../$target"
  node_modules/.bin/babel "$file" --out-file="../$target"
done
