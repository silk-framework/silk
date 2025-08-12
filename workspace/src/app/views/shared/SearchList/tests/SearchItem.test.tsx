import React from "react";
import { shallow, ShallowWrapper } from "enzyme";
import SearchItem from "../SearchItem";
import { createMemoryHistory } from "history";
import { byTestId, clickElement, testWrapper, withMount } from "../../../../../../test/integration/TestHelper";
import { CreateArtefactModal } from "../../modals/CreateArtefactModal/CreateArtefactModal";
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

    const provider = testWrapper(
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
    return withMount(provider);
};

describe("Project Row Component", () => {
    it("should duplicate button fire the onOpenDuplicateModal function", () => {
        const wrapper = getWrapper();
        clickElement(wrapper, byTestId("open-duplicate-modal"));
        expect(onOpenDuplicateModalFn).toHaveBeenCalled();
    });
});
