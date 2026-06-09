import { useState, useEffect } from "react";
import {
    getBillingKeys,
    registerBillingKey,
    deleteBillingKey,
} from "../../api/subscriptionApi.js";

export default function BillingKeySection({ onSelectBillingKey, selectedBillingKeyId }) {
    const [billingKeys, setBillingKeys] = useState([]);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState({ customerUid: "", cardName: "", cardNumber: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const fetchKeys = () =>
        getBillingKeys().then((res) => setBillingKeys(res.data.data ?? []));

    useEffect(() => {
        fetchKeys();
    }, []);

    const handleRegister = async () => {
        setErrorMsg("");
        if (!form.customerUid || !form.cardName || !form.cardNumber) {
            setErrorMsg("모든 필드를 입력해주세요.");
            return;
        }
        setLoading(true);
        try {
            await registerBillingKey(form.customerUid, form.cardName, form.cardNumber);
            setForm({ customerUid: "", cardName: "", cardNumber: "" });
            setShowForm(false);
            fetchKeys();
        } catch (err) {
            setErrorMsg(err.response?.data?.message || "등록 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (billingKeyId) => {
        if (!window.confirm("결제수단을 삭제하시겠습니까?")) return;
        try {
            await deleteBillingKey(billingKeyId);
            fetchKeys();
        } catch (err) {
            alert(err.response?.data?.message || "삭제 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="mb-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
                <h6 className="mb-0 fw-semibold">결제수단</h6>
                <button className="btn btn-sm btn-outline-primary" onClick={() => setShowForm((v) => !v)}>
                    {showForm ? "취소" : "+ 카드 등록"}
                </button>
            </div>

            {showForm && (
                <div className="card p-3 mb-3">
                    {["customerUid", "cardName", "cardNumber"].map((field) => (
                        <input
                            key={field}
                            className="form-control form-control-sm mb-2"
                            placeholder={
                                field === "customerUid" ? "Customer UID" : field === "cardName" ? "카드사" : "카드번호"
                            }
                            value={form[field]}
                            onChange={(e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))}
                        />
                    ))}
                    {errorMsg && <div className="text-danger small mb-2">{errorMsg}</div>}
                    <button className="btn btn-sm btn-primary w-100" onClick={handleRegister} disabled={loading}>
                        {loading ? "등록 중..." : "등록"}
                    </button>
                </div>
            )}

            {billingKeys.length === 0 ? (
                <p className="text-muted small">등록된 결제수단이 없습니다.</p>
            ) : (
                <div className="list-group">
                    {billingKeys.map((key) => (
                        <div
                            key={key.billingKeyId}
                            className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${
                                selectedBillingKeyId === key.billingKeyId ? "active" : ""
                            }`}
                            onClick={() => onSelectBillingKey(key.billingKeyId)}
                            style={{ cursor: "pointer" }}
                        >
                            <div>
                                <div className="fw-semibold">{key.cardName}</div>
                                <div className="small opacity-75">{key.cardNumber}</div>
                            </div>
                            <button
                                className="btn btn-sm btn-outline-danger"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleDelete(key.billingKeyId);
                                }}
                            >
                                삭제
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}