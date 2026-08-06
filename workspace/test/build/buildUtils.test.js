jest.mock("../../config/paths", () => ({
    additionalEntries: jest.fn(),
    additionalSourcePaths: jest.fn(),
}));

const paths = require("../../config/paths");
const { adaptWebpackConfig, setBuildFailureExitCode } = require("../../scripts/build.utils");

const applicationSource = "/workspace/src";
const guiElementsSource = "/workspace/libs/gui-elements";
const pluginEntry = "/di/workspacePlugins/src/index.tsx";
const pluginSource = "/di/workspacePlugins/src";

const createConfig = () => ({
    entry: ["/workspace/src/index.tsx"],
    module: {
        rules: [
            {
                oneOf: [
                    {
                        loader: "babel-loader",
                        include: [applicationSource, guiElementsSource],
                    },
                    {
                        loader: "file-loader",
                    },
                ],
            },
        ],
    },
});

describe("DI webpack configuration adaptation", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("adds DI entries and source roots to the application compilation", () => {
        paths.additionalEntries.mockReturnValue([pluginEntry]);
        paths.additionalSourcePaths.mockReturnValue([pluginSource]);

        const config = adaptWebpackConfig(createConfig());

        expect(config.entry).toEqual(["/workspace/src/index.tsx", pluginEntry]);
        expect(config.module.rules[0].oneOf[0].include).toEqual([applicationSource, guiElementsSource, pluginSource]);
    });

    it("preserves standalone entries and source roots without DI extensions", () => {
        paths.additionalEntries.mockReturnValue([]);
        paths.additionalSourcePaths.mockReturnValue([]);
        const originalConfig = createConfig();

        const config = adaptWebpackConfig(originalConfig);

        expect(config.entry).toEqual(originalConfig.entry);
        expect(config.module.rules[0].oneOf[0].include).toEqual([applicationSource, guiElementsSource]);
    });
});

describe("build failure exit status", () => {
    it("marks one-shot compilation failures as unsuccessful", () => {
        const runtimeProcess = {};

        setBuildFailureExitCode(false, runtimeProcess);

        expect(runtimeProcess.exitCode).toBe(1);
    });

    it("allows watch mode to recover after a compilation failure", () => {
        const runtimeProcess = {};

        setBuildFailureExitCode(true, runtimeProcess);

        expect(runtimeProcess.exitCode).toBeUndefined();
    });
});
