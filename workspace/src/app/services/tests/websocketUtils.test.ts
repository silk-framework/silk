import { convertToWebsocketUrl } from "../websocketUtils";

describe("websocketUtils", () => {
    it("should convert to a correct websocket URL given a http URL", () => {
        expect(convertToWebsocketUrl("https://websocketserver.com")).toBe("wss://websocketserver.com");
        expect(convertToWebsocketUrl("http://websocketserver.com")).toBe("ws://websocketserver.com");
        expect(convertToWebsocketUrl("http://websocketserver.com/path/")).toBe("ws://websocketserver.com/path/");
        expect(convertToWebsocketUrl("https://websocketserver.com/some/path?param1=val1&param2=val2")).toBe(
            "wss://websocketserver.com/some/path?param1=val1&param2=val2"
        );
    });
});
