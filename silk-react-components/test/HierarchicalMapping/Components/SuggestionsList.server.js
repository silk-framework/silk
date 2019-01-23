import nock from "nock";

// default baseUrl in case nothing else is defined.
// for more details see store.js:
// hierarchicalMappingChannel.subject('setSilkDetails')
const baseUrl = 'http://test.url';

const mockup = (case1, case2) => {

    const data2 = (testCase) => {
        return {
            url: `/transform/tasks/test/test/rule/${testCase}/valueSourcePaths?unusedOnly=true`,
        };
    };

    const response2 = (testCase) => {
        switch (testCase) {
            case "200":
                return {
                    code: 200,
                    body: [
                        "loanId",
                        "interestRateType",
                        "initialInterestRate",
                    ],
                };
            case "404NF":
                return {
                    code: 404,
                    body: {
                        title: "Not Found",
                        detail: "Not Found",
                    }
                };
            case "404":
                return {
                    code: 404,
                    body: {
                        title: "second error title",
                        detail: "error's detail 404",
                    }
                };
            case "500":
                return {
                    code: 500,
                    body: {
                        title: "second error title",
                        detail: "error's detail 500",

                    }
                };
            default:
                throw new Error(`Undefined response for code ${testCase}`);

        }
    };

    const data1 = (testCase) => {
        return {
            url: '/ontologyMatching/matchVocabularyClassDataset',
            data: {
                dataTypePropertiesOnly: false,
                datasetUriPrefix: "",
                matchFromDataset: true,
                nrCandidates: 1,
                projectName: "test",
                ruleId: testCase,
                targetClassUris: [testCase],
                transformTaskName: "test",
            },
        }
    };


    const response1 = (testCase) => {
        switch (testCase) {
            case "200":
                return {
                    code: 200,
                    body: {
                        "collateralized": [
                            {
                                uri: "http://spec.edmcouncil.org/fibo/red/loan/loans/loans-collateral/collateralType",
                                confidence: 0.31501831501831523,
                                type: "value",
                            },
                        ],
                    }
                };
            case "404NF":
                return {
                    code: 404,
                    body: {
                        title: "Not Found",
                        detail: "Not Found",
                    }
                };
            case "404":
                return {
                    code: 404,
                    body: {
                        title: "first error title",
                        detail: "error's detail 404",

                    }
                };
            case "500":
                return {
                    code: 500,
                    body: {

                        title: "first error title",
                        detail: "error's detail 500",

                    }
                };
            default:
                throw new Error(`Undefined response for testCase ${testCase}`);
        }
    };

    const firstPayload = data1(case1);
    const firstResponse = response1(case1);
    const secondPayload = data2(case2);
    const secondResponse = response2(case2);

    // create mockup for the 2 endpoints used in silk, with data case1 and case2
    nock(baseUrl)
        .post(firstPayload.url, firstPayload.data)
        .reply(firstResponse.code, () => firstResponse.body)
    ;

    // second call for suggestions
    // 3 suggestions
    nock(baseUrl)
        .get(secondPayload.url)
        .reply(secondResponse.code, () => secondResponse.body)
    ;
};

// mock up testCases cases

// 3 and 2 suggestions
mockup("200", "200");
// 404 with json response
mockup("404", "404");
// 404 with json Not Found response from silk
mockup("404NF", "404NF");
// 500 with json response
mockup("500", "500");
