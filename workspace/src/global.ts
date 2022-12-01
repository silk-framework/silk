import { HOST } from "./app/constants/path";
import { onErrorHandler } from "./app/services/errorLogger";
import { createPlugin } from "./app/services/pluginApi";

/**
 * @override
 */
const registerGlobals = () => {
    window.onerror = onErrorHandler;
    (window as any).DMInstance = {
        createPlugin,
    };
    /**
     * @Global: __webpack_public_path__
     * @Link: https://webpack.js.org/guides/public-path/
     * @Note: __webpack_public_path__ is used to require the files with webpack async. import
     * In React we use Lazy loading of components and for each webpack generates the chunk files
     * Generated chunk files imported from the Root Origin.
     * Overriding webpack public path with dynamically received path from the Window.DI object
     * which provided by back-end
     */
    //@ts-ignore
    try {
        //@ts-ignore
        __webpack_public_path__ = HOST + __webpack_public_path__;
    } catch {}
};

export default registerGlobals;
