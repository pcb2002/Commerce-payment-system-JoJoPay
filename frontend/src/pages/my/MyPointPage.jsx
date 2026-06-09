// src/pages/my/MyPointPage.jsx

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPointBalance, getPointHistories } from '../../api/pointApi';

const TYPE_LABEL = {
    EARN:           { label: '적립', cls: 'text-success' },
    USE:            { label: '사용', cls: 'text-danger' },
    USE_RECOVERY:   { label: '사용 복구', cls: 'text-primary' },
    EARN_FORFEIT:   { label: '적립 취소', cls: 'text-warning' },
};

export default function MyPointPage() {
    const navigate = useNavigate();
    const [balance, setBalance] = useState(null);
    const [histories, setHistories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        Promise.all([getPointBalance(), getPointHistories()])
            .then(([balanceRes, historiesRes]) => {
                setBalance(balanceRes.data.data.pointBalance);
                setHistories(historiesRes.data.data ?? []);
            })
            .catch(() => setError('포인트 정보를 불러오는 데 실패했습니다.'))
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
            <h2 className="h5 fw-semibold mb-4">내 포인트</h2>

            <div className="card mb-4 text-center py-4">
                <p className="text-muted small mb-1">현재 포인트 잔액</p>
                <p className="fw-semibold mb-0" style={{ fontSize: 32 }}>
                    {balance?.toLocaleString()}<span className="fs-6 text-muted ms-1">P</span>
                </p>
            </div>

            <h6 className="fw-medium mb-3 text-muted">포인트 이력</h6>

            {histories.length === 0 ? (
                <div className="text-center py-5">
                    <p className="text-muted">포인트 이력이 없습니다.</p>
                </div>
            ) : (
                <div className="d-flex flex-column gap-2">
                    {histories.map((h) => {
                        const t = TYPE_LABEL[h.transactionType] || { label: h.transactionType, cls: 'text-secondary' };
                        const isEarn = h.transactionType === 'EARN' || h.transactionType === 'USE_RECOVERY';
                        return (
                            <div key={h.id} className="card">
                                <div className="card-body py-2 px-3 d-flex justify-content-between align-items-center">
                                    <div>
                                        <span className={`small fw-medium ${t.cls}`}>{h.transactionTypeDescription || t.label}</span>
                                        <p className="text-muted small mb-0">{new Date(h.createdAt).toLocaleString('ko-KR')}</p>
                                    </div>
                                    <span className={`fw-semibold ${isEarn ? 'text-success' : 'text-danger'}`}>
                                        {isEarn ? '+' : '-'}{h.amount?.toLocaleString()}P
                                    </span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}