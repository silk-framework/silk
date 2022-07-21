import "regenerator-runtime/runtime";
import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";

configure({ adapter: new Adapter() });

if (window.document) {
    (window.document.body as any).createTextRange = function() {
        return {
            setEnd: function(){},
            setStart: function(){},
            getBoundingClientRect: function(){
                return {right: 0};
            },
            getClientRects: function(){
                return {
                    length: 0,
                    left: 0,
                    right: 0
                }
            }
        }
    }
}
