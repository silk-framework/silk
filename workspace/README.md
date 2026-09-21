The Workspace frontend uses a custom webpack build derived from an ejected Create React App setup.
Use Node.js 24 LTS and install Yarn Classic 1.22.22 externally; the repository does not include a
Yarn binary.

## Local gui-elements development

Workspace uses `@eccenca/gui-elements` directly from the local `../libs/gui-elements` source.
Run `yarn watch` from this directory when developing the workspace or shared components;
a separate gui-elements build is not required.

## Available Scripts

In the project directory, you can run:

### `watch`

Runs the app in watch development mode.<br>
You will also see any lint errors in the console.
JS and CSS edits will re-build automatically.

### `build-di-dev`

Build the Development version of application.

### `build-di-prod`

Build the Production ready version of application.

### `i18n-parser`

Merge manual and configured additional translations, scan source files for translation keys and
write the results to `src/locales/generated/{{lngCode}}.json`. Missing translations cause the command
to fail. Generated files are overwritten; maintain translations in the manual or configured additional
language files instead.

### `yarn test`

Launches the test runner in the interactive watch mode.<br>
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

Use `yarn test --no-watch` for a single run. To diagnose leaks, use `yarn test-debug`, which enables
`DEBUG=true` and open-handle detection and runs tests serially. Use `yarn test-workers-debug` to run
with `DEBUG=true` and up to two workers instead. Do not add `--detectOpenHandles` when investigating
worker behavior: Jest then runs serially regardless of the worker limit.

Use Testing Library's `render` for isolated components. The integration helpers `renderWrapper`
and `testWrapper` provide a fresh Redux store and connected router when those are part of the test.

### Build diagnostics

Append `--stats` to a development or production build to write `bundle-stats.json`. Append
`--analyze` to generate a static bundle report or `--profile` to capture webpack-native module,
phase and plugin timings. Analyzer and profiler output is written below `.local/webpack` and is not
enabled during normal builds.

### Error Handling

- All Errors handled and stored in browser IndexedDB `logs` table.
- React based errors handled by [componentDidCatch](https://reactjs.org/blog/2017/07/26/error-handling-in-react-16.html) lifecycle hook.
- All Network errors handled by `Axios` Response Interceptor.
- The rest of JS errors(`ReferenceError`, `Uncaught Error`) handled by custom event handler assigned to `window.onerror` listener in `src/global.ts`

## Running tests in the IDE

When running the UI tests in an IDE like Intellij IDEA, environment variables need to be configured for the jest template, e.g. `HOST=http://localhost:9000;API_ENDPOINT=/api`.
Check the `.env.test` file for current ENV config.
