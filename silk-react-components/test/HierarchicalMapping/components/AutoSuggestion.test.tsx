import React from "react";
import "@testing-library/jest-dom";
import { render } from "@testing-library/react";
import AutoSuggestion, {IProps} from "../../../src/HierarchicalMapping/components/AutoSuggestion/AutoSuggestion"

let props: IProps
describe("AutoSuggestion", () => {
   beforeEach(() => {
      props = {
          label:"test value path",
          initialValue:"",
          onChange: jest.fn((value) => {}),
          fetchSuggestions: jest.fn((inputString, cursorPosition) => undefined),
          checkInput: jest.fn(inputString => undefined),
          onInputChecked: jest.fn(validInput => {}),
          validationErrorText: "", 
          clearIconText:"",
          onFocusChange: jest.fn(hasFocus => {}),
          id:"test-auto-suggestion"
      }
   })

   it("should render properly", () => { 
       const {container} = render(<AutoSuggestion {...props} />) 
       expect(container).not.toBeEmptyDOMElement()
   })

   it("should set label prop properly", () => {
        const {getByText} = render(<AutoSuggestion {...props}/> )
        expect(getByText(props.label)).toBeTruthy()
   })

   afterAll(() => {
       props = null;
   })
})