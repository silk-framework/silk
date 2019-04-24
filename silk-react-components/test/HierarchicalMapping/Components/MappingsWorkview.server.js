import nock from "nock";

/**
 * Base url for mock
 * @type {string}
 */
const baseUrl = 'http://test.url',
	/**
	 * Initial property rules
	 * @type {*[]}
	 */
	propertyRules = [
		{
			type: "direct",
			id: "country",
			sourcePath: "dbpediaowl:country",
			mappingTarget: {
				uri: "<urn:ruleProperty:country>",
				valueType: {
					nodeType: "StringValueType"
				},
				isBackwardProperty: false,
				isAttribute: false
			},
			metadata: {
				label: ""
			}
		},
		{
			type: "direct",
			id: "basedOn",
			sourcePath: "dbpediaowl:basedOn",
			mappingTarget: {
				uri: "<urn:ruleProperty:basedOn>",
				valueType: { "nodeType": "StringValueType" },
				isBackwardProperty: false,
				isAttribute: false
			},
			metadata: {
				label: ""
			}
		}
	];

/**
 * mocking up the backend functionality
 *
 * @param identifier {string}
 */
const mockUpFunction = (identifier) => {
	/**
	 * getting the URL for data
	 *
	 * @param testCase
	 * @returns {{url: string}}
	 */
	const dataURL = (testCase) => {
		return {
			url: `/transform/tasks/test/test/peak/${testCase}`
		};
	};

	/**
	 * Getting the response form URL
	 *
	 * @param testCase
	 * @returns {*}
	 */
	const getResponse = (testCase) => {
		return {
			code: 200,
			body: {
				sourcePaths: [
					[`/dbpediaowl:${testCase}`]
				],
				results: [],
				status: {
					id: "success",
					msg: ""
				}
			}
		};

	};

	/**
	 * Getting URl of rules
	 *
	 * @returns {{url: string}}
	 */
	const rulesDataURL = () => {
		return {
			url: '/transform/tasks/test/test/rules'
		};
	};

	/**
	 * Getting response for rules
	 *
	 * @returns {{code: number, body: {metadata: {label: string}, rules: {typeRules: Array, propertyRules: *[]}, id: string, type: string}}}
	 */
	const getRulesResponse = () => {
		return {
			code: 200,
			body: {
				type: "root",
				id: "root",
				rules: {
					typeRules: [],
					propertyRules: propertyRules
				},
				metadata: {
					label: "Root Mapping"
				}
			}
		};
	};

	/**
	 * Getting the URL of copyRule
	 *
	 * @param sourceRule {string}
	 * @returns {{url: string}}
	 */
	const rulesDataURLCopyFromCopy = (sourceRule) => {
		return {
			url: `/transform/tasks/test/test/rule/root/rules/copyFrom?sourceProject=test&sourceTask=test&sourceRule=${sourceRule}&afterRuleId`
		};
	};

	/**
	 * Getting the URL of copyRule
	 *
	 * @param sourceRule {string}
	 * @returns {{url: string}}
	 */
	const rulesDataURLCopyFromClone = (sourceRule) => {
		return {
			url: `/transform/tasks/test/test/rule/root/rules/copyFrom?sourceProject=test&sourceTask=test&sourceRule=${sourceRule}&afterRuleId=country`
		};
	};

	/**
	 * Getting the response for copyRule
	 *
	 * @param testCase {string}
	 * @returns {{code: number, body: {metadata: {label: string}, id: string, type: string, sourcePath: string, mappingTarget: {isAttribute: boolean, valueType: {nodeType: string}, uri: string, isBackwardProperty: boolean}}}}
	 */
	const getRulesDataURLCopyFromResponse = (testCase) => {
		return {
			code: 200,
			body: {
				type: "direct",
				id: `${testCase}1`,
				sourcePath: `dbpediaowl:${testCase}`,
				mappingTarget: {
					uri: `<urn:ruleProperty:${testCase}>`,
					valueType: {
						nodeType: "StringValueType"
					},
					isBackwardProperty: false,
					isAttribute: false
				},
				metadata: {
					label: `Copy of urn:ruleProperty:${testCase}`
				}
			}
		};
	};

	/**
	 * Adding new property rule
	 *
	 * @param testCase
	 */
	const addNewPropertyRule = (testCase) => {
		propertyRules.push({
			type: "direct",
			id: `${identifier}1`,
			sourcePath: `dbpediaowl:${testCase}`,
			mappingTarget: {
				uri: `<urn:ruleProperty:${testCase}>`,
				valueType: {
					nodeType: "StringValueType"
				},
				isBackwardProperty: false,
				isAttribute: false
			},
			metadata: {
				label: `Copy of urn:ruleProperty:${testCase}`
			}
		});
	};
	/**
	 * @type {{url: string}}
	 */
	const payload = dataURL(identifier),

		/**
		 * @type {{code, body}}
		 */
		response = getResponse(identifier),

		/**
		 * @type {{url}}
		 */
		rulesPayload = rulesDataURL(),

		/**
		 * @type {{code, body}}
		 */
		rulesResponse = getRulesResponse(),

		/**
		 * @type {{url}}
		 */
		rulesDataURLCopyFromPayload = rulesDataURLCopyFromCopy(identifier),

		/**
		 * @type {{url}}
		 */
		rulesDataURLCopyFromPayloadClone = rulesDataURLCopyFromClone(identifier),

		/**
		 * @type {{code, body}}
		 */
		rulesDataURLCopyFromResponse = getRulesDataURLCopyFromResponse(identifier);

	nock(baseUrl)
		.post(payload.url)
		.reply(response.code, () => response.body)

		.get(rulesPayload.url)
		.reply(rulesResponse.code, () => rulesResponse.body)

		.post(rulesDataURLCopyFromPayload.url)
		.reply(rulesDataURLCopyFromResponse.code, () => {
			addNewPropertyRule(identifier);
			rulesResponse.body.rules.propertyRules = propertyRules;
			return rulesDataURLCopyFromResponse.body;
		})

		.post(rulesDataURLCopyFromPayloadClone.url)
		.reply(rulesDataURLCopyFromResponse.code, () => {
			addNewPropertyRule(identifier);
			rulesResponse.body.rules.propertyRules = propertyRules;
			return rulesDataURLCopyFromResponse.body;
		});
};

// mock up testCases cases
mockUpFunction("basedOn");

mockUpFunction('country');

export {
	propertyRules
};
