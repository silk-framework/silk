import React from "react";
import { shallow, mount } from 'enzyme';
import {
    ContextMenu,
    BreadcrumbList,
    BreadcrumbItem,
    MenuItem,
} from '@eccenca/gui-elements';

import MappingHeader from '../../../src/HierarchicalMapping/containers/MappingHeader';
import ArrowBackButton from '../../../src/HierarchicalMapping/elements/buttons/ArrowBack';

const onRuleIdChangeFn = jest.fn();
const onToggleTreeNavFn = jest.fn();
const onToggleDetailsFn = jest.fn();

const props = {
    rule: {
        id: 'children'
    },
    onRuleIdChange: onRuleIdChangeFn,
    onToggleTreeNav: onToggleTreeNavFn,
    onToggleDetails: onToggleDetailsFn,
};

const selectors = {
    TREE_NAV: '.ecc-silk-mapping__ruleslistmenu__item-toggletree',
    BACK_BTN: 'button[data-button-id="back"]',
    EXPAND_ALL_BTN: '.ecc-silk-mapping__ruleslistmenu__item-expand',
    REDUCE_ALL_BTN: '.ecc-silk-mapping__ruleslistmenu__item-reduce',
    BREADCRUMB_ITEM: '[data-test-selector="breadcrumb-item"]',
};

const getWrapper = (renderer = shallow, args = props) => renderer(
    <MappingHeader {...args} />
);

describe("MappingHeader Component", () => {
    describe("on component mounted, ",() => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should nothing rendered when rule is empty", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {}
            });
            expect(wrapper.get(0)).toBeFalsy();
        });
    
        it("should render ArrowBackButton, when parent is presented in breadcrumbs", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {
                    breadcrumbs: [
                        {
                            id: "root",
                            property: false,
                            type: "fibo-fnd-tim-tim:Day"
                        }
                    ]
                }
            });
            expect(wrapper.find(ArrowBackButton)).toHaveLength(1);
        });
    
        it("should render breadcrumb, when breadcrumbs presented", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {
                    breadcrumbs: [
                        {
                            id: "root",
                            property: false,
                            type: "fibo-fnd-tim-tim:Day"
                        }
                    ]
                }
            });
            expect(wrapper.find(BreadcrumbItem)).toHaveLength(2);
        });
    
        it("should menu items rendered", () => {
            expect(wrapper.find(MenuItem)).toHaveLength(3);
        });
    
        afterEach(() => {
            wrapper.unmount();
        })
    });
    
    describe('on user interaction', () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it('should ArrowBackButton navigate back', () => {
            const wrapper = getWrapper(mount, {
                ...props,
                rule: {
                    ...props.rule,
                    breadcrumbs: [
                        {
                            id: "root",
                            property: false,
                            type: "fibo-fnd-tim-tim:Day"
                        }
                    ]
                }
            });
            wrapper.find(selectors.BACK_BTN).simulate('click');
            expect(onRuleIdChangeFn).toBeCalledWith({ newRuleId: 'root', parentId: 'children' })
        });
    
        it('should BreadCrumb navigate the page', () => {
            const wrapper = getWrapper(mount, {
                ...props,
                rule: {
                    id: 'root',
                    breadcrumbs: [
                        {
                            id: "someId",
                            property: false,
                            type: "fibo-fnd-tim-tim:Day"
                        }
                    ]
                }
            });
            wrapper.find(selectors.BREADCRUMB_ITEM).last().simulate('click');
            expect(onRuleIdChangeFn).toBeCalledWith({ newRuleId: 'someId', parentId: 'root' })
        });
    
        it('should tree navigation collapse when click on button', () => {
            const wrapper = getWrapper(mount);
            wrapper.find(selectors.TREE_NAV).first().simulate('click');
            expect(onToggleTreeNavFn).toBeCalled()
        });
    
        it('should expand all button working correctly', () => {
            const wrapper = getWrapper(mount);
            wrapper.find(selectors.EXPAND_ALL_BTN).first().simulate('click');
            expect(onToggleDetailsFn).toBeCalledWith({expanded: true})
        });
    
        it('should reduce all button working correctly', () => {
            const wrapper = getWrapper(mount);
            wrapper.find(selectors.REDUCE_ALL_BTN).first().simulate('click');
            expect(onToggleDetailsFn).toBeCalledWith({expanded: false})
        });
    
        afterEach(() => {
            wrapper.unmount();
        })
    })
});
