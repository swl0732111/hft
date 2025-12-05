import React, { useState } from 'react';
import { dashboardAPI } from '../services/api';

function WithdrawForm({ accountId, walletAddress, balance, onSuccess }) {
    const [amount, setAmount] = useState('');
    const [asset, setAsset] = useState('USDT');
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);
        setLoading(true);

        if (!amount || parseFloat(amount) <= 0) {
            setError('Please enter a valid amount');
            setLoading(false);
            return;
        }

        if (parseFloat(amount) > (balance?.availableBalance || 0)) {
            setError('Insufficient funds');
            setLoading(false);
            return;
        }

        try {
            // 1. Get Nonce
            const nonceRes = await dashboardAPI.getNonce(walletAddress);
            const nonce = nonceRes.data.nonce;
            const timestamp = nonceRes.data.timestamp || Date.now();

            // 2. Sign Message
            const message = `Welcome to HFT Trading Platform!\n\nSign this message to verify your wallet ownership.\n\nWallet: ${walletAddress}\nNonce: ${nonce}\nTimestamp: ${timestamp}\n\nThis request will not trigger a blockchain transaction or cost any gas fees.`;

            const signature = await window.ethereum.request({
                method: 'personal_sign',
                params: [message, walletAddress],
            });

            // 3. Submit Withdrawal
            await dashboardAPI.withdraw({
                accountId,
                asset,
                amount: parseFloat(amount),
                walletAddress,
                signature,
                nonce
            });

            setSuccess(`Successfully withdrew ${amount} ${asset}`);
            setAmount('');
            if (onSuccess) onSuccess();
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || err.message || 'Failed to process withdrawal');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-gray-800 rounded-lg p-6 shadow-lg border border-gray-700">
            <h3 className="text-lg font-semibold text-white mb-4">Withdraw Funds</h3>

            {error && (
                <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-4 py-2 rounded mb-4 text-sm">
                    {error}
                </div>
            )}

            {success && (
                <div className="bg-green-500/10 border border-green-500/20 text-green-400 px-4 py-2 rounded mb-4 text-sm">
                    {success}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-400 mb-1">Asset</label>
                    <select
                        value={asset}
                        onChange={(e) => setAsset(e.target.value)}
                        className="w-full bg-gray-900 border border-gray-700 rounded px-3 py-2 text-white focus:outline-none focus:border-purple-500"
                    >
                        <option value="USDT">USDT</option>
                        <option value="BTC">BTC</option>
                        <option value="ETH">ETH</option>
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-400 mb-1">Amount</label>
                    <div className="relative">
                        <input
                            type="number"
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            placeholder="0.00"
                            step="0.01"
                            min="0"
                            className="w-full bg-gray-900 border border-gray-700 rounded px-3 py-2 text-white focus:outline-none focus:border-purple-500"
                        />
                        <div className="absolute right-3 top-2 text-xs text-gray-500">
                            Max: {balance?.availableBalance?.toLocaleString() || '0.00'}
                        </div>
                    </div>
                </div>
                <div className="pt-2">
                    <button
                        type="submit"
                        disabled={loading}
                        className={`w-full py-2 px-4 rounded font-medium transition-colors ${loading
                                ? 'bg-purple-600/50 cursor-not-allowed text-white/50'
                                : 'bg-purple-600 hover:bg-purple-700 text-white'
                            }`}
                    >
                        {loading ? 'Signing & Processing...' : 'Sign & Withdraw'}
                    </button>
                    <p className="text-xs text-center text-gray-500 mt-2">
                        <span className="inline-block w-2 h-2 bg-green-500 rounded-full mr-1"></span>
                        Connected: {walletAddress.substring(0, 6)}...{walletAddress.substring(38)}
                    </p>
                </div>
            </form>
        </div >
    );
}

export default WithdrawForm;
