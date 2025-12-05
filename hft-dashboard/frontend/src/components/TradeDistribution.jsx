import React from 'react';
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = ['#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#6366f1'];

function TradeDistribution({ data }) {
    if (!data) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    Trade Distribution
                </h2>
                <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600"></div>
                </div>
            </div>
        );
    }

    const { hourlyDistribution, pairDistribution, buySellRatio } = data;

    // Prepare buy/sell data for chart
    const buySellData = [
        { name: 'Buy', value: buySellRatio.buyVolume, count: buySellRatio.buyCount, percentage: buySellRatio.buyPercentage },
        { name: 'Sell', value: buySellRatio.sellVolume, count: buySellRatio.sellCount, percentage: buySellRatio.sellPercentage }
    ];

    return (
        <div className="space-y-6">
            {/* Hourly Distribution */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    📊 24-Hour Trading Activity
                </h2>
                <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={hourlyDistribution}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                        <XAxis
                            dataKey="hour"
                            stroke="#9ca3af"
                            label={{ value: 'Hour of Day', position: 'insideBottom', offset: -5 }}
                        />
                        <YAxis stroke="#9ca3af" />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            labelStyle={{ color: '#f3f4f6' }}
                            formatter={(value, name) => {
                                if (name === 'volume') return ['$' + value.toLocaleString(), 'Volume'];
                                if (name === 'tradeCount') return [value, 'Trades'];
                                return [value, name];
                            }}
                        />
                        <Legend />
                        <Bar dataKey="volume" fill="#8b5cf6" name="Volume ($)" />
                        <Bar dataKey="tradeCount" fill="#ec4899" name="Trade Count" />
                    </BarChart>
                </ResponsiveContainer>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 text-center">
                    Peak trading hours: 9 AM - 4 PM
                </p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Trading Pair Distribution */}
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                        💱 Trading Pairs
                    </h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={pairDistribution}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ symbol, percentage }) => `${symbol}: ${percentage.toFixed(1)}%`}
                                outerRadius={80}
                                fill="#8884d8"
                                dataKey="volume"
                            >
                                {pairDistribution.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                            <Tooltip
                                formatter={(value) => '$' + value.toLocaleString()}
                                contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                    <div className="mt-4 space-y-2">
                        {pairDistribution.map((pair, index) => (
                            <div key={pair.symbol} className="flex justify-between items-center text-sm">
                                <div className="flex items-center">
                                    <div
                                        className="w-3 h-3 rounded-full mr-2"
                                        style={{ backgroundColor: COLORS[index % COLORS.length] }}
                                    ></div>
                                    <span className="text-gray-700 dark:text-gray-300">{pair.symbol}</span>
                                </div>
                                <span className="text-gray-500 dark:text-gray-400">
                                    ${pair.volume.toLocaleString()} ({pair.percentage.toFixed(1)}%)
                                </span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Buy/Sell Ratio */}
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                        ⚖️ Buy/Sell Ratio
                    </h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={buySellData}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ name, percentage }) => `${name}: ${percentage.toFixed(1)}%`}
                                outerRadius={80}
                                fill="#8884d8"
                                dataKey="value"
                            >
                                <Cell fill="#10b981" />
                                <Cell fill="#ef4444" />
                            </Pie>
                            <Tooltip
                                formatter={(value) => '$' + value.toLocaleString()}
                                contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                    <div className="mt-4 space-y-3">
                        <div className="flex justify-between items-center p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
                            <div>
                                <div className="text-sm font-medium text-green-700 dark:text-green-300">Buy Orders</div>
                                <div className="text-xs text-green-600 dark:text-green-400">{buySellRatio.buyCount} trades</div>
                            </div>
                            <div className="text-right">
                                <div className="text-lg font-bold text-green-700 dark:text-green-300">
                                    ${buySellRatio.buyVolume.toLocaleString()}
                                </div>
                                <div className="text-xs text-green-600 dark:text-green-400">
                                    {buySellRatio.buyPercentage.toFixed(1)}%
                                </div>
                            </div>
                        </div>
                        <div className="flex justify-between items-center p-3 bg-red-50 dark:bg-red-900/20 rounded-lg">
                            <div>
                                <div className="text-sm font-medium text-red-700 dark:text-red-300">Sell Orders</div>
                                <div className="text-xs text-red-600 dark:text-red-400">{buySellRatio.sellCount} trades</div>
                            </div>
                            <div className="text-right">
                                <div className="text-lg font-bold text-red-700 dark:text-red-300">
                                    ${buySellRatio.sellVolume.toLocaleString()}
                                </div>
                                <div className="text-xs text-red-600 dark:text-red-400">
                                    {buySellRatio.sellPercentage.toFixed(1)}%
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default TradeDistribution;
