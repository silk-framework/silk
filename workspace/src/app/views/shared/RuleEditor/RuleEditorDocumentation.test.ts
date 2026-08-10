import { IRuleOperator } from "./RuleEditor.typings";
import { ruleOperatorToPluginDocumentation } from "./RuleEditorDocumentation";

const documentedOperator = {
    pluginId: "replace",
    label: "Replace",
    description: "Replaces text.",
    markdownDocumentation: "# Replace",
    relatedPlugins: [{ id: "regexReplace", description: "Uses regular expressions." }],
} satisfies Pick<IRuleOperator, "pluginId" | "label" | "description" | "markdownDocumentation" | "relatedPlugins">;

describe("ruleOperatorToPluginDocumentation", () => {
    it("resolves related plugins against the available rule operators", () => {
        const documentation = ruleOperatorToPluginDocumentation(documentedOperator, [
            {
                pluginId: "regexReplace",
                label: "Regex replace",
                description: "Replaces text using a regular expression.",
                markdownDocumentation: "# Regex replace",
            },
        ]);

        expect(documentation).toEqual({
            key: "replace",
            title: "Replace",
            description: "Replaces text.",
            markdownDocumentation: "# Replace",
            relatedPlugins: [
                {
                    plugin: {
                        key: "regexReplace",
                        title: "Regex replace",
                        description: "Replaces text using a regular expression.",
                        markdownDocumentation: "# Regex replace",
                    },
                    description: "Uses regular expressions.",
                },
            ],
        });
    });

    it("uses the related plugin ID and description when the operator is unavailable", () => {
        const documentation = ruleOperatorToPluginDocumentation(documentedOperator);

        expect(documentation.relatedPlugins).toEqual([
            {
                plugin: {
                    key: "regexReplace",
                    title: "regexReplace",
                    description: undefined,
                    markdownDocumentation: undefined,
                },
                description: "Uses regular expressions.",
            },
        ]);
    });

    it("uses the available operator documentation when an existing node has no related plugins", () => {
        const documentation = ruleOperatorToPluginDocumentation(
            {
                pluginId: "replace",
                label: "Replace",
            },
            [documentedOperator],
        );

        expect(documentation.relatedPlugins).toHaveLength(1);
        expect(documentation.markdownDocumentation).toBe("# Replace");
    });

    it("uses the available operator relations when an existing node has an empty relation list", () => {
        const documentation = ruleOperatorToPluginDocumentation(
            {
                pluginId: "replace",
                label: "Replace",
                relatedPlugins: [],
            },
            [documentedOperator],
        );

        expect(documentation.relatedPlugins).toHaveLength(1);
    });
});
