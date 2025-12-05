import React from 'react';
import { PieChart, Pie, Cell, BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = ['#10b981', '#ef4444'];

function PerformanceMetrics({ data }) {
    if (!data) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    Performance Metrics
                </h2>
                <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600"></div>
                </div>
            </div>
        );
    }

    const { winRate, holdingTimeDistribution, drawdown, riskMetrics } = data;

    // Prepare win/loss data for pie chart
    const winLossData = [
        { name: 'Winning Trades', value: winRate.profitableTrades },
        { name: 'Losing Trades', value: winRate.losingTrades }
    ];

    return (
        <div className="space-y-6">
            {/* Key Metrics Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Win Rate</div>
                    <div className="text-3xl font-bold mt-1">{winRate.winRate.toFixed(1)}%</div>
                    <div className="text-xs opacity-75 mt-1">
                        {winRate.profitableTrades}/{winRate.totalTrades} trades
                    </div>
                </div>

                <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Profit Factor</div>
                    <div className="text-3xl font-bold mt-1">{winRate.profitFactor.toFixed(2)}</div>
                    <div className="text-xs opacity-75 mt-1">
                        Avg Win: ${winRate.avgWin.toFixed(0)}
                    </div>
                </div>

                <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Sharpe Ratio</div>
                    <div className="text-3xl font-bold mt-1">{riskMetrics.sharpeRatio.toFixed(2)}</div>
                    <div className="text-xs opacity-75 mt-1">
                        Volatility: {(riskMetrics.volatility * 100).toFixed(1)}%
                    </div>
                </div>

                <div className="bg-gradient-to-br from-red-500 to-red-600 rounded-lg shadow p-4 text-white">
                    <div className="text-sm opacity-90">Max Drawdown</div>
                    <div className="text-3xl font-bold mt-1">{drawdown.maxDrawdownPercent.toFixed(1)}%</div>
                    <div className="text-xs opacity-75 mt-1">
                        ${Math.abs(drawdown.maxDrawdown).toLocaleString()}
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Win Rate Pie Chart */}
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                        🎯 Win/Loss Distribution
                    </h2>
                    <ResponsiveContainer width="100%" height={250}>
                        <PieChart>
                            <Pie
                                data={winLossData}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ name, value, percent }) => `${name}: ${(percent * 100).toFixed(1)}%`}
                                outerRadius={80}
                                fill="#8884d8"
                                dataKey="value"
                            >
                                {winLossData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index]} />
                                ))}
                            </Pie>
                            <Tooltip
                                formatter={(value) => value + ' trades'}
                                contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                    <div className="mt-4 space-y-2">
                        <div className="flex justify-between p-2 bg-green-50 dark:bg-green-900/20 rounded">
                            <span className="text-sm text-green-700 dark:text-green-300">Avg Win</span>
                            <span className="text-sm font-bold text-green-700 dark:text-green-300">
                                ${winRate.avgWin.toFixed(2)}
                            </span>
                        </div>
                        <div className="flex justify-between p-2 bg-red-50 dark:bg-red-900/20 rounded">
                            <span className="text-sm text-red-700 dark:text-red-300">Avg Loss</span>
                            <span className="text-sm font-bold text-red-700 dark:text-red-300">
                                ${Math.abs(winRate.avgLoss).toFixed(2)}
                            </span>
                        </div>
                        <div className="flex justify-between p-2 bg-purple-50 dark:bg-purple-900/20 rounded">
                            <span className="text-sm text-purple-700 dark:text-purple-300">Profit Factor</span>
                            <span className="text-sm font-bold text-purple-700 dark:text-purple-300">
                                {winRate.profitFactor.toFixed(2)}x
                            </span>
                        </div>
                    </div>
                </div>

                {/* Holding Time Distribution */}
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                        ⏱️ Holding Time Distribution
                    </h2>
                    <ResponsiveContainer width="100%" height={250}>
                        <BarChart data={holdingTimeDistribution} layout="vertical">
                            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                            <XAxis type="number" stroke="#9ca3af" />
                            <YAxis dataKey="timeRange" type="category" stroke="#9ca3af" width={80} />
                            <Tooltip
                                contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                                formatter={(value, name) => {
                                    if (name === 'tradeCount') return [value, 'Trades'];
                                    if (name === 'percentage') return [value.toFixed(1) + '%', 'Percentage'];
                                    return [value, name];
                                }}
                            />
                            <Bar dataKey="tradeCount" fill="#8b5cf6" name="Trade Count" />
                        </BarChart>
                    </ResponsiveContainer>
                    <div className="mt-4 text-sm text-gray-600 dark:text-gray-400">
                        <p>Most trades held: <span className="font-bold text-gray-900 dark:text-white">1-5 minutes</span></p>
                        <p className="mt-1">Avg P&L increases with holding time</p>
                    </div>
                </div>
            </div>

            {/* Drawdown Analysis */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    📉 Drawdown Analysis
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                    <div className="p-4 bg-red-50 dark:bg-red-900/20 rounded-lg border border-red-200 dark:border-red-800">
                        <div className="text-sm text-red-600 dark:text-red-400">Max Drawdown</div>
                        <div className="text-2xl font-bold text-red-700 dark:text-red-300 mt-1">
                            {drawdown.maxDrawdownPercent.toFixed(2)}%
                        </div>
                        <div className="text-xs text-red-600 dark:text-red-400 mt-1">
                            ${Math.abs(drawdown.maxDrawdown).toLocaleString()}
                        </div>
                    </div>
                    <div className="p-4 bg-orange-50 dark:bg-orange-900/20 rounded-lg border border-orange-200 dark:border-orange-800">
                        <div className="text-sm text-orange-600 dark:text-orange-400">Current Drawdown</div>
                        <div className="text-2xl font-bold text-orange-700 dark:text-orange-300 mt-1">
                            ${Math.abs(drawdown.currentDrawdown).toLocaleString()}
                        </div>
                        <div className="text-xs text-orange-600 dark:text-orange-400 mt-1">
                            {drawdown.drawdownDays} days
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                        <div className="text-sm text-gray-600 dark:text-gray-400">Max DD Date</div>
                        <div className="text-lg font-bold text-gray-900 dark:text-white mt-1">
                            {new Date(drawdown.maxDrawdownDate).toLocaleDateString()}
                        </div>
                    </div>
                </div>

                {drawdown.drawdownPeriods && drawdown.drawdownPeriods.length > 0 && (
                    <div className="mt-4">
                        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                            Recent Drawdown Periods
                        </h3>
                        <div className="space-y-2">
                            {drawdown.drawdownPeriods.map((period, index) => (
                                <div key={index} className="flex justify-between items-center p-3 bg-gray-50 dark:bg-gray-700 rounded">
                                    <div>
                                        <div className="text-sm text-gray-900 dark:text-white">
                                            {new Date(period.startDate).toLocaleDateString()} - {new Date(period.endDate).toLocaleDateString()}
                                        </div>
                                        <div className="text-xs text-gray-500 dark:text-gray-400">
                                            Duration: {period.durationDays} days
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <div className="text-lg font-bold text-red-600 dark:text-red-400">
                                            {period.drawdownPercent.toFixed(1)}%
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* Risk Metrics */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    ⚠️ Risk Metrics
                </h2>
                <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg text-center">
                        <div className="text-xs text-gray-600 dark:text-gray-400">Sharpe Ratio</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            {riskMetrics.sharpeRatio.toFixed(2)}
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg text-center">
                        <div className="text-xs text-gray-600 dark:text-gray-400">Volatility</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            {(riskMetrics.volatility * 100).toFixed(1)}%
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg text-center">
                        <div className="text-xs text-gray-600 dark:text-gray-400">Max Losses</div>
                        <div className="text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
                            {riskMetrics.maxConsecutiveLosses}
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg text-center">
                        <div className="text-xs text-gray-600 dark:text-gray-400">Max Wins</div>
                        <div className="text-2xl font-bold text-green-600 dark:text-green-400 mt-1">
                            {riskMetrics.maxConsecutiveWins}
                        </div>
                    </div>
                    <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg text-center">
                        <div className="text-xs text-gray-600 dark:text-gray-400">Avg Risk/Trade</div>
                        <div className="text-2xl font-bold text-gray-900 dark:text-white mt-1">
                            {(riskMetrics.avgTradeRisk * 100).toFixed(1)}%
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default PerformanceMetrics;
