import { useState, useEffect } from "react";
import { getPointBalance, getPointHistories } from "../../api/pointApi.js";

const TYPE_BADGE = {
    EARN: "bg-success",
    USE: "bg-danger",
    CANCEL: "bg-secondary",
};

export default function PointPage() {
    const [balance, setBalance] = useState(null);
    const [histories, setHistories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState("");

    useEffect(() => {
        Promise.all([getPointBalance(), getPointHistories()])
            .then(([balRes, histRes]) => {
                setBalance(balRes.data.data?.pointBalance ?? 0);
                setHistories(histRes.data.data ?? []);
            })
            .catch((err) => {
                setErrorMsg(err.response?.data?.message || "데이터를 불러오는 중 오류가 발생했습니다.");
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="container mt-5 text-center">
                <div className="spinner-border text-primary" role="status" />
            </div>
        );
    }

    return (
        <div className="container mt-4" style={{ maxWidth: 640 }}>
            <h4 className="mb-4">포인트</h4>

            {/* 잔액 카드 */}
            <div className="card mb-4 border-primary">
                <div className="card-body text-center">
                    <div className="text-muted small mb-1">현재 포인트 잔액</div>
                    <div className="fs-2 fw-bold text-primary">{balance?.toLocaleString()} P</div>
                </div>
            </div>

            {errorMsg && <div className="alert alert-danger py-2">{errorMsg}</div>}

            {/* 거래 내역 */}
            <h6 className="fw-semibold mb-3">거래 내역</h6>
            {histories.length === 0 ? (
                <p className="text-muted">거래 내역이 없습니다.</p>
            ) : (
                <ul className="list-group">
                    {histories.map((h) => (
                        <li key={h.id} className="list-group-item">
                            <div className="d-flex justify-content-between align-items-center">
                                <div className="d-flex align-items-center gap-2">
                  <span className={`badge ${TYPE_BADGE[h.transactionType] ?? "bg-secondary"}`}>
                    {h.transactionTypeDescription}
                  </span>
                                </div>
                                <span className={`fw-semibold ${h.transactionType === "USE" ? "text-danger" : "text-success"}`}>
                  {h.transactionType === "USE" ? "-" : "+"}{h.amount?.toLocaleString()} P
                </span>
                            </div>
                            <div className="text-muted small mt-1">
                                {h.createdAt ? new Date(h.createdAt).toLocaleString("ko-KR") : "-"}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}