import { fetch } from "./fetch/fetch";

/**
 * Connects to a WebSocket and calls a provided function on every update.
 * Returns a clean-up function that closes the web socket and optionally the polling-interval.
 *
 * @param webSocketUrl Address of a WebSocket endpoint that supplies updates via JSON objects.
 * @param pollingUrl   Address of a polling endpoint for fallback.
 *                     The polling endpoint must accept a parameter 'timestamp' and only return updates after the given time.
 *                     It must return a JSON of the format { "timestamp": Server timestamp, "updates": [ update1, update2, ...] }
 * @param updateFunc   Function to be called on every update. Receives a single argument, which is the received JSON object.
 *
 */ export const connectWebSocket = <T>(
    webSocketUrl: string,
    pollingUrl: string = "",
    updateFunc: (updateItem: T) => any,
    registerError?: (errorId: string, err: any, cause?: any) => void,
    __isReconnecting__: boolean = false,
    __hasPrevConnection: boolean = false
): CleanUpFunction => {
    const cleanUpFunctions: CleanUpFunction[] = [];
    const fixedWebSocketUrl = convertToWebsocketUrl(webSocketUrl);
    const websocket = new WebSocket(fixedWebSocketUrl);
    let hasEstablishedConnection = __hasPrevConnection;
    let isReconnecting = __isReconnecting__;

    websocket.onmessage = function (evt) {
        updateFunc(JSON.parse(evt.data));
    };

    websocket.onopen = function () {
        hasEstablishedConnection = true;
        isReconnecting = false;
    };

    websocket.onerror = function (event) {
        if (!isReconnecting) {
            console.log("Connecting to WebSocket at '" + fixedWebSocketUrl + "' failed. Falling back to polling...");
            let lastUpdate = 0;
            const timeout = setInterval(async function () {
                let timestampedUrl =
                    pollingUrl + (pollingUrl.indexOf("?") >= 0 ? "&" : "?") + "timestamp=" + lastUpdate;
                const response = await fetch({
                    url: timestampedUrl,
                });
                const responseData = response.data;
                if (responseData.timestamp === undefined || responseData.updates === undefined) {
                } else {
                    lastUpdate = responseData.timestamp;
                    responseData.updates.forEach(updateFunc);
                }
            }, 1000);
            cleanUpFunctions.push(() => {
                clearInterval(timeout);
            });
        }
    };

    websocket.onclose = function (event) {
        console.log("There has been a disconnect, attempting to reconnect...");
        if ((event.code === 1006 || event.code === 1011) && hasEstablishedConnection) {
            //only retry for abnormal closures and server internal error
            registerError &&
                registerError("Socket.Connection.Close", "There has been a disconnect, attempting to reconnect");
            const timeoutId = setTimeout(() => {
                connectWebSocket(
                    webSocketUrl,
                    pollingUrl,
                    updateFunc,
                    registerError,
                    isReconnecting,
                    hasEstablishedConnection
                );
            }, 5000);

            cleanUpFunctions.push(() => {
                clearTimeout(timeoutId);
            });
        }
    };

    cleanUpFunctions.push(() => {
        websocket.onerror = null;
        websocket.close(1000, "Closing web socket connection.");
    });
    return () => {
        cleanUpFunctions.forEach((cleanUpFn) => cleanUpFn());
    };
};

export const convertToWebsocketUrl = (url: string): string => {
    const u = new URL(url);
    let wsProtocol = "ws";
    if (u.protocol === "https:") {
        wsProtocol = "wss";
    }
    let pathName = u.pathname;
    if (pathName === "/") {
        pathName = "";
    }
    return `${wsProtocol}://${u.host}${pathName}${u.search}`;
};

// Function that should be called to clean up
type CleanUpFunction = () => any;
