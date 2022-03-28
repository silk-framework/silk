import { ruleEditorModelUtilsFactory } from "../RuleEditorModel.utils";

describe("Rule editor model utils", () => {
    const utils = ruleEditorModelUtilsFactory();

    it("should remove undefined input ports from array", () => {
        const x = "x";
        const n = undefined;
        const test = (inputArray: ("x" | undefined)[], expectedArray: ("x" | undefined)[]) => {
            utils.adaptInputArray(inputArray);
            expect(inputArray).toStrictEqual(expectedArray);
        };
        test([n, x, n], [n, x]);
        test([x, n, n], [x]);
        test([x, n, x], [x, n, x]);
        test([x, n, n, x, n], [x, n, n, x]);
        test([], []);
        test([x], [x]);
        test([n], []);
    });
});
