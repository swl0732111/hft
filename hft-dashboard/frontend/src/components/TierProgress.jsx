import React from 'react';

export default function TierProgress({ tierInfo }) {
    if (!tierInfo) return <div>Loading...</div>;

    const getTierBadgeColor = (tier) => {
        const colors = {
            VIP0: 'bg-gray-500',
            VIP1: 'bg-bronze-500',
            VIP2: 'bg-gray-400',
            VIP3: 'bg-yellow-500',
            VIP4: 'bg-purple-500',
            VIP5: 'bg-blue-500',
        };
        return colors[tier] || 'bg-gray-500';
    };

    const getTierIcon = (tier) => {
        const icons = {
            VIP0: '🥉',
            VIP1: '🥉',
            VIP2: '🥈',
            VIP3: '🥇',
            VIP4: '💎',
            VIP5: '👑',
        };
        return icons[tier] || '⭐';
    };

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(value);
    };

    return (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
                    Current Tier: {tierInfo.currentTier} {getTierIcon(tierInfo.currentTier)}
                </h2>
                <span className={`px-3 py-1 rounded-full text-white text-sm font-semibold ${getTierBadgeColor(tierInfo.currentTier)}`}>
                    Level {tierInfo.tierLevel}
                </span>
            </div>

            {/* Progress Bar */}
            {tierInfo.nextTier && (
                <div className="mb-6">
                    <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-2">
                        <span>Progress to {tierInfo.nextTier}</span>
                        <span>{tierInfo.progressPercent.toFixed(2)}%</span>
                    </div>
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-4">
                        <div
                            className="bg-gradient-to-r from-blue-500 to-purple-600 h-4 rounded-full transition-all duration-500"
                            style={{ width: `${Math.min(tierInfo.progressPercent, 100)}%` }}
                        />
                    </div>
                    <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
                        <span>{formatCurrency(tierInfo.volume30d)}</span>
                        <span>{formatCurrency(tierInfo.volume30d + tierInfo.volumeToNextTier)}</span>
                    </div>
                </div>
            )}

            {tierInfo.nextTier === null && (
                <div className="mb-6 text-center py-4">
                    <p className="text-lg font-semibold text-purple-600 dark:text-purple-400">
                        🎉 You've reached the maximum tier!
                    </p>
                </div>
            )}

            {/* Fee Rates */}
            <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Maker Fee</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                        {tierInfo.makerFeePercent}%
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{tierInfo.makerFeeBps} bps</p>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Taker Fee</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                        {tierInfo.takerFeePercent}%
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{tierInfo.takerFeeBps} bps</p>
                </div>
            </div>

            {/* Fee Savings */}
            <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-4 mb-6">
                <p className="text-sm text-green-700 dark:text-green-400">Total Fee Savings (vs VIP0)</p>
                <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                    {formatCurrency(tierInfo.feeSavings)}
                </p>
            </div>

            {/* Benefits */}
            <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">Tier Benefits</h3>
                <div className="space-y-2">
                    <div className="flex items-center text-sm">
                        <span className="text-gray-500 dark:text-gray-400 w-40">API Rate Limit:</span>
                        <span className="font-semibold text-gray-900 dark:text-white">{tierInfo.benefits.apiRateLimit}</span>
                    </div>
                    <div className="flex items-center text-sm">
                        <span className="text-gray-500 dark:text-gray-400 w-40">Support Priority:</span>
                        <span className="font-semibold text-gray-900 dark:text-white">{tierInfo.benefits.supportPriority}</span>
                    </div>
                    {tierInfo.benefits.priorityWithdrawal && (
                        <div className="flex items-center text-sm text-green-600 dark:text-green-400">
                            <span>✓ Priority Withdrawal</span>
                        </div>
                    )}
                    {tierInfo.benefits.dedicatedAccountManager && (
                        <div className="flex items-center text-sm text-purple-600 dark:text-purple-400">
                            <span>✓ Dedicated Account Manager</span>
                        </div>
                    )}
                    {tierInfo.benefits.customApiSolutions && (
                        <div className="flex items-center text-sm text-blue-600 dark:text-blue-400">
                            <span>✓ Custom API Solutions</span>
                        </div>
                    )}
                </div>
            </div>

            {/* Tier Lock Info */}
            {tierInfo.tierLocked && (
                <div className="mt-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-lg p-3">
                    <p className="text-sm text-yellow-700 dark:text-yellow-400">
                        🔒 Tier is locked (promotional)
                    </p>
                </div>
            )}
        </div>
    );
}
