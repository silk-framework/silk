import { convertToWebsocketUrl, connectWebSocket } from "../websocketUtils";
import WS from "jest-websocket-mock";
import { waitFor } from "@testing-library/react";

describe("websocketUtils", () => {
    it("should convert to a correct websocket URL given a http URL", () => {
        expect(convertToWebsocketUrl("https://websocketserver.com")).toBe("wss://websocketserver.com");
        expect(convertToWebsocketUrl("http://websocketserver.com")).toBe("ws://websocketserver.com");
        expect(convertToWebsocketUrl("http://websocketserver.com/path/")).toBe("ws://websocketserver.com/path/");
        expect(convertToWebsocketUrl("https://websocketserver.com/some/path?param1=val1&param2=val2")).toBe(
            "wss://websocketserver.com/some/path?param1=val1&param2=val2"
        );
    });

    it("should attempt reconnect after server connection closes", async () => {
        const server = new WS("ws://localhost:1234");
        const messages: any[] = [];
        let errorIsRegistered = false;
        connectWebSocket(
            "http://localhost:1234",
            "",
            (data) => {
                messages.push(data);
            },
            () => {
                errorIsRegistered = true;
            },
            100
        );
        await server.connected;
        const testMsg = `{"event": "testing"}`;
        server.send(testMsg);
        server.close({ code: 1006, reason: "NOPE", wasClean: true });
        await server.closed;
        WS.clean();
        const server2 = new WS("ws://localhost:1234");
        await server2.connected;
        server2.send(testMsg);
        expect(errorIsRegistered).toBeTruthy();
        await waitFor(() => expect(messages.length).toBe(2));
    });
});
