/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from "react";
import _ from "lodash";
import { copyRuleAsync, errorChannel, getApiDetails, getRuleAsync } from "../store";
import { Spinner, Spacing, Notification, ClassNames } from "@eccenca/gui-elements";
import RootMappingRule from "./RootMappingRule";
import ObjectMappingRuleForm from "./MappingRule/ObjectRule/ObjectRuleForm";
import ValueMappingRuleForm from "./MappingRule/ValueRule/ValueRuleForm";
import MappingsList from "./MappingsList/MappingsList";
import SuggestionsListContainer from "./SuggestionNew/SuggestionContainer";
import {
    isRootOrObjectRule,
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
    MESSAGES,
} from "../utils/constants";
import EventEmitter from "../utils/EventEmitter";
import { diErrorMessage } from "@ducks/error/typings";
import { IViewActions } from "../../../../plugins/PluginRegistry";
import { SuggestionNGProps } from "../../../../plugins/plugin.types";
import { ParentStructure } from "../components/ParentStructure";
import RuleTitle from "../elements/RuleTitle";
import { MAPPING_ROOT_RULE_ID } from "../HierarchicalMapping";

interface MappingsWorkviewProps {
    onRuleIdChange: (param: any) => any;
    onAskDiscardChanges: (param: any) => any;
    onClickedRemove: () => any;
    openMappingEditor: () => any;
    currentRuleId?: string; // selected rule id
    askForDiscardData: object | boolean; // selected rule id
    viewActions: IViewActions;
    startFullScreen: boolean;
}

const MappingsWorkview = ({
    onRuleIdChange,
    onAskDiscardChanges,
    onClickedRemove,
    openMappingEditor,
    currentRuleId,
    viewActions,
    startFullScreen,
}: MappingsWorkviewProps) => {
    const [loading, setLoading] = React.useState(true);
    const [ruleData, setRuleData] = React.useState<any>({});
    const [ruleEditView, setRuleEditorView] = React.useState<any>(false);
    const [editing, setEditing] = React.useState<any[]>([]);
    const [isCopying, setIsCopying] = React.useState(!!sessionStorage.getItem("copyingData"));
    const [showSuggestions, setShowSuggestions] = React.useState(false);
    const [selectedVocabs, setSelectedVocabs] = React.useState([]);
    const [error, setError] = React.useState<string | null | undefined>(undefined);

    React.useEffect(() => {
        loadData({ initialLoad: true });
        EventEmitter.on(MESSAGES.RELOAD, loadData);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, handleRuleEditClose);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, handleRuleEditClose);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, handleRuleEditOpen);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, discardAll);
        return () => {
            EventEmitter.off(MESSAGES.RELOAD, loadData);
            EventEmitter.off(MESSAGES.RULE_VIEW.UNCHANGED, handleRuleEditClose);
            EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, handleRuleEditClose);
            EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, handleRuleEditOpen);
            EventEmitter.off(MESSAGES.RULE_VIEW.DISCARD_ALL, discardAll);
        };
    }, [currentRuleId]);

    React.useEffect(() => {
        loadData();
    }, [currentRuleId]);

    const onRuleCreate = ({ type }) => {
        setRuleEditorView({
            type,
        });
    };

    const handleRuleEditOpen = ({ id }) => {
        if (!editing.includes(id)) {
            setEditing((e) => [...e, id]);
        }
    };

    const handleRuleEditClose = ({ id }) => {
        if (id === 0) {
            setRuleEditorView(false);
            setEditing((old) => old.filter((e) => e !== id));
        } else {
            setEditing((old) => old.filter((e) => e !== id));
        }
    };

    const discardAll = () => {
        setEditing([]);
        setShowSuggestions(false);
    };

    const handleShowSuggestions = () => {
        if (editing.length === 0) {
            setShowSuggestions(true);
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        } else {
            onAskDiscardChanges({
                suggestions: true,
            });
        }
    };

    const loadData = (params: any = {}) => {
        const { initialLoad = false, onFinish } = params;
        setLoading(true);
        setError(undefined);
        getRuleAsync(currentRuleId, true).subscribe(
            ({ rule }) => {
                if (initialLoad && currentRuleId && rule.id !== currentRuleId) {
                    let toBeOpened;

                    // If the currentRuleId equals the uriRule's id, we want to expand the object mapping
                    if (_.get(rule, "rules.uriRule.id") === currentRuleId) {
                        toBeOpened = rule.id;
                    } else {
                        // otherwise we want to expand the value mapping
                        toBeOpened = currentRuleId;
                    }
                    EventEmitter.emit(MESSAGES.RULE_VIEW.TOGGLE, {
                        expanded: true,
                        id: toBeOpened,
                    });
                }
                setRuleData(rule);
            },
            (err) => {
                setError(diErrorMessage(err));
                setLoading(false);
            },
            () => {
                if (onFinish) {
                    onFinish();
                }
                setLoading(false);
            },
        );
    };

    // sends event to expand / collapse all mapping rules
    const handleToggleRuleDetails = ({ expanded }) => {
        if (editing.length === 0 || expanded) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.TOGGLE, { expanded, id: true });
        } else {
            onAskDiscardChanges({
                expanded,
            });
        }
    };

    // jumps to selected rule as new center of view
    const handleCreate = ({ type }) => {
        if (editing.length === 0) {
            onRuleCreate({ type });
        } else {
            onAskDiscardChanges({
                type,
            });
        }
    };

    const handleCloseSuggestions = () => {
        setShowSuggestions(false);
        EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id: 0 });
    };

    const handleCopy = (id, type) => {
        errorChannel.subject("message.info").onNext({
            message: 'Mapping rule copied. Use "+" button to paste',
        });
        const apiDetails = getApiDetails();
        const copyingData = {
            project: apiDetails.project,
            transformTask: apiDetails.transformTask,
            id,
            type,
            cloning: false,
        };
        sessionStorage.setItem("copyingData", JSON.stringify(copyingData));
        setIsCopying((old) => !old);
    };

    const handlePaste = (cloning = false) => {
        const copiedData = sessionStorage.getItem("copyingData");
        if (!copiedData) {
            return;
        }
        const copyingData = JSON.parse(copiedData),
            { breadcrumbs, id } = ruleData;
        if (!_.isEmpty(copyingData)) {
            const data = {
                id:
                    breadcrumbs.length > 0 && isRootOrObjectRule(copyingData.type) && copyingData.cloning
                        ? breadcrumbs[breadcrumbs.length - 1].id
                        : id,
                queryParameters: {
                    sourceProject: copyingData.project,
                    sourceTask: copyingData.transformTask,
                    sourceRule: copyingData.id,
                    afterRuleId: copyingData.cloning ? copyingData.id : null,
                },
            };
            copyRuleAsync(data).subscribe((newRuleId) => {
                if (copyingData.type === MAPPING_RULE_TYPE_DIRECT || copyingData.type === MAPPING_RULE_TYPE_COMPLEX) {
                    sessionStorage.setItem("pastedId", newRuleId);
                } else if (
                    copyingData.type === MAPPING_RULE_TYPE_OBJECT ||
                    copyingData.type === MAPPING_RULE_TYPE_ROOT
                ) {
                    onRuleIdChange({ newRuleId });
                }
                if (cloning) {
                    sessionStorage.removeItem("copyingData");
                }
                EventEmitter.emit(MESSAGES.RELOAD, true);
            });
        }
    };

    const handleClone = (id, type, parent = false) => {
        const apiDetails = getApiDetails();
        const copyingData = {
            project: apiDetails.project,
            transformTask: apiDetails.transformTask,
            id,
            type,
            cloning: true,
            parentId: parent || currentRuleId,
        };
        sessionStorage.setItem("copyingData", JSON.stringify(copyingData));
        setIsCopying((old) => !old);
        handlePaste(true);
    };

    const handleAddNewRule = (callback) => {
        EventEmitter.emit(MESSAGES.RELOAD, {
            onFinish: callback,
        });
    };

    const handleVocabSelection = (vocabs) => {
        setSelectedVocabs(vocabs);
    };

    const { rules = {}, id } = ruleData;
    const loadingWidget = loading ? <Spinner position={"global"} /> : null;
    const createType = _.get(ruleEditView, "type", false);

    const createRuleForm = createType ? (
        <div className="ecc-silk-mapping__createrule">
            {createType === MAPPING_RULE_TYPE_OBJECT ? (
                <ObjectMappingRuleForm
                    parentId={ruleData.id}
                    parent={{
                        id: ruleData.id,
                        property: _.get(ruleData, "mappingTarget.uri"),
                        type: _.get(ruleData, "rules.typeRules[0].typeUri"),
                    }}
                    ruleData={{ type: MAPPING_RULE_TYPE_OBJECT }}
                    onAddNewRule={handleAddNewRule}
                    viewActions={viewActions}
                />
            ) : (
                <ValueMappingRuleForm
                    type={createType}
                    parentId={ruleData.id}
                    edit
                    onAddNewRule={handleAddNewRule}
                    openMappingEditor={openMappingEditor}
                    viewActions={viewActions}
                />
            )}
        </div>
    ) : (
        false
    );

    React.useEffect(() => {
        if (viewActions.addLocalBreadcrumbs && ruleData && loading === false) {
            const breadcrumbs = (ruleData.breadcrumbs ?? []).filter((b) => b.id !== MAPPING_ROOT_RULE_ID);
            const localBreadcrumbs = breadcrumbs.map((breadcrumb, idx) => {
                return {
                    text: <ParentStructure parent={breadcrumb} />,
                    href: "?ruleId=" + breadcrumb.id,
                    onClick: (event) => {
                        event.preventDefault();
                        onRuleIdChange({ newRuleId: breadcrumb.id, parentId: ruleData.id });
                        event.stopPropagation();
                    },
                };
            });
            if ((ruleData.breadcrumbs ?? []).length > 0) {
                localBreadcrumbs.push({
                    text: <RuleTitle rule={ruleData} />,
                });
            }
            viewActions.addLocalBreadcrumbs(localBreadcrumbs);
        }
    }, [ruleData]);

    const types =
        !createRuleForm && showSuggestions && _.has(ruleData, "rules.typeRules")
            ? _.map(ruleData.rules.typeRules, (v) => v.typeUri.replace("<", "").replace(">", ""))
            : [];

    // !createRuleForm && showSuggestions &&_.has(ruleData, 'rules.typeRules') &&
    const listSuggestions = !createRuleForm && showSuggestions && _.has(ruleData, "rules.typeRules") && (
        <SuggestionsListContainer
            ruleId={_.get(ruleData, "id", MAPPING_ROOT_RULE_ID)}
            onClose={handleCloseSuggestions}
            targetClassUris={types}
            onAskDiscardChanges={onAskDiscardChanges}
            selectedVocabs={selectedVocabs}
            setSelectedVocabs={handleVocabSelection}
        />
    );
    const listMappings =
        !createRuleForm && !listSuggestions ? (
            <MappingsList
                currentRuleId={currentRuleId ?? MAPPING_ROOT_RULE_ID}
                rules={_.get(rules, "propertyRules", [])}
                parentRuleId={id}
                handleCopy={handleCopy}
                handlePaste={handlePaste}
                handleClone={handleClone}
                isCopying={isCopying}
                onRuleIdChange={onRuleIdChange}
                onAskDiscardChanges={onAskDiscardChanges}
                onClickedRemove={onClickedRemove}
                onShowSuggestions={handleShowSuggestions}
                onMappingCreate={handleCreate}
                openMappingEditor={openMappingEditor}
                loading={loading}
                startFullScreen={startFullScreen}
                viewActions={viewActions}
            />
        ) : null;

    return (
        <div className="ecc-silk-mapping__rules">
            {loadingWidget}
            <RootMappingRule
                rule={ruleData}
                key={`objhead_${id}`}
                handleCopy={handleCopy}
                handleClone={handleClone}
                onAskDiscardChanges={onAskDiscardChanges}
                onClickedRemove={onClickedRemove}
                openMappingEditor={openMappingEditor}
                startFullScreen={startFullScreen}
                viewActions={viewActions}
            />
            {listSuggestions || <div className={ClassNames.Blueprint.elevationClass(1)}>{listMappings}</div>}
            {createRuleForm}
            {error ? (
                <>
                    <Spacing />
                    <Notification intent="warning" message={error} />
                </>
            ) : null}
        </div>
    );
};

export default MappingsWorkview;
