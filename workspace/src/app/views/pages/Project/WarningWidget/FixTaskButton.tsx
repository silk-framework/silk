import { IconButton } from "@eccenca/gui-elements";
import React, { useCallback, useState } from "react";

interface FixTaskButtonProps {
    text: string;
    handleClick: () => Promise<any> | any;
}
export const FixTaskButton = ({ text, handleClick }: FixTaskButtonProps) => {
    const [loading, setLoading] = useState(false);

    const handleFix = useCallback(async () => {
        setLoading(true);
        try {
            await handleClick();
        } finally {
            setLoading(false);
        }
    }, []);

    return (
        <IconButton
            name={"operation-fix"}
            data-test-id={"taskLoadingErrorFixBtn"}
            minimal
            text={text}
            onClick={handleFix}
            loading={loading}
        />
    );
};
