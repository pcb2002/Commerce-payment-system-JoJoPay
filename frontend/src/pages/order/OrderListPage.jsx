import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyOrders } from '../../api/orderApi';

const STATUS_LABEL = {
    PENDING_PAYMENT: { label: '결제 대기', cls: 'text-bg-warning' },
    COMPLETED:       { label: '결제 완료', cls: 'text-bg-success' },
    CANCELLED:       { label: '취소됨',   cls: 'text-bg-secondary' },
    FAILED:          { label: '결제 실패', cls: 'text-bg-danger' },
};

export default function OrderListPage() {
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalPages: 0, number: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);

    useEffect(() => {
        const fetch = async () => {
            setLoading(true);
            try {
                const data = await getMyOrders({ page, size: 10 });
                setOrders(data.data.content);
                setPageInfo({ totalPages: data.data.totalPages, number: data.data.number });
            } catch (err) {
                setError('주문 내역을 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };
        fetch();
    }, [page]);

    const badge = (status) => {
        const s = STATUS_LABEL[status] || { label: status, cls: 'text-bg-light' };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    return (
        <div className="container py-4" style={{ maxWidth: '720px' }}>
            <h2 className="h5 fw-semibold mb-4">주문 내역</h2>

            {error && <div className="alert alert-danger">{error}</div>}

            {loading ? (
                <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status" />
                </div>
            ) : orders.length === 0 ? (
                <div className="card">
                    <div className="card-body text-center py-5 text-muted">
                        <p className="mb-3">주문 내역이 없습니다.</p>
                        <button className="btn btn-primary btn-sm" onClick={() => navigate('/products')}>상품 보러가기</button>
                    </div>
                </div>
            ) : (
                <>
                    <div className="card mb-3">
                        <ul className="list-group list-group-flush">
                            {orders.map((order) => (
                                <li
                                    key={order.orderNumber}
                                    className="list-group-item list-group-item-action py-3"
                                    style={{ cursor: 'pointer' }}
                                    onClick={() => navigate(`/orders/${order.orderNumber}`)}
                                >
                                    <div className="d-flex justify-content-between align-items-start">
                                        <div>
                                            <p className="fw-medium mb-1">{order.orderNumber}</p>
                                            <small className="text-muted">
                                                {new Date(order.createdAt).toLocaleString('ko-KR')}
                                            </small>
                                        </div>
                                        <div className="text-end">
                                            <p className="fw-semibold mb-1">{order.totalAmount.toLocaleString()}원</p>
                                            {badge(order.status)}
                                        </div>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>

                    {pageInfo.totalPages > 1 && (
                        <nav className="d-flex justify-content-center">
                            <ul className="pagination pagination-sm mb-0">
                                <li className={`page-item ${pageInfo.number === 0 ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => setPage(pageInfo.number - 1)}>이전</button>
                                </li>
                                {Array.from({ length: pageInfo.totalPages }, (_, i) => (
                                    <li key={i} className={`page-item ${pageInfo.number === i ? 'active' : ''}`}>
                                        <button className="page-link" onClick={() => setPage(i)}>{i + 1}</button>
                                    </li>
                                ))}
                                <li className={`page-item ${pageInfo.number === pageInfo.totalPages - 1 ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => setPage(pageInfo.number + 1)}>다음</button>
                                </li>
                            </ul>
                        </nav>
                    )}
                </>
            )}
        </div>
    );
}