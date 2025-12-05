import React, { useState, useEffect } from 'react';
import { dashboardAPI } from '../services/api';
import DepositForm from './DepositForm';
import WithdrawForm from './WithdrawForm';
import TransactionHistory from './TransactionHistory';

function WalletPanel({ accountId }) {
    const [balance, setBalance] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeAction, setActiveAction] = useState('deposit'); // 'deposit' or 'withdraw'
    const [walletAddress, setWalletAddress] = useState(null);
    const [isConnecting, setIsConnecting] = useState(false);

    const fetchWalletData = async () => {
        try {
            setLoading(true);
            const [balanceRes, txRes, walletsRes] = await Promise.all([
                dashboardAPI.getWalletBalance(accountId),
                dashboardAPI.getWalletTransactions(accountId),
                dashboardAPI.getAccountWallets(accountId)
            ]);
            setBalance(balanceRes.data);
            setTransactions(txRes.data);

            if (walletsRes.data && walletsRes.data.length > 0) {
                setWalletAddress(walletsRes.data[0].walletAddress);
            }
        } catch (error) {
            console.error('Failed to fetch wallet data:', error);
        } finally {
            setLoading(false);
        }
    };

    const connectWallet = async () => {
        if (!window.ethereum) {
            alert('Please install MetaMask to connect wallet!');
            return;
        }

        try {
            setIsConnecting(true);

            // 1. Request accounts
            const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' });
            const address = accounts[0];

            // 2. Get nonce
            const nonceRes = await dashboardAPI.getNonce(address);
            const nonce = nonceRes.data.nonce;
            const timestamp = nonceRes.data.timestamp || Date.now();

            // 3. Sign message
            const message = `Welcome to HFT Trading Platform!\n\nSign this message to verify your wallet ownership.\n\nWallet: ${address}\nNonce: ${nonce}\nTimestamp: ${timestamp}\n\nThis request will not trigger a blockchain transaction or cost any gas fees.`;

            const signature = await window.ethereum.request({
                method: 'personal_sign',
                params: [message, address],
            });

            setWalletAddress(address);

        } catch (error) {
            console.error('Failed to connect wallet:', error);
            alert('Failed to connect wallet: ' + error.message);
        } finally {
            setIsConnecting(false);
        }
    };

    const simulateWallet = async () => {
        const { mockEthereum } = await import('../utils/mockWallet');
        window.ethereum = mockEthereum;
        await connectWallet();
    };

    useEffect(() => {
        fetchWalletData();
    }, [accountId]);

    if (loading && !balance) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Balance Card */}
            <div className="bg-gradient-to-r from-purple-600 to-indigo-600 rounded-lg shadow-lg p-6 text-white">
                <div className="flex justify-between items-start">
                    <div>
                        <h2 className="text-sm font-medium opacity-80">Total Balance (Est.)</h2>
                        <div className="text-3xl font-bold mt-1">
                            ${balance?.usdValue?.toLocaleString() || '0.00'}
                        </div>
                        {walletAddress && (
                            <div className="mt-2 flex items-center text-xs bg-black/20 rounded px-2 py-1 w-fit">
                                <span className="w-2 h-2 bg-green-400 rounded-full mr-2"></span>
                                {walletAddress.substring(0, 6)}...{walletAddress.substring(38)}
                            </div>
                        )}
                    </div>
                    <div className="text-right">
                        <div className="text-sm font-medium opacity-80">Available {balance?.asset}</div>
                        <div className="text-xl font-bold mt-1">
                            {balance?.availableBalance?.toLocaleString() || '0.00'}
                        </div>
                        {!walletAddress && (
                            <div className="flex flex-col items-end gap-2 mt-2">
                                <button
                                    onClick={connectWallet}
                                    disabled={isConnecting}
                                    className="text-xs bg-white/20 hover:bg-white/30 transition-colors rounded px-3 py-1 font-medium"
                                >
                                    {isConnecting ? 'Connecting...' : 'Connect Wallet'}
                                </button>
                                <button
                                    onClick={simulateWallet}
                                    disabled={isConnecting}
                                    className="text-xs bg-yellow-500/20 hover:bg-yellow-500/30 text-yellow-100 transition-colors rounded px-3 py-1 font-medium border border-yellow-500/30"
                                >
                                    Simulate Wallet
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Action Panel */}
                <div className="lg:col-span-1 space-y-6">
                    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-1">
                        <div className="flex">
                            <button
                                onClick={() => setActiveAction('deposit')}
                                className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${activeAction === 'deposit'
                                    ? 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-200'
                                    : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                                    }`}
                            >
                                Deposit
                            </button>
                            <button
                                onClick={() => setActiveAction('withdraw')}
                                className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${activeAction === 'withdraw'
                                    ? 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-200'
                                    : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                                    }`}
                            >
                                Withdraw
                            </button>
                        </div>
                    </div>

                    {!walletAddress ? (
                        <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4 text-yellow-200 text-sm">
                            Please connect your wallet to deposit or withdraw funds.
                        </div>
                    ) : (
                        activeAction === 'deposit' ? (
                            <DepositForm
                                accountId={accountId}
                                walletAddress={walletAddress}
                                onSuccess={fetchWalletData}
                            />
                        ) : (
                            <WithdrawForm
                                accountId={accountId}
                                walletAddress={walletAddress}
                                balance={balance}
                                onSuccess={fetchWalletData}
                            />
                        )
                    )}
                </div>

                {/* Transaction History */}
                <div className="lg:col-span-2">
                    <TransactionHistory transactions={transactions} />
                </div>
            </div>
        </div>
    );
}

export default WalletPanel;
