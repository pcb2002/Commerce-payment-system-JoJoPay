import { useState, useEffect } from "react";
import {
    getMySubscription,
    startSubscription,
    cancelSubscription,
    getMySubscriptionBillings,
} from "../../api/subscriptionApi.js";
import BillingKeySection from "./BillingKeySection.jsx";

const PLANS = ["BASIC", "STANDARD", "PREMIUM"];

export default function SubscriptionPage() {
    const [subscription, setSubscription] = useState(null);
    const [billings, setBillings] = useState([]);
    const [selectedBillingKeyId, setSelectedBillingKeyId] = useState(null);
    const [selectedPlan, setSelectedPlan] = useState("BASIC");
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [tab, setTab] = useState("info"); // info | billings

    const fetchSubscription = () =>
        getMySubscription()
            .then((res) => setSubscription(res.data.data))
            .catch(() => setSubscription(null));

    const fetchBillings = () =>
        getMySubscriptionBillings()
            .then((res) => setBillings(res.data.data ?? []));

    useEffect(() => {
        fetchSubscription();
        fetchBillings();
    }, []);

    const handleStart = async () => {
        setErrorMsg("");
        if (!selectedBillingKeyId) {
            setErrorMsg("결제수단을 선택해주세요.");
            return;
        }
        setLoading(true);
        try {
            await startSubscription(selectedBillingKeyId, selectedPlan);
            fetchSubscription();
        } catch (err) {
            setErrorMsg(err.response?.data?.message || "구독 시작 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!window.confirm("구독을 해지하시겠습니까?")) return;
        setLoading(true);
        try {
            await cancelSubscription();
            fetchSubscription();
        } catch (err) {
            setErrorMsg(err.response?.data?.message || "해지 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const isActive = subscription?.status === "ACTIVE";

    return (
        <div className="container mt-4" style={{ maxWidth: 640 }}>
            <h4 className="mb-4">구독 관리</h4>

            {/* 현재 구독 상태 */}
            <div className="card mb-4">
                <div className="card-body">
                    {subscription ? (
                        <>
                            <div className="d-flex justify-content-between align-items-center mb-2">
                                <span className="fw-semibold">{subscription.planName}</span>
                                <span className={`badge ${isActive ? "bg-success" : "bg-secondary"}`}>
                  {subscription.status}
                </span>
                            </div>
                            <div className="text-muted small mb-1">
                                월 {subscription.price?.toLocaleString()}원
                            </div>
                            <div className="text-muted small mb-3">
                                다음 결제일: {subscription.nextBillingDate ?? "-"}
                            </div>
                            {isActive && (
                                <button className="btn btn-sm btn-outline-danger" onClick={handleCancel} disabled={loading}>
                                    구독 해지
                                </button>
                            )}
                        </>
                    ) : (
                        <p className="text-muted mb-0">활성 구독이 없습니다.</p>
                    )}
                </div>
            </div>

            {/* 탭 */}
            <ul className="nav nav-tabs mb-3">
                <li className="nav-item">
                    <button className={`nav-link ${tab === "info" ? "active" : ""}`} onClick={() => setTab("info")}>
                        구독 신청
                    </button>
                </li>
                <li className="nav-item">
                    <button className={`nav-link ${tab === "billings" ? "active" : ""}`} onClick={() => setTab("billings")}>
                        결제 내역
                    </button>
                </li>
            </ul>

            {tab === "info" && (
                <>
                    <BillingKeySection
                        selectedBillingKeyId={selectedBillingKeyId}
                        onSelectBillingKey={setSelectedBillingKeyId}
                    />

                    <div className="mb-3">
                        <label className="form-label fw-semibold">플랜 선택</label>
                        <div className="d-flex gap-2">
                            {PLANS.map((plan) => (
                                <button
                                    key={plan}
                                    className={`btn btn-sm ${selectedPlan === plan ? "btn-primary" : "btn-outline-secondary"}`}
                                    onClick={() => setSelectedPlan(plan)}
                                >
                                    {plan}
                                </button>
                            ))}
                        </div>
                    </div>

                    {errorMsg && <div className="alert alert-danger py-2">{errorMsg}</div>}

                    <button
                        className="btn btn-primary w-100"
                        onClick={handleStart}
                        disabled={loading || isActive}
                    >
                        {isActive ? "이미 구독 중입니다" : loading ? "처리 중..." : "구독 시작"}
                    </button>
                </>
            )}

            {tab === "billings" && (
                <div>
                    {billings.length === 0 ? (
                        <p className="text-muted">결제 내역이 없습니다.</p>
                    ) : (
                        <ul className="list-group">
                            {billings.map((b) => (
                                <li key={b.subscriptionBillingId} className="list-group-item">
                                    <div className="d-flex justify-content-between">
                                        <span className="fw-semibold">{b.billingPeriod} (#{b.billingCycle}회차)</span>
                                        <span>{b.amount?.toLocaleString()}원</span>
                                    </div>
                                    <div className="d-flex justify-content-between text-muted small mt-1">
                                        <span>{b.billingStatus}</span>
                                        <span>{b.createdAt ? new Date(b.createdAt).toLocaleString("ko-KR") : "-"}</span>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>
    );
}