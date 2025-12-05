import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';

export default function VolumeChart({ chartData }) {
    if (!chartData) return <div>Loading...</div>;

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(value);
    };

    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return `${date.getMonth() + 1}/${date.getDate()}`;
    };

    return (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
                30-Day Trading Volume
            </h2>

            <ResponsiveContainer width="100%" height={400}>
                <BarChart data={chartData.data}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                    <XAxis
                        dataKey="date"
                        tickFormatter={formatDate}
                        stroke="#9CA3AF"
                    />
                    <YAxis
                        tickFormatter={(value) => `$${(value / 1000).toFixed(0)}K`}
                        stroke="#9CA3AF"
                    />
                    <Tooltip
                        formatter={(value) => formatCurrency(value)}
                        labelFormatter={(label) => `Date: ${label}`}
                        contentStyle={{
                            backgroundColor: '#1F2937',
                            border: '1px solid #374151',
                            borderRadius: '8px'
                        }}
                    />
                    <Legend />
                    <Bar dataKey="volume" fill="#8B5CF6" name="Daily Volume" />

                    {/* Tier threshold lines */}
                    {chartData.tierThresholds.map((threshold, index) => (
                        <ReferenceLine
                            key={index}
                            y={threshold.volume}
                            label={threshold.tier}
                            stroke="#10B981"
                            strokeDasharray="3 3"
                        />
                    ))}
                </BarChart>
            </ResponsiveContainer>

            {/* Tier Thresholds Legend */}
            <div className="mt-4 flex flex-wrap gap-4">
                {chartData.tierThresholds.map((threshold, index) => (
                    <div key={index} className="flex items-center text-sm">
                        <div className="w-4 h-0.5 bg-green-500 mr-2" style={{ borderTop: '2px dashed' }} />
                        <span className="text-gray-600 dark:text-gray-400">
                            {threshold.tier}: {formatCurrency(threshold.volume)}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}
