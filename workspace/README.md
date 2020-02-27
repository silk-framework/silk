This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app)
and ejected, configured for DI. 

## Available Scripts

In the project directory, you can run:


### `watch-di-dev`

Runs the app in watch development mode.<br>
You will also see any lint errors in the console.
JS and CSS edits will re-build automatically.

### `build-di-dev`

Build the Development version of application.

### `build-di-prod`

Build the Production ready version of application.

### `npm test`

Launches the test runner in the interactive watch mode.<br>
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

@NOTE: Also [Create React App](https://github.com/facebook/create-react-app) default commands are available too.

### Error Handling
* All Errors handled and stored in browser IndexedDB `logs` table.
* React based errors handled by [componentDidCatch](https://reactjs.org/blog/2017/07/26/error-handling-in-react-16.html) lifecycle hook.
* All Network errors handled by `Axios` Response Interceptor.
* The rest of JS errors(`ReferenceError`, `Uncaught Error`) handled by custom event handler assigned to `window.onerror` listener in `src/global.ts`
