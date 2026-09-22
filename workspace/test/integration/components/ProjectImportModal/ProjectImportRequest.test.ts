import { requestStartProjectImport } from "@ducks/workspace/requests";
import fetch from "../../../../src/app/services/fetch";
jest.mock("../../../../src/app/services/fetch", () => jest.fn());
jest.mock("../../../../src/app/utils/getApiEndpoint", () => ({ workspaceApi: (path: string) => path }));
it("sends the custom destination ID and ACL groups as query parameters", async () => {
    await requestStartProjectImport("upload", false, false, ["editors", "admins"], "my-project");
    const request = jest.mocked(fetch).mock.calls[0][0];
    const url = new URL(request.url, "http://localhost");
    expect(request.method).toBe("POST");
    expect(url.pathname).toBe("/projectImport/upload");
    expect(url.searchParams.get("newProjectId")).toBe("my-project");
    expect(url.searchParams.get("generateNewId")).toBe("false");
    expect(url.searchParams.get("overwriteExisting")).toBe("false");
    expect(url.searchParams.getAll("groups")).toEqual(["editors", "admins"]);
});
