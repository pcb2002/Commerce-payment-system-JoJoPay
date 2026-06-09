import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getOrderDetail, cancelOrder } from '../../api/orderApi';

const STATUS_LABEL = {
    PENDING_PAYMENT: { label: '결제 대기', cls: 'text-bg-warning' },
    COMPLETED:       { label: '결제 완료', cls: 'text-bg-success' },
    CANCELLED:       { label: '취소됨',   cls: 'text-bg-secondary' },
    FAILED:          { label: '결제 실패', cls: 'text-bg-danger' },
};

const PAYMENT_STATUS_LABEL = {
    READY:  '결제 준비',
    PAID:   '결제 완료',
    FAILED: '결제 실패',
    CANCELLED: '결제 취소',
};

export default function OrderDetailPage() {
    const { orderNumber } = useParams();
    const navigate = useNavigate();

    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [cancelling, setCancelling] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetch = async () => {
            try {
                const data = await getOrderDetail(orderNumber);
                setOrder(data.data);
            } catch (err) {
                setError('주문 상세를 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };
        fetch();
    }, [orderNumber]);

    const handleCancel = async () => {
        if (!window.confirm('해당 주문을 취소하시겠습니까?')) return;
        setCancelling(true);
        try {
            await cancelOrder(orderNumber);
            const data = await getOrderDetail(orderNumber);
            setOrder(data.data);
        } catch (err) {
            alert(err.response?.data?.message || '주문 취소에 실패했습니다.');
        } finally {
            setCancelling(false);
        }
    };

    const badge = (status) => {
        const s = STATUS_LABEL[status] || { label: status, cls: 'text-bg-light' };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => navigate('/orders')}>← 목록으로</button>
        </div>
    );

    return (
        <div className="container py-4" style={{ maxWidth: '640px' }}>
            <button className="btn btn-outline-secondary btn-sm mb-3" onClick={() => navigate('/orders')}>← 주문 내역</button>

            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="h5 fw-semibold mb-0">주문 상세</h2>
                {badge(order.status)}
            </div>

            {/* 주문 기본 정보 */}
            <div className="card mb-3">
                <div className="card-header bg-white fw-medium">주문 정보</div>
                <div className="card-body">
                    <table className="table table-borderless mb-0 small">
                        <tbody>
                        <tr>
                            <td className="text-muted ps-0" style={{ width: '120px' }}>주문 번호</td>
                            <td className="fw-medium">{order.orderNumber}</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">주문 일시</td>
                            <td>{new Date(order.createdAt).toLocaleString('ko-KR')}</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">총 금액</td>
                            <td className="fw-semibold">{order.totalAmount.toLocaleString()}원</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 주문 상품 목록 */}
            <div className="card mb-3">
                <div className="card-header bg-white fw-medium">주문 상품</div>
                <ul className="list-group list-group-flush">
                    {order.orderItems.map((item, idx) => (
                        <li key={idx} className="list-group-item">
                            <div className="d-flex justify-content-between align-items-center">
                                <div>
                                    <p className="fw-medium mb-0">{item.productName}</p>
                                    <small className="text-muted">
                                        {item.priceAtOrder.toLocaleString()}원 × {item.quantity}
                                        {item.status && <span className="ms-2 badge text-bg-light text-dark border">{item.status}</span>}
                                    </small>
                                </div>
                                <span className="fw-medium">{(item.priceAtOrder * item.quantity).toLocaleString()}원</span>
                            </div>
                        </li>
                    ))}
                </ul>
            </div>

            {/* 결제 정보 */}
            {order.payment && (
                <div className="card mb-4">
                    <div className="card-header bg-white fw-medium">결제 정보</div>
                    <div className="card-body">
                        <table className="table table-borderless mb-0 small">
                            <tbody>
                            <tr>
                                <td className="text-muted ps-0" style={{ width: '140px' }}>결제 상태</td>
                                <td>{PAYMENT_STATUS_LABEL[order.payment.paymentStatus] || order.payment.paymentStatus}</td>
                            </tr>
                            <tr>
                                <td className="text-muted ps-0">포인트 사용</td>
                                <td>{order.payment.usedPoint.toLocaleString()}P</td>
                            </tr>
                            <tr>
                                <td className="text-muted ps-0">카드 결제 금액</td>
                                <td className="fw-semibold">{order.payment.pgRealAmount.toLocaleString()}원</td>
                            </tr>
                            {order.payment.portonePaymentId && (
                                <tr>
                                    <td className="text-muted ps-0">결제 ID</td>
                                    <td className="text-muted small">{order.payment.portonePaymentId}</td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* 주문 취소 버튼 - PENDING_PAYMENT 상태일 때만 표시 */}
            {order.status === 'PENDING_PAYMENT' && (
                <button
                    className="btn btn-outline-danger w-100"
                    onClick={handleCancel}
                    disabled={cancelling}
                >
                    {cancelling ? <span className="spinner-border spinner-border-sm me-2" /> : null}
                    주문 취소
                </button>
            )}
        </div>
    );
}