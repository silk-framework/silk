# Configuration

The configuration is located in the `conf` folder in the root directory. The default configuration is shipped in the `reference.conf` file. The configuration can be customized by adding a `application.conf` file, which may override configuration options. By the default, the `application.conf` file is searched for in the `conf` folder.  A different search folder for `application.conf` can be configured using one of the following system properties:

- `config.resource` specifies a different file name than `application.conf`.
- `config.file` specifies a filesystem path, including the full file name.
- `config.url` specifies a URL.

If the Workbench is not running standalone, but packaged as War or Docker, the `conf` folder is not directly accessible. In that case, please set one of the above system properties to point to your custom external configuration file.

The configuration is based on HOCON. More information on HOCON can be found at: [[https://github.com/typesafehub/config]]