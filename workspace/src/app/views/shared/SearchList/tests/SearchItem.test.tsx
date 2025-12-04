import React from "react";
import SearchItem from "../SearchItem";
import { createMemoryHistory } from "history";
import { byTestId, clickFoundElement, renderWrapper } from "../../../../../../test/integration/TestHelper";
import { SERVE_PATH } from "../../../../constants/path";

const onOpenDeleteModalFn = jest.fn(),
    onOpenDuplicateModalFn = jest.fn(),
    onRowClickFn = jest.fn();

const item = {
    description: "",
    itemLinks: [
        { label: "Mapping editor", path: "/transform/CMEM/regression-845/editor" },
        { label: "Transform evaluation", path: "/transform/CMEM/regression-845/evaluate" },
        { label: "Transform execution", path: "/transform/CMEM/regression-845/execute" },
    ],
    id: "regression-845",
    label: "",
    type: "transform",
    projectId: "CMEM",
};

const getWrapper = (currentUrl: string = `${SERVE_PATH}`) => {
    const history = createMemoryHistory<{}>();
    history.push(currentUrl);
    return renderWrapper(
        <SearchItem
            item={item}
            onOpenDeleteModal={onOpenDeleteModalFn}
            onOpenDuplicateModal={onOpenDuplicateModalFn}
            onRowClick={onRowClickFn}
            onOpenCopyToModal={() => {}}
            toggleShowIdentifierModal={() => {}}
        />,
        history,
        {},
    );
};

describe("Project Row Component", () => {
    it("should duplicate button fire the onOpenDuplicateModal function", () => {
        const wrapper = getWrapper();
        clickFoundElement(wrapper, byTestId("open-duplicate-modal"));
        expect(onOpenDuplicateModalFn).toHaveBeenCalled();
    });
});
