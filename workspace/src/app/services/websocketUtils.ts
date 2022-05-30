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
    updateFunc: (updateItem: T) => any
): CleanUpFunction => {
    const cleanUpFunctions: CleanUpFunction[] = [];
    const fixedWebSocketUrl = convertToWebsocketUrl(webSocketUrl);
    const websocket = new WebSocket(fixedWebSocketUrl);
    websocket.onmessage = function (evt) {
        updateFunc(JSON.parse(evt.data));
    };

    websocket.onerror = function (event) {
        console.log("Connecting to WebSocket at '" + fixedWebSocketUrl + "' failed. Falling back to polling...");
        let lastUpdate = 0;
        const timeout = setInterval(async function () {
            let timestampedUrl = pollingUrl + (pollingUrl.indexOf("?") >= 0 ? "&" : "?") + "timestamp=" + lastUpdate;
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
