import React from "react";
import { EvaluatedTransformEntity } from "../transform/transform.types";

interface RuleBlockEvaluationOptionalContextProps {
    /** When defined, the evaluation view shows these externally provided results instead of running a backend evaluation. */
    externalEvaluationResults?: EvaluatedTransformEntity[];
}

export const RuleBlockEvaluationOptionalContext = React.createContext<RuleBlockEvaluationOptionalContextProps>({});
