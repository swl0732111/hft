import React, { useState, useEffect } from 'react';
import './AccountDashboard.css';

/**
 * Real-time account state dashboard
 * Fetches data from RocksDB-backed API for ultra-low latency
 */
export default function AccountDashboard({ accountId }) {
    const [accountState, setAccountState] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // 初始加载
        loadAccountState();

        // 每5秒刷新一次（实际可以用 WebSocket 推送）
        const interval = setInterval(loadAccountState, 5000);

        return () => clearInterval(interval);
    }, [accountId]);

    const loadAccountState = async () => {
        try {
            const response = await fetch(`/api/account/${accountId}/state`);
            if (response.ok) {
                const data = await response.json();
                setAccountState(data);
                setLoading(false);
            }
        } catch (error) {
            console.error('Failed to load account state:', error);
        }
    };

    if (loading || !accountState) {
        return <div className="loading">Loading account state...</div>;
    }

    const { balances, positions, riskMetrics } = accountState;

    return (
        <div className="account-dashboard">
            {/* 风险指标卡片 */}
            <div className="risk-card">
                <h3>保证金状态</h3>
                <div className="risk-metrics">
                    <div className="metric">
                        <span className="label">保证金率</span>
                        <span className={`value ${getMarginLevelClass(riskMetrics.marginLevel)}`}>
                            {riskMetrics.marginLevel?.toFixed(2)}%
                        </span>
                    </div>
                    <div className="metric">
                        <span className="label">总权益</span>
                        <span className="value">{riskMetrics.totalEquity?.toFixed(2)} USDC</span>
                    </div>
                    <div className="metric">
                        <span className="label">可用保证金</span>
                        <span className="value">{riskMetrics.availableMargin?.toFixed(2)} USDC</span>
                    </div>
                    <div className="metric">
                        <span className="label">当前杠杆</span>
                        <span className="value">{riskMetrics.leverage?.toFixed(1)}x</span>
                    </div>
                </div>
            </div>

            {/* 资产余额 */}
            <div className="balances-card">
                <h3>资产余额</h3>
                <table className="balances-table">
                    <thead>
                        <tr>
                            <th>币种</th>
                            <th>可用</th>
                            <th>冻结</th>
                            <th>总计</th>
                        </tr>
                    </thead>
                    <tbody>
                        {Object.values(balances).map(balance => (
                            <tr key={balance.asset}>
                                <td>{balance.asset}</td>
                                <td>{balance.availableBalance?.toFixed(4)}</td>
                                <td>{balance.lockedBalance?.toFixed(4)}</td>
                                <td><strong>{balance.totalBalance?.toFixed(4)}</strong></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* 持仓信息 */}
            {Object.keys(positions).length > 0 && (
                <div className="positions-card">
                    <h3>当前持仓</h3>
                    <table className="positions-table">
                        <thead>
                            <tr>
                                <th>交易对</th>
                                <th>持仓量</th>
                                <th>开仓均价</th>
                                <th>未实现盈亏</th>
                                <th>已实现盈亏</th>
                                <th>持仓时长</th>
                            </tr>
                        </thead>
                        <tbody>
                            {Object.values(positions).map(pos => (
                                <tr key={pos.symbol}>
                                    <td><strong>{pos.symbol}</strong></td>
                                    <td>{pos.quantity?.toFixed(4)}</td>
                                    <td>${pos.avgEntryPrice?.toFixed(2)}</td>
                                    <td className={getPnLClass(pos.unrealizedPnL)}>
                                        ${pos.unrealizedPnL?.toFixed(2)}
                                    </td>
                                    <td className={getPnLClass(pos.realizedPnL)}>
                                        ${pos.realizedPnL?.toFixed(2)}
                                    </td>
                                    <td>{formatDuration(Date.now() - pos.openTime)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

// Helper functions
function getMarginLevelClass(level) {
    if (!level) return '';
    if (level < 110) return 'danger';
    if (level < 130) return 'warning';
    return 'safe';
}

function getPnLClass(value) {
    if (!value) return '';
    return value >= 0 ? 'profit' : 'loss';
}

function formatDuration(ms) {
    const hours = Math.floor(ms / 3600000);
    if (hours < 24) return `${hours}h`;
    const days = Math.floor(hours / 24);
    return `${days}d`;
}
