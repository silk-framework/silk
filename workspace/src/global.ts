import { onErrorHandler } from "./app/providers/errorLogger";
import { createPlugin } from "./app/providers/pluginApi";

/**
 * @override
 */
const registerGlobalListeners = () => {
    window.onerror = onErrorHandler;
    (<any>window).DMInstance = {
        createPlugin
    };
};

export default registerGlobalListeners;
