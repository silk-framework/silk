const { runCompiler, setBuildFailureExitCode } = require("../../scripts/build.utils");

describe("one-shot compiler lifecycle", () => {
    it("closes the compiler after a successful build", async () => {
        const stats = {};
        const compiler = {
            run: jest.fn((callback) => callback(null, stats)),
            close: jest.fn((callback) => callback()),
        };

        await expect(runCompiler(compiler)).resolves.toBe(stats);
        expect(compiler.close).toHaveBeenCalledTimes(1);
    });

    it("closes the compiler and retains the compilation error", async () => {
        const compilationError = new Error("compilation failed");
        const compiler = {
            run: jest.fn((callback) => callback(compilationError)),
            close: jest.fn((callback) => callback()),
        };

        await expect(runCompiler(compiler)).rejects.toBe(compilationError);
        expect(compiler.close).toHaveBeenCalledTimes(1);
    });

    it("reports a compiler close failure after a successful build", async () => {
        const closeError = new Error("compiler close failed");
        const compiler = {
            run: jest.fn((callback) => callback(null, {})),
            close: jest.fn((callback) => callback(closeError)),
        };

        await expect(runCompiler(compiler)).rejects.toBe(closeError);
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
