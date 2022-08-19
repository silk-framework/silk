import {
    Button,
    Card,
    Checkbox,
    Divider,
    Icon,
    IconButton,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import React from "react";
import { useTranslation } from "react-i18next";
import { IEntityLink } from "../linking.types";
import { EntityLink, LabelProperties, ReferenceLinksOrdered } from "./LinkingRuleReferenceLinks.typing";
import referenceLinksUtils from "./LinkingRuleReferenceLinks.utils";
import { ActiveLearningDecisions } from "../activeLearning/LinkingRuleActiveLearning.typings";

interface LinkingRuleReferenceLinksProps {
    /** Title that is shown in the header. */
    title?: string;
    /** If the reference links are being updated/loaded from the backend. */
    loading?: boolean;
    /** Reference links to display. */
    referenceLinks?: ReferenceLinksOrdered;
    /** The paths whose values should be displayed as entity labels. If missing the first value is shown.of each entity. */
    labelPaths?: LabelProperties;
    /** Remove a link from the list. */
    removeLink: (link: EntityLink) => any;
    /** When defined an "open" icon exists that calls this function. */
    openLink?: (link: EntityLink) => any;
    /** If exists then the component allows to show either labeled or unlabeled links. */
    showLinkType?: (type: ReferenceLinkTypeFilterValue) => any;
}

type ReferenceLinkTypeFilterValue = "labeled" | "unlabeled";

type AnnotatedReferenceLink = IEntityLink & {
    misMatch: boolean;
    type: ActiveLearningDecisions;
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
    // Show uncertain links instead of decision history.
    const [showUncertainLinks, setShowUncertainLinks] = React.useState(false);
    const [showConfirmedOnly, setShowConfirmedOnly] = React.useState(false);
    const [showDeclinedOnly, setShowDeclinedOnly] = React.useState(false);
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
            type: l.decision ?? "unlabeled",
            misMatch: l.confidence ? l.confidence < 0 : false,
        }));
        setReferenceLinksFiltered(
            referenceLinksAnnotated.filter((link) => {
                const typeFiltered = !(
                    (showDeclinedOnly && link.type === "positive") ||
                    (showConfirmedOnly && link.type === "negative")
                );
                const misMatchFiltered = !showOnlyMismatches || link.misMatch;
                return typeFiltered && misMatchFiltered;
            })
        );
    }, [referenceLinks, showOnlyMismatches, showConfirmedOnly, showDeclinedOnly]);

    if (referenceLinksFiltered && pagination.total !== referenceLinksFiltered.length) {
        onTotalChange(referenceLinksFiltered.length);
    }

    const positiveLinkCount = positiveLinks.toString() ?? "-";
    const negativeLinkCount = negativeLinks.toString() ?? "-";
    const ReferenceLinksHeader = () => {
        return (
            <Toolbar onClick={() => setShowLinksList((prev) => !prev)}>
                <ToolbarSection>
                    <Spacing vertical={true} size={"tiny"} />
                    <OverviewItem>
                        <OverviewItemDescription>
                            <OverviewItemLine large={true}>{title}</OverviewItemLine>
                        </OverviewItemDescription>
                    </OverviewItem>
                </ToolbarSection>
                <ToolbarSection canGrow={true} />
                <ToolbarSection onClick={(e) => e.stopPropagation()} style={{ verticalAlign: "center" }}>
                    {!showUncertainLinks ? (
                        <>
                            <Button
                                data-test-id={"reference-links-show-confirmed-links"}
                                elevated={showConfirmedOnly}
                                disabled={!showConfirmedOnly && positiveLinks <= 0}
                                onClick={() => {
                                    setShowConfirmedOnly(!showConfirmedOnly);
                                    setShowDeclinedOnly(false);
                                }}
                            >
                                {t("ReferenceLinks.confirmedOnly", { nr: positiveLinkCount })}
                            </Button>
                            <Button
                                data-test-id={"reference-links-show-declined-links"}
                                elevated={showDeclinedOnly}
                                disabled={!showDeclinedOnly && negativeLinks <= 0}
                                onClick={() => {
                                    setShowDeclinedOnly(!showDeclinedOnly);
                                    setShowConfirmedOnly(false);
                                }}
                            >
                                {t("ReferenceLinks.declinedOnly", { nr: negativeLinkCount })}
                            </Button>
                            <Spacing vertical={true} />
                        </>
                    ) : null}
                    {!!showLinkType ? (
                        <>
                            <Button
                                key={"certain"}
                                data-test-id={"reference-links-show-certain-links"}
                                elevated={!showUncertainLinks}
                                onClick={() => showLinksOfType("labeled")}
                            >
                                Certain
                            </Button>
                            <Button
                                key={"uncertain"}
                                data-test-id={"reference-links-show-uncertain-links"}
                                elevated={showUncertainLinks}
                                onClick={() => showLinksOfType("unlabeled")}
                            >
                                Uncertain
                            </Button>
                        </>
                    ) : null}

                    <Spacing vertical={true} />
                    <Checkbox
                        data-test-id={"reference-links-show-mismatches"}
                        disabled={misMatches <= 0}
                        checked={showOnlyMismatches}
                        onChange={() => setShowOnlyMismatches((prev) => !prev)}
                        style={{ verticalAlign: "center" }}
                    >
                        {t("ReferenceLinks.mismatchCheckboxTitle", { nrMismatches: misMatches })}
                    </Checkbox>
                    <Spacing vertical={true} />
                    <IconButton
                        data-test-id={"reference-links-toggler-btn"}
                        onClick={() => setShowLinksList((prev) => !prev)}
                        name={showLinksList ? "toggler-showless" : "toggler-showmore"}
                        text={showLinksList ? t("ReferenceLinks.hideList") : t("ReferenceLinks.showList")}
                    />
                </ToolbarSection>
            </Toolbar>
        );
    };

    const ReferenceLinksTable = () => {
        return (
            <>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableHeader key={"marker-column"} style={{ width: "1px" }}>
                                &nbsp;
                            </TableHeader>
                            <TableHeader key={"warning-column"} style={{ width: "1px" }}>
                                &nbsp;
                            </TableHeader>
                            <TableHeader>Dataset 1</TableHeader>
                            <TableHeader>Dataset 2</TableHeader>
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
                                      const entityLink = referenceLinksUtils.toReferenceEntityLink(link);
                                      if (entityLink) {
                                          const sourceLabel = referenceLinksUtils.entityValuesConcatenated(
                                              entityLink.source,
                                              labelPaths?.sourceProperties
                                          );
                                          const targetLabel = referenceLinksUtils.entityValuesConcatenated(
                                              entityLink.target,
                                              labelPaths?.targetProperties
                                          );
                                          return (
                                              <TableRow key={rowIdx}>
                                                  <TableCell>
                                                      <Icon
                                                          name={
                                                              link.decision !== "unlabeled"
                                                                  ? "state-checked"
                                                                  : "item-question"
                                                          }
                                                      />
                                                  </TableCell>
                                                  <TableCell>
                                                      {link.decision !== "unlabeled" && link.misMatch ? (
                                                          <Icon name={"state-warning"} color={"red"} />
                                                      ) : null}
                                                  </TableCell>
                                                  <TableCell>{sourceLabel}</TableCell>
                                                  <TableCell>{targetLabel}</TableCell>
                                                  <TableCell>
                                                      <Toolbar>
                                                          <ToolbarSection>
                                                              <IconButton
                                                                  name={"item-remove"}
                                                                  disruptive={true}
                                                                  onClick={() => removeLink(entityLink)}
                                                              />
                                                              {openLink ? (
                                                                  <IconButton
                                                                      name={"item-viewdetails"}
                                                                      onClick={() => openLink(entityLink)}
                                                                  />
                                                              ) : null}
                                                          </ToolbarSection>
                                                      </Toolbar>
                                                  </TableCell>
                                              </TableRow>
                                          );
                                      } else {
                                          return null;
                                      }
                                  })
                            : null}
                    </TableBody>
                </Table>
                {paginationElement}
            </>
        );
    };

    return (
        <Card isOnlyLayout elevation={0} data-test-id={"best-learned-rule-visual"}>
            <ReferenceLinksHeader />
            {showLinksList && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        {loading ? <Spinner /> : <ReferenceLinksTable />}
                    </WhiteSpaceContainer>
                </>
            )}
        </Card>
    );
};
