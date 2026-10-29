const getClientEnvironment = require("../../config/env");

describe("frontend build environment", () => {
    const originalHost = process.env.HOST;
    const originalApiEndpoint = process.env.API_ENDPOINT;
    const originalCiToken = process.env.CI_JOB_TOKEN;
    const restoreEnvironmentVariable = (name, value) => {
        if (value === undefined) {
            delete process.env[name];
        } else {
            process.env[name] = value;
        }
    };

    afterAll(() => {
        restoreEnvironmentVariable("HOST", originalHost);
        restoreEnvironmentVariable("API_ENDPOINT", originalApiEndpoint);
        restoreEnvironmentVariable("CI_JOB_TOKEN", originalCiToken);
    });

    it("exposes only explicitly browser-visible variables", () => {
        process.env.HOST = "https://example.test";
        process.env.API_ENDPOINT = "/api-test";
        process.env.CI_JOB_TOKEN = "must-not-be-bundled";

        const environment = getClientEnvironment("/core/assets/new-workspace/");

        expect(environment.raw).toEqual({
            NODE_ENV: "test",
            PUBLIC_URL: "/core/assets/new-workspace/",
            HOST: "https://example.test",
            API_ENDPOINT: "/api-test",
        });
        expect(environment.stringified["process.env"]).not.toHaveProperty("CI_JOB_TOKEN");
    });
});
