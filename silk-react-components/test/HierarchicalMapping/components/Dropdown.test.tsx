import React from 'react';
import '@testing-library/jest-dom'
import {fireEvent, render} from '@testing-library/react'
import {CLASSPREFIX as eccgui} from "gui-elements/src/configuration/constants";
import {Dropdown, IDropdownProps} from '../../../src/HierarchicalMapping/components/AutoSuggestion/Dropdown'

let props:IDropdownProps,  mockOptions

describe("Dropdown Component", () => {
    beforeEach(() => {
        props  = {
            currentlyFocusedIndex: 0,
            loading: false,
            left:0,
            isOpen: false,
            options: [],
            itemToHighlight: () => {},
            onItemSelectionChange: () => {}
        }

        mockOptions = [
            {
                query:"organizations",
                from: 0,
                length: 13,
                value:"organizations/name"
            },
            {
                query:"",
                from: 13,
                length:0,
                value:"/"
            }
        ]
    })


    it("doesn't render when not focused", () => {
        const {container} = render(<Dropdown {...props} />)
        expect(container).toBeEmptyDOMElement()
    })

    it("should be able to display loading state, when fetching suggestions", () => {
        props = {
            ...props,
            loading: true,
            isOpen: true
        }
        const {getByText} = render(<Dropdown {...props} />)
        expect(getByText("Fetching suggestions")).toBeTruthy()
    })

    it("should render list when suggestions are fetched", () => {
       props = {
           ...props,
           loading: false,
           isOpen:true,
           options: mockOptions
       }
       const {container} = render(<Dropdown {...props} />)
       const dropdownItems =  container.getElementsByTagName("li")
       expect(container).not.toBeEmptyDOMElement()
       expect(dropdownItems.length).toBe(2)
    })

    it("should move horizontally for every cursor movement", () => {
       props = {
           ...props,
           options: mockOptions,
           loading: false,
           isOpen: true,
           left: 10
       }
       const {container} = render(<Dropdown {...props} />)
       const parentDiv:HTMLElement = container.querySelector(".ecc-auto-suggestion-box__dropdown")
       const leftOffset = Number(parentDiv.style.left.replace(/px$/,""));
       expect(leftOffset).toBe(props.left)
    })

    it("should have one active item at a time", () => {
        props = {
            ...props,
            loading: false,
            isOpen:true,
            options: mockOptions
        }
        const {container} = render(<Dropdown {...props} />)
        const activeListItems = Array.from(container.querySelectorAll("li .bp3-menu-item.bp3-active"))
        expect(activeListItems.length).toBe(1)
    })

    it("should respond to click on each item and pass the clicked item to autosuggestion", () =>  {
        const mockOnItemSelection = jest.fn((item) => {})
        props = {
            ...props,
            loading: false,
            isOpen:true,
            options: mockOptions,
            onItemSelectionChange: mockOnItemSelection
        }
        const {getByText} =  render(<Dropdown {...props}/>)
        const dropdownListItem =  getByText(props.options[0].query).closest(`.${eccgui}-menu__item`);
        fireEvent.click(dropdownListItem)
        expect(mockOnItemSelection).toHaveBeenCalledTimes(1)
        expect(mockOnItemSelection).toHaveBeenCalledWith(props.options[0])
    })

    it("should call highlight function when list item is mouse hovered",() => {
        const mockItemToHighlight = jest.fn((item) => {})
        props = {
            ...props,
            loading: false,
            isOpen:true,
            options: mockOptions,
            itemToHighlight: mockItemToHighlight
        }
        const {container} =  render(<Dropdown {...props}/>)
        const firstItem =  container.querySelector("li");
        fireEvent.mouseEnter(firstItem)
        expect(mockItemToHighlight).toHaveBeenCalledWith(props.options[0])
        expect(mockItemToHighlight).toHaveBeenCalledTimes(1)
        fireEvent.mouseLeave(firstItem)
        expect(mockItemToHighlight).toHaveBeenCalledTimes(1)
    })

    afterAll(() => {
        props = null, mockOptions = []
    })


})
