import "regenerator-runtime/runtime";

import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";

import { initTestHelpers } from "../test/integration/TestHelper";

initTestHelpers();

configure({ adapter: new Adapter() });

jest.setTimeout(30000);

if (window.document) {
    (window.document.body as any).createTextRange = function () {
        return {
            setEnd: function () {},
            setStart: function () {},
            getBoundingClientRect: function () {
                return { right: 0 };
            },
            getClientRects: function () {
                return {
                    length: 0,
                    left: 0,
                    right: 0,
                };
            },
        };
    };
}
