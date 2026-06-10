// src/pages/my/MyRefundListPage.jsx

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyRefunds } from '../../api/refundApi';

const STATUS_LABEL = {
    READY:     { label: '환불 준비', cls: 'text-bg-warning' },
    COMPLETED: { label: '환불 완료', cls: 'text-bg-success' },
    FAILED:    { label: '환불 실패', cls: 'text-bg-danger' },
};

export default function MyRefundListPage() {
    const navigate = useNavigate();
    const [refunds, setRefunds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        getMyRefunds()
            .then((res) => setRefunds(res.data ?? []))
            .catch(() => setError('환불 내역을 불러오는 데 실패했습니다.'))
            .finally(() => setLoading(false));
    }, []);

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
        </div>
    );

    return (
        <div className="container py-4" style={{ maxWidth: '640px' }}>
            <button className="btn btn-outline-secondary btn-sm mb-3" onClick={() => navigate(-1)}>← 뒤로가기</button>
            <h2 className="h5 fw-semibold mb-4">내 환불 내역</h2>

            {refunds.length === 0 ? (
                <div className="text-center py-5">
                    <p className="text-muted mb-1">환불 내역이 없습니다.</p>
                    <button className="btn btn-outline-primary btn-sm mt-2" onClick={() => navigate('/orders')}>
                        주문 내역 보기
                    </button>
                </div>
            ) : (
                <div className="d-flex flex-column gap-3">
                    {refunds.map((refund) => {
                        const s = STATUS_LABEL[refund.status] || { label: refund.status, cls: 'text-bg-light' };
                        return (
                            <div className="card" key={refund.refundId}>
                                <div className="card-body">
                                    <div className="d-flex justify-content-between align-items-center mb-2">
                                        <span className="small text-muted">주문번호 {refund.orderNumber}</span>
                                        <span className={`badge ${s.cls}`}>{s.label}</span>
                                    </div>
                                    <p className="mb-1 small"><span className="text-muted">사유</span> {refund.reason}</p>
                                    <hr className="my-2" />
                                    <div className="d-flex justify-content-between small text-muted">
                                        <span>포인트 환불</span>
                                        <span>{refund.pointRefundAmount?.toLocaleString()}P</span>
                                    </div>
                                    <div className="d-flex justify-content-between small text-muted">
                                        <span>카드 환불</span>
                                        <span>{refund.pgRefundAmount?.toLocaleString()}원</span>
                                    </div>
                                    <div className="d-flex justify-content-between fw-semibold mt-1">
                                        <span>총 환불 금액</span>
                                        <span>{refund.totalRefundAmount?.toLocaleString()}원</span>
                                    </div>
                                    <p className="text-muted small mt-2 mb-0">
                                        {new Date(refund.createdAt).toLocaleString('ko-KR')}
                                    </p>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}