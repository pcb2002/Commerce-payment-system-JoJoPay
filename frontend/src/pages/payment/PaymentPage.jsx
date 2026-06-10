import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { confirmPayment } from "../../api/paymentApi.js";

export default function PaymentPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState("processing"); // processing | success | error
    const [paymentData, setPaymentData] = useState(null);
    const [errorMsg, setErrorMsg] = useState("");

    useEffect(() => {
        const orderNumber = searchParams.get("orderNumber");
        const portonePaymentId = searchParams.get("portonePaymentId");

        if (!orderNumber || !portonePaymentId) {
            setStatus("error");
            setErrorMsg("결제 정보가 올바르지 않습니다.");
            return;
        }

        confirmPayment(orderNumber, portonePaymentId)
            .then((res) => {
                setPaymentData(res.data.data);
                setStatus("success");
            })
            .catch((err) => {
                setErrorMsg(err.response?.data?.message || "결제 승인 중 오류가 발생했습니다.");
                setStatus("error");
            });
    }, []);

    if (status === "processing") {
        return (
            <div className="container mt-5 text-center">
                <div className="spinner-border text-primary mb-3" role="status" />
                <p>결제를 처리하는 중입니다...</p>
            </div>
        );
    }

    if (status === "error") {
        return (
            <div className="container mt-5" style={{ maxWidth: 480 }}>
                <div className="card border-danger">
                    <div className="card-body text-center">
                        <h4 className="text-danger mb-3">결제 실패</h4>
                        <p className="text-muted">{errorMsg}</p>
                        <button className="btn btn-outline-secondary mt-2" onClick={() => navigate("/orders")}>
                            주문 목록으로
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="container mt-5" style={{ maxWidth: 480 }}>
            <div className="card border-success">
                <div className="card-body text-center">
                    <h4 className="text-success mb-3">결제 완료</h4>
                    {paymentData && (
                        <table className="table table-borderless mb-3">
                            <tbody>
                            <tr>
                                <th className="text-start text-muted">결제 ID</th>
                                <td className="text-end">{paymentData.paymentId}</td>
                            </tr>
                            <tr>
                                <th className="text-start text-muted">상태</th>
                                <td className="text-end">{paymentData.status}</td>
                            </tr>
                            <tr>
                                <th className="text-start text-muted">결제 금액</th>
                                <td className="text-end">{paymentData.amount?.toLocaleString()}원</td>
                            </tr>
                            <tr>
                                <th className="text-start text-muted">승인 시각</th>
                                <td className="text-end">
                                    {paymentData.approvedAt
                                        ? new Date(paymentData.approvedAt).toLocaleString("ko-KR")
                                        : "-"}
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    )}
                    <div className="d-flex gap-2 justify-content-center">
                        <button className="btn btn-primary" onClick={() => navigate("/orders")}>
                            주문 목록
                        </button>
                        <button className="btn btn-outline-secondary" onClick={() => navigate("/products")}>
                            쇼핑 계속하기
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}