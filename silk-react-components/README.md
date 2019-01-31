# silk-react-components

> JavaScript & React components of the silk project.

This project is part of the effort to modernize the javascript build process and asset management in the silk project.
It serves three main purposes at the moment:

1. Providing a build and development environment for new React components
2. Providing an environment to manage other JavaScript assets for silk
3. Providing a way to manage external libraries

## Usage

You need to have a working version of node v6 and yarn installed.
Please change your working directory to this directory for all commands listed below.
For a list of available commands, please run `make help`.

All three build commands from below can be simply executed with `make build`.

### JavaScript asset Management

**Build command**: `make bablify`

JavaScript assets are managed in the folder `silk-workbench`.
This folder is structured like [`silk-workbench` in the root](../silk-workbench) of the project.
The assets managed in this folder are run through babel, to ensure that they are ecmascript 5 compatible;
and then copied to their respective locations.

To execute this process simply run `make bablify`.

Furthermore you are able to check, that all files are ecmascript 5 compatible with `make check-es5`

### External library management

**Build command**: `make vendors`

External library scripts are maintained with the help of yarn and the npm registry.
They are defined as dependencies in the `package.json`.
`make vendors` installs those dependencies and copies them to their respective location.

### React development

**Build command**: `make build-react`

- With `yarn start` you start a local development environment which mocks all APIs.
- With `make build-react` you can build all React components of silk and copy them to their respective locations

### React development with Silk backend

Run in Silk root directory: sbt "project workbench" ~run

This will start the Silk workbench with file watchers active that watch for source changes in the Silk React subproject and trigger builds of the component.
