import { stripMarkdown } from "../stripMarkdown";

describe("stripMarkdown", () => {
    it("should leave plain text untouched", () => {
        expect(stripMarkdown("Just a plain description.")).toBe("Just a plain description.");
    });

    it("should remove emphasis markers", () => {
        expect(stripMarkdown("Some **bold** and *italic* and ***both*** and ~~gone~~ text")).toBe(
            "Some bold and italic and both and gone text",
        );
        expect(stripMarkdown("__strong__ and _em_")).toBe("strong and em");
    });

    it("should keep the text of links and the alt text of images", () => {
        expect(stripMarkdown("See [the docs](https://example.com/docs) and ![a chart](chart.png).")).toBe(
            "See the docs and a chart.",
        );
    });

    it("should unwrap inline code and drop heading, list and blockquote markers", () => {
        expect(stripMarkdown("## Heading\n- uses `myPlugin`\n> quoted")).toBe("Heading uses myPlugin quoted");
    });

    it("should collapse multi-line descriptions into a single line", () => {
        expect(stripMarkdown("First line\n\nSecond line")).toBe("First line Second line");
    });
});
