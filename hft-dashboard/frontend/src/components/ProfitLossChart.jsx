import React from 'react';
import { LineChart, Line, AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

function ProfitLossChart({ data }) {
    if (!data) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    Profit & Loss Analysis
                </h2>
                <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600"></div>
                </div>
            </div>
        );
    }

    const { dailyPnL, cumulativePnL, feeAnalysis } = data;

    // Calculate cumulative P&L for area chart
    let cumulative = 0;
    const cumulativeData = dailyPnL.map(day => {
        cumulative += day.netPnL;
        return {
            ...day,
            cumulative: cumulative
        };
    });

    return (
        <div className="space-y-6">
            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Net Profit</div>
                    <div className="text-2xl font-bold mt-1">
                        ${cumulativePnL.netProfit.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </div>
                    <div className="text-xs opacity-75 mt-1">
                        ROI: {cumulativePnL.roi.toFixed(2)}%
                    </div>
                </div>

                <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Total Realized</div>
                    <div className="text-2xl font-bold mt-1">
                        ${cumulativePnL.totalRealized.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </div>
                    <div className="text-xs opacity-75 mt-1">
                        Unrealized: ${cumulativePnL.totalUnrealized.toFixed(2)}
                    </div>
                </div>

                <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Win Rate</div>
                    <div className="text-2xl font-bold mt-1">
                        {((cumulativePnL.profitableDays / (cumulativePnL.profitableDays + cumulativePnL.losingDays)) * 100).toFixed(1)}%
                    </div>
                    <div className="text-xs opacity-75 mt-1">
                        {cumulativePnL.profitableDays}W / {cumulativePnL.losingDays}L
                    </div>
                </div>

                <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Fee Savings</div>
                    <div className="text-2xl font-bold mt-1">
                        ${feeAnalysis.totalFeeSavings.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </div>
                    <div className="text-xs opacity-75 mt-1">
                        Paid: ${feeAnalysis.totalFeesPaid.toFixed(2)}
                    </div>
                </div>
            </div>

            {/* Daily P&L Chart */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    📈 Daily Profit & Loss
                </h2>
                <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={dailyPnL}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                        <XAxis
                            dataKey="date"
                            stroke="#9ca3af"
                            tick={{ fontSize: 12 }}
                            tickFormatter={(value) => new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                        />
                        <YAxis stroke="#9ca3af" />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            labelFormatter={(value) => new Date(value).toLocaleDateString()}
                            formatter={(value, name) => {
                                const labels = {
                                    realizedPnL: 'Realized P&L',
                                    unrealizedPnL: 'Unrealized P&L',
                                    netPnL: 'Net P&L'
                                };
                                return ['$' + value.toFixed(2), labels[name] || name];
                            }}
                        />
                        <Legend />
                        <Line type="monotone" dataKey="realizedPnL" stroke="#10b981" strokeWidth={2} dot={false} name="Realized" />
                        <Line type="monotone" dataKey="netPnL" stroke="#8b5cf6" strokeWidth={2} dot={false} name="Net P&L" />
                    </LineChart>
                </ResponsiveContainer>
            </div>

            {/* Cumulative P&L */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    📊 Cumulative Profit
                </h2>
                <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={cumulativeData}>
                        <defs>
                            <linearGradient id="colorCumulative" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.8} />
                                <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                        <XAxis
                            dataKey="date"
                            stroke="#9ca3af"
                            tick={{ fontSize: 12 }}
                            tickFormatter={(value) => new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                        />
                        <YAxis stroke="#9ca3af" />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            labelFormatter={(value) => new Date(value).toLocaleDateString()}
                            formatter={(value) => '$' + value.toFixed(2)}
                        />
                        <Area
                            type="monotone"
                            dataKey="cumulative"
                            stroke="#8b5cf6"
                            strokeWidth={2}
                            fillOpacity={1}
                            fill="url(#colorCumulative)"
                            name="Cumulative P&L"
                        />
                    </AreaChart>
                </ResponsiveContainer>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 text-center">
                    Total cumulative profit: ${cumulative.toFixed(2)}
                </p>
            </div>

            {/* Fee Analysis */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    💰 Fee Analysis
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                        <div className="text-sm text-gray-600 dark:text-gray-400">Total Fees Paid</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            ${feeAnalysis.totalFeesPaid.toFixed(2)}
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                        <div className="text-sm text-gray-600 dark:text-gray-400">Avg Fee/Trade</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            ${feeAnalysis.avgFeePerTrade.toFixed(2)}
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                        <div className="text-sm text-gray-600 dark:text-gray-400">Fee % of Volume</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            {feeAnalysis.feeAsPercentOfVolume.toFixed(3)}%
                        </div>
                    </div>
                </div>
                <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={feeAnalysis.dailyFees.slice(-14)}> {/* Last 14 days */}
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                        <XAxis
                            dataKey="date"
                            stroke="#9ca3af"
                            tick={{ fontSize: 12 }}
                            tickFormatter={(value) => new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                        />
                        <YAxis stroke="#9ca3af" />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            formatter={(value, name) => {
                                const labels = { feesPaid: 'Fees Paid', feeSavings: 'Savings' };
                                return ['$' + value.toFixed(2), labels[name] || name];
                            }}
                        />
                        <Legend />
                        <Bar dataKey="feesPaid" fill="#ef4444" name="Fees Paid" />
                        <Bar dataKey="feeSavings" fill="#10b981" name="Savings" />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}

export default ProfitLossChart;
