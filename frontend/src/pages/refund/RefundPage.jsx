import { useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { refundOrder } from "../../api/refundApi.js";

export default function RefundPage() {
    const { orderId } = useParams();
    const { state } = useLocation(); // { orderNumber, items: [{orderItemId, productName, quantity, price}] }
    const navigate = useNavigate();

    const [reason, setReason] = useState("");
    const [quantities, setQuantities] = useState(
        () =>
            Object.fromEntries(
                (state?.items ?? []).map((item) => [item.orderItemId, item.quantity])
            )
    );
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const orderNumber = state?.orderNumber;
    const items = state?.items ?? [];

    const handleQuantityChange = (orderItemId, value) => {
        const max = items.find((i) => i.orderItemId === orderItemId)?.quantity ?? 1;
        const parsed = Math.max(1, Math.min(Number(value), max));
        setQuantities((prev) => ({ ...prev, [orderItemId]: parsed }));
    };

    const handleSubmit = async () => {
        setErrorMsg("");
        if (!reason.trim()) {
            setErrorMsg("환불 사유를 입력해주세요.");
            return;
        }
        const refundItems = items.map((item) => ({
            orderItemId: item.orderItemId,
            quantity: quantities[item.orderItemId],
        }));
        setLoading(true);
        try {
            await refundOrder(orderId, orderNumber, reason, refundItems);
            navigate(`/orders/${orderNumber}`, { replace: true });
        } catch (err) {
            setErrorMsg(err.response?.data?.message || "환불 처리 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    if (!orderNumber || items.length === 0) {
        return (
            <div className="container mt-5 text-center">
                <p className="text-muted">잘못된 접근입니다.</p>
                <button className="btn btn-outline-secondary" onClick={() => navigate("/orders")}>
                    주문 목록으로
                </button>
            </div>
        );
    }

    return (
        <div className="container mt-4" style={{ maxWidth: 600 }}>
            <h4 className="mb-4">환불 신청</h4>

            <div className="card mb-3">
                <div className="card-header text-muted small">주문 번호: {orderNumber}</div>
                <ul className="list-group list-group-flush">
                    {items.map((item) => (
                        <li key={item.orderItemId} className="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <div className="fw-semibold">{item.productName}</div>
                                <div className="text-muted small">
                                    {item.price?.toLocaleString()}원 / 최대 {item.quantity}개
                                </div>
                            </div>
                            <div style={{ width: 80 }}>
                                <input
                                    type="number"
                                    className="form-control form-control-sm text-center"
                                    min={1}
                                    max={item.quantity}
                                    value={quantities[item.orderItemId]}
                                    onChange={(e) => handleQuantityChange(item.orderItemId, e.target.value)}
                                />
                            </div>
                        </li>
                    ))}
                </ul>
            </div>

            <div className="mb-3">
                <label className="form-label fw-semibold">환불 사유 <span className="text-danger">*</span></label>
                <textarea
                    className="form-control"
                    rows={3}
                    placeholder="환불 사유를 입력해주세요."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                />
            </div>

            {errorMsg && <div className="alert alert-danger py-2">{errorMsg}</div>}

            <div className="d-flex gap-2 justify-content-end">
                <button
                    className="btn btn-outline-secondary"
                    onClick={() => navigate(-1)}
                    disabled={loading}
                >
                    취소
                </button>
                <button
                    className="btn btn-danger"
                    onClick={handleSubmit}
                    disabled={loading}
                >
                    {loading ? (
                        <><span className="spinner-border spinner-border-sm me-2" />처리 중...</>
                    ) : (
                        "환불 신청"
                    )}
                </button>
            </div>
        </div>
    );
}