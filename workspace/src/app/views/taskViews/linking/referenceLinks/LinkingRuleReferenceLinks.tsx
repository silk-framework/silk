import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    ContextMenu,
    Checkbox,
    Divider,
    Icon,
    IconButton,
    MenuItem,
    Spacing,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection, Tooltip, ConfidenceValue,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import React from "react";
import { useTranslation } from "react-i18next";
import { LabelProperties } from "./LinkingRuleReferenceLinks.typing";
import {
    ActiveLearningReferenceLink,
    ActiveLearningReferenceLinks,
    UnlabeledEntityLink,
} from "../activeLearning/LinkingRuleActiveLearning.typings";
import { EntityLinkUrisModal } from "./EntityLinkUrisModal";

interface LinkingRuleReferenceLinksProps {
    /** Title that is shown in the header. */
    title?: string;
    /** If the reference links are being updated/loaded from the backend. */
    loading?: boolean;
    /** Reference links to display. */
    referenceLinks?: ActiveLearningReferenceLinks;
    /** The paths whose values should be displayed as entity labels. If missing the first value is shown.of each entity. */
    labelPaths?: LabelProperties;
    /** Remove a link from the list. */
    removeLink: (link: ActiveLearningReferenceLink) => any;
    /** When defined an "open" icon exists that calls this function. */
    openLink?: (link: ActiveLearningReferenceLink) => any;
    /** If exists then the component allows to show either labeled or unlabeled links. */
    showLinkType?: (type: ReferenceLinkTypeFilterValue) => any;
}

type ReferenceLinkTypeFilterValue = "labeled" | "unlabeled";

type AnnotatedReferenceLink = ActiveLearningReferenceLink & {
    misMatch: boolean;
};

/** Displays reference links of a linking rule. */
export const LinkingRuleReferenceLinks = ({
    loading,
    referenceLinks,
    labelPaths,
    removeLink,
    openLink,
    showLinkType,
    title,
}: LinkingRuleReferenceLinksProps) => {
    const [showLinksList, setShowLinksList] = React.useState(false);
    const [showOnlyMismatches, setShowOnlyMismatches] = React.useState(false);
    const [activeTableRow, setActiveTableRow] = React.useState<ActiveLearningReferenceLink | undefined>(undefined);
    // Show uncertain links instead of decision history.
    const [showUncertainLinks, setShowUncertainLinks] = React.useState(false);
    const [showConfirmedOnly, setShowConfirmedOnly] = React.useState(false);
    const [showDeclinedOnly, setShowDeclinedOnly] = React.useState(false);
    const [entityUrisToOpenInModal, setEntityUrisToOpenInModal] = React.useState<UnlabeledEntityLink | undefined>(
        undefined
    );
    const [pagination, paginationElement, onTotalChange] = usePagination({
        initialPageSize: 10,
        pageSizes: [5, 10, 20, 50],
        presentation: { hideInfoText: true },
    });
    const [referenceLinksFiltered, setReferenceLinksFiltered] = React.useState<AnnotatedReferenceLink[] | undefined>(
        undefined
    );
    const [t] = useTranslation();
    const misMatches = (referenceLinksFiltered ?? []).filter((link) => link.misMatch).length;
    const positiveLinks = (referenceLinks?.links ?? []).filter((l) => l.decision === "positive").length;
    const negativeLinks = (referenceLinks?.links ?? []).filter((l) => l.decision === "negative").length;
    const showLinksOfType = (type: ReferenceLinkTypeFilterValue) => {
        showLinkType && showLinkType(type);
        switch (type) {
            case "labeled":
                setShowUncertainLinks(false);
                break;
            case "unlabeled":
                setShowUncertainLinks(true);
                break;
        }
    };

    // Annotate and filter reference links
    React.useEffect(() => {
        const referenceLinksAnnotated: AnnotatedReferenceLink[] = (referenceLinks?.links ?? []).map((l) => ({
            ...l,
            decision: l.decision ?? "unlabeled",
            misMatch:
                (l.decision === "positive" || l.decision === "negative") && l.score
                    ? (l.score < 0 && l.decision === "positive") || (l.score >= 0 && l.decision === "negative")
                    : false,
        }));
        setReferenceLinksFiltered(
            referenceLinksAnnotated.filter((link) => {
                const typeFiltered = !(
                    (showDeclinedOnly && link.decision === "positive") ||
                    (showConfirmedOnly && link.decision === "negative")
                );
                const misMatchFiltered = !showOnlyMismatches || link.misMatch;
                return typeFiltered && misMatchFiltered;
            })
        );
        setActiveTableRow(undefined);
    }, [referenceLinks, showOnlyMismatches, showConfirmedOnly, showDeclinedOnly]);

    if (referenceLinksFiltered && pagination.total !== referenceLinksFiltered.length) {
        onTotalChange(referenceLinksFiltered.length);
    }

    const positiveLinkCount = positiveLinks.toString() ?? "-";
    const negativeLinkCount = negativeLinks.toString() ?? "-";
    const ReferenceLinksHeader = () => {
        return (
            <CardHeader>
                <CardTitle>
                    {title} {referenceLinks?.links.length ? ` (${referenceLinks?.links.length})` : ""}
                </CardTitle>
                <CardOptions>
                    {showLinksList && (
                        <>
                            {!showUncertainLinks ? (
                                <>
                                    <Button
                                        small
                                        data-test-id={"reference-links-show-confirmed-links"}
                                        elevated={showConfirmedOnly}
                                        tooltip={t("ReferenceLinks.confirmedOnlyTooltip")}
                                        disabled={!showConfirmedOnly && positiveLinks <= 0}
                                        onClick={() => {
                                            setShowConfirmedOnly(!showConfirmedOnly);
                                            setShowDeclinedOnly(false);
                                        }}
                                    >
                                        {t("ReferenceLinks.confirmedOnly", { nr: positiveLinkCount })}
                                    </Button>
                                    <Button
                                        small
                                        data-test-id={"reference-links-show-declined-links"}
                                        elevated={showDeclinedOnly}
                                        tooltip={t("ReferenceLinks.declinedOnlyTooltip")}
                                        disabled={!showDeclinedOnly && negativeLinks <= 0}
                                        onClick={() => {
                                            setShowDeclinedOnly(!showDeclinedOnly);
                                            setShowConfirmedOnly(false);
                                        }}
                                    >
                                        {t("ReferenceLinks.declinedOnly", { nr: negativeLinkCount })}
                                    </Button>
                                    <Spacing vertical={true} size="small" />
                                </>
                            ) : null}
                            {!!showLinkType ? (
                                <>
                                    <Button
                                        size={"small"}
                                        key={"certain"}
                                        tooltip={t("ReferenceLinks.certainOnlyTooltip")}
                                        data-test-id={"reference-links-show-certain-links"}
                                        elevated={!showUncertainLinks}
                                        onClick={() => showLinksOfType("labeled")}
                                    >
                                        {t("ReferenceLinks.certainOnly")}
                                    </Button>
                                    <Button
                                        size={"small"}
                                        key={"uncertain"}
                                        tooltip={t("ReferenceLinks.uncertainOnlyTooltip")}
                                        data-test-id={"reference-links-show-uncertain-links"}
                                        elevated={showUncertainLinks}
                                        onClick={() => showLinksOfType("unlabeled")}
                                    >
                                        {t("ReferenceLinks.uncertainOnly")}
                                    </Button>
                                    <Spacing vertical={true} size="small" />
                                </>
                            ) : null}
                            <Tooltip content={t("ReferenceLinks.mismatchCheckboxTooltip")}>
                                <Checkbox
                                    data-test-id={"reference-links-show-mismatches"}
                                    disabled={misMatches <= 0}
                                    checked={showOnlyMismatches}
                                    onChange={() => setShowOnlyMismatches((prev) => !prev)}
                                    style={{margin: "0px"}}
                                >
                                    {t("ReferenceLinks.mismatchCheckboxTitle", {nrMismatches: misMatches})}
                                </Checkbox>
                            </Tooltip>
                            <Spacing vertical={true} size="small" />
                        </>
                    )}
                    <IconButton
                        data-test-id={"reference-links-toggler-btn"}
                        onClick={() => setShowLinksList((prev) => !prev)}
                        name={showLinksList ? "toggler-showless" : "toggler-showmore"}
                        text={showLinksList ? t("ReferenceLinks.hideList") : t("ReferenceLinks.showList")}
                    />
                </CardOptions>
            </CardHeader>
        );
    };

    const entityLabels = (link: AnnotatedReferenceLink): [string, string] => {
        const { sourceProperties, targetProperties } = labelPaths ?? {};
        const extractLabel = (ofTarget: boolean): string => {
            const properties = ofTarget ? targetProperties : sourceProperties;
            const pathValues = link.comparisons.map((comparison) => {
                return {
                    path: ofTarget ? comparison.target : comparison.source,
                    examples: ofTarget ? comparison.targetExamples : comparison.sourceExamples,
                };
            });
            if (properties) {
                const labelValues = properties.map((property, idx) => {
                    const match = pathValues.find((pv) => pv.path.path === property);
                    if (match) {
                        return match.examples;
                    } else {
                        return pathValues[idx]?.examples ?? [];
                    }
                });
                return labelValues.flat().join(", ");
            } else {
                return (pathValues[0]?.examples ?? []).flat().join(", ");
            }
        };
        return [extractLabel(false), extractLabel(true)];
    };

    const openEntityUrisModal = (link: UnlabeledEntityLink) => {
        setEntityUrisToOpenInModal(link);
    };

    const ReferenceLinksTable = () => {
        return (
            <>
                <Table columnWidths={["30px", "30px", "35%", "35%", "90px", "100px"]}>
                    <TableHead>
                        <TableRow>
                            <TableHeader key={"marker-column"}>&nbsp;</TableHeader>
                            <TableHeader key={"warning-column"}>&nbsp;</TableHeader>
                            <TableHeader>{t("ActiveLearning.config.entitiyPair.sourceColumnTitle")}</TableHeader>
                            <TableHeader>{t("ActiveLearning.config.entitiyPair.targetColumnTitle")}</TableHeader>
                            <TableHeader>{t("linkingEvaluationTabView.table.header.score")}</TableHeader>
                            <TableHeader key={"actions-column"} style={{ width: "1px" }}>
                                &nbsp;
                            </TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {referenceLinksFiltered
                            ? referenceLinksFiltered
                                  .slice(
                                      (pagination.current - 1) * pagination.limit,
                                      pagination.current * pagination.limit
                                  )
                                  .map((link, rowIdx) => {
                                      const [sourceLabel, targetLabel] = entityLabels(link);
                                      return (
                                          <TableRow key={rowIdx} isSelected={openLink && activeTableRow === link}>
                                              <TableCell>
                                                  <Icon
                                                      name={
                                                          link.decision !== "unlabeled"
                                                              ? link.decision === "positive"
                                                                  ? "state-confirmed"
                                                                  : "state-declined"
                                                              : "item-question"
                                                      }
                                                      tooltipText={
                                                          link.decision !== "unlabeled"
                                                              ? link.decision === "positive"
                                                                  ? t("ReferenceLinks.confirmed")
                                                                  : t("ReferenceLinks.declined")
                                                              : t("ReferenceLinks.uncertainOnly")
                                                      }
                                                  />
                                              </TableCell>
                                              <TableCell>
                                                  {link.decision !== "unlabeled" && link.misMatch ? (
                                                      <Icon
                                                          name={"state-warning"}
                                                          color={"red"}
                                                          tooltipText={t("ReferenceLinks.mismatchWithRule")}
                                                      />
                                                  ) : null}
                                              </TableCell>
                                              <TableCell>{sourceLabel}</TableCell>
                                              <TableCell>{targetLabel}</TableCell>
                                              <TableCell>
                                                  {link.score ? <ConfidenceValue
                                                          value={link.score}
                                                      /> :
                                                      "N/A"
                                                  }
                                              </TableCell>
                                              <TableCell>
                                                  <Toolbar>
                                                      <ToolbarSection>
                                                          <IconButton
                                                              name={"item-remove"}
                                                              text={t("ReferenceLinks.removeLink")}
                                                              disruptive={true}
                                                              onClick={() => removeLink(link)}
                                                          />
                                                          {openLink ? (
                                                              <IconButton
                                                                  name={"item-viewdetails"}
                                                                  text={t("ReferenceLinks.checkFeedback")}
                                                                  onClick={() => {
                                                                      openLink(link);
                                                                      setActiveTableRow(link);
                                                                  }}
                                                              />
                                                          ) : null}
                                                          <ContextMenu
                                                              data-test-id={`reference-link-more-menu-${rowIdx}`}
                                                              togglerText={t(
                                                                  "common.action.moreOptions",
                                                                  "Show more options"
                                                              )}
                                                          >
                                                              <MenuItem
                                                                  data-test-id="show-entity-uris"
                                                                  icon="item-viewdetails"
                                                                  onClick={() => openEntityUrisModal(link)}
                                                                  text={t("ReferenceLinks.showEntityUris.menuText")}
                                                              />
                                                          </ContextMenu>
                                                      </ToolbarSection>
                                                  </Toolbar>
                                              </TableCell>
                                          </TableRow>
                                      );
                                  })
                            : null}
                    </TableBody>
                </Table>
                {paginationElement}
            </>
        );
    };

    return (
        <Card elevation={0} data-test-id={"linking-reference-links"}>
            <ReferenceLinksHeader />
            {showLinksList && (
                <>
                    <Divider />
                    <CardContent>{loading ? <Spinner /> : <ReferenceLinksTable />}</CardContent>
                </>
            )}
            {entityUrisToOpenInModal ? (
                <EntityLinkUrisModal
                    link={entityUrisToOpenInModal}
                    onClose={() => setEntityUrisToOpenInModal(undefined)}
                />
            ) : null}
        </Card>
    );
};
