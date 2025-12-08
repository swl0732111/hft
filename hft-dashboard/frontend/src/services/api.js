import axios from 'axios';

const API_BASE_URL = '/api/dashboard';

export const dashboardAPI = {
    getOverview: (accountId) =>
        axios.get(`${API_BASE_URL}/overview`, { params: { accountId } }),

    getTierInfo: (accountId) =>
        axios.get(`${API_BASE_URL}/tier-info`, { params: { accountId } }),

    getVolumeChart: (accountId) =>
        axios.get(`${API_BASE_URL}/volume-chart`, { params: { accountId } }),

    getTradeDistribution: (accountId) =>
        axios.get(`${API_BASE_URL}/trade-distribution`, { params: { accountId } }),

    getProfitLoss: (accountId) =>
        axios.get(`${API_BASE_URL}/profit-loss`, { params: { accountId } }),

    getPerformanceMetrics: (accountId) =>
        axios.get(`${API_BASE_URL}/${accountId}/performance`),

    getTradeHistory: (accountId) =>
        axios.get(`${API_BASE_URL}/${accountId}/trades`),

    // Wallet API
    getWalletBalance: (accountId) =>
        axios.get(`${API_BASE_URL}/wallet/balance`, { params: { accountId } }),

    getWalletTransactions: (accountId) =>
        axios.get(`${API_BASE_URL}/wallet/transactions`, { params: { accountId } }),

    deposit: (data) =>
        axios.post(`${API_BASE_URL}/wallet/deposit`, data),

    withdraw: (data) =>
        axios.post(`${API_BASE_URL}/wallet/withdraw`, data),

    // Web3 Wallet API
    getNonce: (address) =>
        axios.get(`${API_BASE_URL.replace('/dashboard', '')}/wallet/nonce/${address}`),

    verifySignature: (data) =>
        axios.post(`${API_BASE_URL.replace('/dashboard', '')}/wallet/verify`, data),

    linkWallet: (data) =>
        axios.post(`${API_BASE_URL.replace('/dashboard', '')}/wallet/link`, data),

    getAccountWallets: (accountId) =>
        axios.get(`${API_BASE_URL.replace('/dashboard', '')}/wallet/account/${accountId}`),

    disconnectWallet: (data) =>
        axios.post(`${API_BASE_URL.replace('/dashboard', '')}/wallet/disconnect`, data),

    // Trading API
    submitOrder: (orderData) =>
        axios.post(`${API_BASE_URL.replace('/dashboard', '')}/orders`, orderData),
};
