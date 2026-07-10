import React from "react";
import "@testing-library/jest-dom";
import { render } from "@testing-library/react";
import { EvaluationActivityControl } from "../EvaluationActivityControl";

const mockTranslate = (key: string, options?: string | { defaultValue?: string }) =>
    typeof options === "string" ? options : (options?.defaultValue ?? key);

jest.mock("react-i18next", () => ({
    useTranslation: () =>
        Object.assign([mockTranslate], {
            t: mockTranslate,
            i18n: { language: "en" },
        }),
}));

describe("EvaluationActivityControl", () => {
    it("keeps the show or hide evaluation action visible when a result exists but is currently hidden", () => {
        const { container } = render(
            <EvaluationActivityControl
                score={undefined}
                loading={false}
                evaluationResultsShown={false}
                hasEvaluationResult={true}
                evaluationResultsShownToggleButton={{
                    "data-test-id": "toggle-evaluation-results",
                    icon: "item-viewdetails",
                    action: jest.fn(),
                    tooltip: "Show evaluation",
                }}
            />,
        );

        expect(container.querySelector('[data-test-id="toggle-evaluation-results"]')).toBeInTheDocument();
    });

    it("hides the show or hide evaluation action if no evaluation result exists yet", () => {
        const { container } = render(
            <EvaluationActivityControl
                score={undefined}
                loading={false}
                evaluationResultsShown={false}
                hasEvaluationResult={false}
                evaluationResultsShownToggleButton={{
                    "data-test-id": "toggle-evaluation-results",
                    icon: "item-viewdetails",
                    action: jest.fn(),
                    tooltip: "Show evaluation",
                }}
            />,
        );

        expect(container.querySelector('[data-test-id="toggle-evaluation-results"]')).not.toBeInTheDocument();
    });
});
