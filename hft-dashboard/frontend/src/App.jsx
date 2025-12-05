import React, { useState, useEffect } from 'react';
import { dashboardAPI } from './services/api';
import websocketService from './services/websocket';
import DashboardOverview from './components/DashboardOverview';
import TierProgress from './components/TierProgress';
import VolumeChart from './components/VolumeChart';
import TradeDistribution from './components/TradeDistribution';
import ProfitLossChart from './components/ProfitLossChart';
import PerformanceMetrics from './components/PerformanceMetrics';
import WalletPanel from './components/WalletPanel';
import ExportButton from './components/ExportButton';

function App() {
    const [accountId, setAccountId] = useState('test-account-1');
    const [activeTab, setActiveTab] = useState('overview');

    // Data states
    const [overview, setOverview] = useState(null);
    const [tierInfo, setTierInfo] = useState(null);
    const [volumeChart, setVolumeChart] = useState(null);
    const [tradeDistribution, setTradeDistribution] = useState(null);
    const [profitLoss, setProfitLoss] = useState(null);
    const [performanceMetrics, setPerformanceMetrics] = useState(null);

    const [loading, setLoading] = useState(true);
    const [darkMode, setDarkMode] = useState(true);
    const [wsConnected, setWsConnected] = useState(false);
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        // Initial data load
        loadDashboardData();

        // Connect WebSocket
        websocketService.connect(accountId, () => {
            setWsConnected(true);
            console.log('WebSocket connected, real-time updates enabled');
        });

        // Set up WebSocket callbacks
        websocketService.setOverviewCallback(accountId, (data) => {
            console.log('Real-time overview update received');
            setOverview(data);
        });

        websocketService.setTierCallback(accountId, (data) => {
            console.log('Real-time tier update received');
            setTierInfo(data);
        });

        websocketService.setNotificationCallback(accountId, (notification) => {
            console.log('Real-time notification received:', notification);
            setNotifications(prev => [notification, ...prev].slice(0, 5));

            // Show browser notification
            if (Notification.permission === 'granted') {
                new Notification('HFT Trading', {
                    body: `New ${notification.type}: ${notification.symbol}`,
                    icon: '/favicon.ico'
                });
            }
        });

        // Cleanup on unmount
        return () => {
            websocketService.disconnect(accountId);
        };
    }, [accountId]);

    // Request notification permission
    useEffect(() => {
        if (Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);
            // Load core data first
            const [overviewRes, tierRes, chartRes] = await Promise.all([
                dashboardAPI.getOverview(accountId),
                dashboardAPI.getTierInfo(accountId),
                dashboardAPI.getVolumeChart(accountId),
            ]);

            setOverview(overviewRes.data);
            setTierInfo(tierRes.data);
            setVolumeChart(chartRes.data);

            // Load extended analytics data
            const [distRes, pnlRes, perfRes] = await Promise.all([
                dashboardAPI.getTradeDistribution(accountId),
                dashboardAPI.getProfitLoss(accountId),
                dashboardAPI.getPerformanceMetrics(accountId),
            ]);

            setTradeDistribution(distRes.data);
            setProfitLoss(pnlRes.data);
            setPerformanceMetrics(perfRes.data);

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    const tabs = [
        { id: 'overview', label: 'Overview' },
        { id: 'analytics', label: 'Analytics' },
        { id: 'performance', label: 'Performance' },
        { id: 'wallet', label: 'Wallet' },
    ];

    return (
        <div className={darkMode ? 'dark' : ''}>
            <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
                {/* Header */}
                <header className="bg-white dark:bg-gray-800 shadow sticky top-0 z-10">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
                        <div className="flex justify-between items-center">
                            <div className="flex items-center space-x-4">
                                <div>
                                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                                        HFT Dashboard
                                    </h1>
                                    <div className="flex items-center mt-1 space-x-2">
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${wsConnected
                                            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                                            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                                            }`}>
                                            <span className={`w-1.5 h-1.5 rounded-full mr-1.5 ${wsConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                                                }`}></span>
                                            {wsConnected ? 'Live' : 'Disconnected'}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-center space-x-4">
                                <ExportButton
                                    data={{
                                        overview,
                                        tierInfo,
                                        tradeDistribution,
                                        profitLoss,
                                        performanceMetrics
                                    }}
                                />
                                <button
                                    onClick={() => setDarkMode(!darkMode)}
                                    className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-xl"
                                    title={darkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}
                                >
                                    {darkMode ? '☀️' : '🌙'}
                                </button>
                            </div>
                        </div>

                        {/* Navigation Tabs */}
                        <div className="mt-6 border-b border-gray-200 dark:border-gray-700">
                            <nav className="-mb-px flex space-x-8">
                                {tabs.map((tab) => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setActiveTab(tab.id)}
                                        className={`${activeTab === tab.id
                                            ? 'border-purple-500 text-purple-600 dark:text-purple-400'
                                            : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300'
                                            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors`}
                                    >
                                        {tab.label}
                                    </button>
                                ))}
                            </nav>
                        </div>
                    </div>
                </header>

                {/* Notifications Banner */}
                {notifications.length > 0 && (
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4">
                        <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-3 flex items-center justify-between animate-fade-in">
                            <div className="flex items-center">
                                <span className="text-blue-800 dark:text-blue-200 text-sm font-medium mr-2">
                                    🔔 Recent Activity:
                                </span>
                                <span className="text-blue-600 dark:text-blue-300 text-sm">
                                    {notifications[0].type} - {notifications[0].symbol}
                                </span>
                            </div>
                            <button
                                onClick={() => setNotifications([])}
                                className="text-blue-500 hover:text-blue-700 text-sm"
                            >
                                Dismiss
                            </button>
                        </div>
                    </div>
                )}

                {/* Main Content */}
                <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    {loading ? (
                        <div className="flex flex-col justify-center items-center h-64">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mb-4"></div>
                            <p className="text-gray-500 dark:text-gray-400">Loading dashboard data...</p>
                        </div>
                    ) : (
                        <div className="space-y-8 animate-fade-in">
                            {activeTab === 'overview' && (
                                <>
                                    <TierProgress tierInfo={tierInfo} />
                                    <DashboardOverview stats={overview} />
                                    <VolumeChart chartData={volumeChart} />
                                </>
                            )}

                            {activeTab === 'analytics' && (
                                <>
                                    <ProfitLossChart data={profitLoss} />
                                    <TradeDistribution data={tradeDistribution} />
                                </>
                            )}

                            {activeTab === 'performance' && (
                                <PerformanceMetrics data={performanceMetrics} />
                            )}

                            {activeTab === 'wallet' && (
                                <WalletPanel accountId={accountId} />
                            )}
                        </div>
                    )}
                </main>

                {/* Footer */}
                <footer className="bg-white dark:bg-gray-800 shadow mt-auto">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
                        <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                            HFT Trading System Dashboard &copy; 2024
                        </p>
                    </div>
                </footer>
            </div>
        </div>
    );
}

export default App;
