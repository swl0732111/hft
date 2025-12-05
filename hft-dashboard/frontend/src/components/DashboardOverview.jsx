import React from 'react';

export default function DashboardOverview({ stats }) {
    if (!stats) return <div>Loading...</div>;

    const StatCard = ({ title, value, change, subtitle }) => (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</h3>
            <div className="mt-2 flex items-baseline">
                <p className="text-3xl font-semibold text-gray-900 dark:text-white">
                    {value}
                </p>
                {change && (
                    <span className={`ml-2 text-sm font-medium ${change >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {change >= 0 ? '↑' : '↓'} {Math.abs(change)}%
                    </span>
                )}
            </div>
            {subtitle && (
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>
            )}
        </div>
    );

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(value);
    };

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Trading Overview</h2>

            {/* 24h Stats */}
            <div>
                <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300 mb-3">24 Hours</h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard
                        title="Volume"
                        value={formatCurrency(stats.stats24h.volume)}
                    />
                    <StatCard
                        title="Trades"
                        value={stats.stats24h.tradeCount}
                    />
                    <StatCard
                        title="Avg Trade Size"
                        value={formatCurrency(stats.stats24h.avgTradeSize)}
                    />
                    <StatCard
                        title="Fees Paid"
                        value={formatCurrency(stats.stats24h.feesPaid)}
                        subtitle={`Saved: ${formatCurrency(stats.stats24h.feeSavings)}`}
                    />
                </div>
            </div>

            {/* 7d Stats */}
            <div>
                <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300 mb-3">7 Days</h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard
                        title="Volume"
                        value={formatCurrency(stats.stats7d.volume)}
                    />
                    <StatCard
                        title="Trades"
                        value={stats.stats7d.tradeCount}
                    />
                    <StatCard
                        title="Avg Trade Size"
                        value={formatCurrency(stats.stats7d.avgTradeSize)}
                    />
                    <StatCard
                        title="Fees Paid"
                        value={formatCurrency(stats.stats7d.feesPaid)}
                        subtitle={`Saved: ${formatCurrency(stats.stats7d.feeSavings)}`}
                    />
                </div>
            </div>

            {/* 30d Stats */}
            <div>
                <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300 mb-3">30 Days</h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard
                        title="Volume"
                        value={formatCurrency(stats.stats30d.volume)}
                    />
                    <StatCard
                        title="Trades"
                        value={stats.stats30d.tradeCount}
                    />
                    <StatCard
                        title="Avg Trade Size"
                        value={formatCurrency(stats.stats30d.avgTradeSize)}
                    />
                    <StatCard
                        title="Fees Paid"
                        value={formatCurrency(stats.stats30d.feesPaid)}
                        subtitle={`Saved: ${formatCurrency(stats.stats30d.feeSavings)}`}
                    />
                </div>
            </div>
        </div>
    );
}
