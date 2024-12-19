Removes file resources from the project based on a regular expression (regex).

The project-relative path of each file of the current project is tested against a user given regular expression and the file is deleted if the expression matches this name. The file names include the sub-directory structure if present but do not start with a `/`. The regular expression has to match the full path of the file and is case sensitive.

Given this list of example files of a project:

```
dataset.csv
my-dataset.xml
json/example.json
json/example_new.json
json/data.xml
```

Here are some regular expressions with the expected result:

- The regex `dataset\.csv` deletes only the first file.
- The regex `json/.*` deletes all files in the `json` sub-directory.
- The regex `new` deletes nothing.
- The regex `.*new.*` deletes the file `json/example_new.json` (and all other files with `new` in the path)

We recommend testing your regular expression before using it. [regex101.com](https://regex101.com) is a nice service to test your regular expressions. [This deep-link](https://regex101.com/?testString=dataset.csv%0Amy-dataset.xml%0Ajson/example.json%0Ajson/example_new.json%0Ajson/data.xml&regex=.*new.*) provides a test bed using the example files and the last expression from the list.
