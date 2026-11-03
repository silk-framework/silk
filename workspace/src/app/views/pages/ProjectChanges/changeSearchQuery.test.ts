import { changeSearchMatches } from "./changeSearchQuery";

it("treats conversation highlighting as context rather than a filter", () => {
    expect(changeSearchMatches("?highlightConversation=c1", { textQuery: "" }, [], 1)).toBe(true);
    expect(changeSearchMatches("?highlightConversation=c1&conversationId=c2&page=2", { textQuery: "" }, [], 2)).toBe(
        true,
    );
});

it.each([
    "?f_ids=changeConversation&types=keyword&f_keys=c1%20%26two",
    "?f_ids[]=changeConversation&types[]=keyword&f_keys[]=c1%20%26two",
])("compares scalar and array query encodings with real conversation IDs: %s", (query) => {
    expect(
        changeSearchMatches(
            query,
            { textQuery: "" },
            [{ facetId: "changeConversation", type: "keyword", keywordIds: ["c1 &two"] }],
            1,
        ),
    ).toBe(true);
});

it("detects changed filters, pages and a return to all project changes", () => {
    const facets = [{ facetId: "changeConversation", type: "keyword", keywordIds: ["c1"] }];
    expect(changeSearchMatches("?highlightConversation=c1", { textQuery: "" }, facets, 1)).toBe(false);
    expect(changeSearchMatches("?textQuery=movies", { textQuery: "people" }, [], 1)).toBe(false);
    expect(changeSearchMatches("?page=2", { textQuery: "" }, [], 1)).toBe(false);
});

it.each([",", "%2C"])("matches combined Unreviewed and conversation filters with %s separators", (separator) => {
    const query = `?highlightConversation=c1&f_ids=changeState${separator}changeConversation&types=keyword${separator}keyword&f_keys=unreviewed${separator}c1`;
    expect(
        changeSearchMatches(
            query,
            { textQuery: "" },
            [
                { facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] },
                { facetId: "changeConversation", type: "keyword", keywordIds: ["c1"] },
            ],
            1,
        ),
    ).toBe(true);
});
