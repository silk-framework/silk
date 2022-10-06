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
 */
export const connectWebSocket = <T>(
    webSocketUrl: string,
    pollingUrl: string,
    updateFunc: (updateItem: T, cleanUp: CleanUpFunction) => any,
    registerError?: (errorId: string, err: any, cause?: any) => void,
    retryTimeout: number = 5000
): CleanUpFunction => {
    const cleanUpFunctions: CleanUpFunction[] = [];
    const cleanUp = () => {
        cleanUpFunctions.forEach((cleanUpFn) => cleanUpFn());
    };
    const fixedWebSocketUrl = convertToWebsocketUrl(webSocketUrl);
    const websocketState = {
        socket: new WebSocket(fixedWebSocketUrl),
        hasEstablishedConnection: false,
    };

    const initWebsocket = () => {
        websocketState.socket.onmessage = function (evt) {
            updateFunc(JSON.parse(evt.data), cleanUp);
        };

        websocketState.socket.onopen = function () {
            websocketState.hasEstablishedConnection = true;
        };

        websocketState.socket.onerror = function (event) {
            if (!websocketState.hasEstablishedConnection) {
                console.log(
                    "Connecting to WebSocket at '" + fixedWebSocketUrl + "' failed. Falling back to polling..."
                );
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

        websocketState.socket.onclose = function (event) {
            console.log("There has been a disconnect, attempting to reconnect...");
            if ((event.code === 1006 || event.code === 1011) && websocketState.hasEstablishedConnection) {
                //only retry for abnormal closures and server internal error
                registerError &&
                    registerError("Socket.Connection.Close", "There has been a disconnect, attempting to reconnect");
                const timeoutId = setTimeout(() => {
                    websocketState.socket = new WebSocket(fixedWebSocketUrl);
                    initWebsocket();
                }, retryTimeout);

                cleanUpFunctions.push(() => {
                    clearTimeout(timeoutId);
                });
            }
        };

        cleanUpFunctions.push(() => {
            websocketState.socket.onerror = null;
            websocketState.socket.close(1000, "Closing web socket connection.");
        });
    };
    initWebsocket();
    return cleanUp;
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
