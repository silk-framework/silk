import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import ProjectRow from "../ProjectRow";

const onOpenDeleteModalFn = jest.fn()
    , onOpenDuplicateModalFn = jest.fn()
    , onRowClickFn = jest.fn();

const item = {
    description: "",
    itemLinks: [
        {"label": "Mapping editor", "path": "/dataintegration/transform/CMEM/regression-845/editor"},
        {"label": "Transform evaluation", "path": "/dataintegration/transform/CMEM/regression-845/evaluate"},
        {"label": "Transform execution", "path": "/dataintegration/transform/CMEM/regression-845/execute"}
    ],
    id: "regression-845",
    label: "",
    type: 'transform',
    projectId: "CMEM"
};


const getWrapper = (renderer: Function = shallow): ShallowWrapper => {
    return renderer(<ProjectRow
        item={item}
        onOpenDeleteModal={onOpenDeleteModalFn}
        onOpenDuplicateModal={onOpenDuplicateModalFn}
        onRowClick={onRowClickFn}
    />);
};

describe('Project Row Component', () => {
    it('should duplicate button fire the onOpenDuplicateModal function', () => {
        getWrapper().find('[data-test-id="open-duplicate-modal"]').simulate('click');
        expect(onOpenDuplicateModalFn).toBeCalled();
    });
});
