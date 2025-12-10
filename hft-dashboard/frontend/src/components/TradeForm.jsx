import React, { useState } from 'react';
import { dashboardAPI } from '../services/api';

const TradeForm = ({ accountId }) => {
    const [formData, setFormData] = useState({
        symbol: 'BTC-USDC',
        side: 'BUY',
        price: '',
        quantity: '',
        type: 'LIMIT'
    });
    const [status, setStatus] = useState({ type: '', message: '' });
    const [loading, setLoading] = useState(false);
    const [quoteData, setQuoteData] = useState(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleGetQuote = async () => {
        if (!formData.symbol) return;

        try {
            // Using existing dashboardAPI which should have getQuote implemented
            const response = await dashboardAPI.getQuote(formData.symbol);
            const quote = response.data;
            setQuoteData(quote);

            // Auto-fill price based on side
            // BUY -> Best Ask (Lowest price seller is willing to accept)
            // SELL -> Best Bid (Highest price buyer is willing to pay)
            const suggestion = formData.side === 'BUY' ? quote.bestAskPx : quote.bestBidPx;

            setFormData(prev => ({
                ...prev,
                price: suggestion
            }));

            setStatus({
                type: 'success',
                message: `Fetched quote for ${formData.symbol}. Best ${formData.side === 'BUY' ? 'Ask' : 'Bid'}: ${suggestion}`
            });

        } catch (error) {
            console.error(error);
            setStatus({ type: 'error', message: 'Failed to fetch quote. Is Aggregator running?' });
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setStatus({ type: '', message: '' });

        try {
            const orderData = {
                ...formData,
                accountId: accountId,
                price: parseFloat(formData.price),
                quantity: parseFloat(formData.quantity),
                walletAddress: "0x742d35Cc6634C0532925a3b844Bc454e4438f44e", // Mock address for now
                chain: "POLYGON" // Mock chain for now
            };

            await dashboardAPI.submitOrder(orderData);
            setStatus({ type: 'success', message: 'Order submitted successfully!' });
            // Reset form but keep symbol/side
            setFormData(prev => ({
                ...prev,
                price: '',
                quantity: ''
            }));
        } catch (error) {
            setStatus({
                type: 'error',
                message: error.response?.data?.message || 'Failed to submit order'
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 rounded-lg shadow-md">
            <h2 className="text-xl font-bold mb-4">Place Order</h2>

            {status.message && (
                <div className={`p-3 mb-4 rounded ${status.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                    {status.message}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Symbol</label>
                        <select
                            name="symbol"
                            value={formData.symbol}
                            onChange={(e) => {
                                handleChange(e);
                                setQuoteData(null); // Clear old quote on symbol change
                            }}
                            className="w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
                        >
                            <option value="BTC-USDC">BTC-USDC</option>
                            <option value="ETH-USDC">ETH-USDC</option>
                            <option value="SOL-USDC">SOL-USDC</option>
                            <option value="BTC/USDT">BTC/USDT</option>
                            {/* Added BTC/USDT as per test cases usually using this */}
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Side</label>
                        <div className="flex space-x-2">
                            <button
                                type="button"
                                onClick={() => setFormData(prev => ({ ...prev, side: 'BUY' }))}
                                className={`flex-1 py-2 px-4 rounded font-medium ${formData.side === 'BUY'
                                    ? 'bg-green-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                Buy
                            </button>
                            <button
                                type="button"
                                onClick={() => setFormData(prev => ({ ...prev, side: 'SELL' }))}
                                className={`flex-1 py-2 px-4 rounded font-medium ${formData.side === 'SELL'
                                    ? 'bg-red-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                Sell
                            </button>
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Price
                            {quoteData && (
                                <span className="text-xs text-gray-500 ml-2 font-normal">
                                    Spread: {quoteData.spread.toFixed(2)}
                                </span>
                            )}
                        </label>
                        <div className="flex space-x-2">
                            <input
                                type="number"
                                name="price"
                                value={formData.price}
                                onChange={handleChange}
                                placeholder="0.00"
                                step="0.01"
                                required
                                className="w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
                            />
                            <button
                                type="button"
                                onClick={handleGetQuote}
                                className="bg-blue-100 text-blue-700 px-3 py-2 rounded text-sm hover:bg-blue-200"
                                title="Get Best Market Quote"
                            >
                                Quote
                            </button>
                        </div>
                        {quoteData && (
                            <div className="text-xs text-gray-500 mt-1">
                                Bid: {quoteData.bestBidPx.toFixed(2)} | Ask: {quoteData.bestAskPx.toFixed(2)}
                            </div>
                        )}
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                        <input
                            type="number"
                            name="quantity"
                            value={formData.quantity}
                            onChange={handleChange}
                            placeholder="0.00"
                            step="0.0001"
                            required
                            className="w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={loading}
                    className={`w-full py-3 px-4 rounded font-bold text-white ${loading
                        ? 'bg-gray-400 cursor-not-allowed'
                        : formData.side === 'BUY'
                            ? 'bg-green-600 hover:bg-green-700'
                            : 'bg-red-600 hover:bg-red-700'
                        }`}
                >
                    {loading ? 'Submitting...' : `${formData.side} ${formData.symbol}`}
                </button>
            </form>
        </div>
    );
};

export default TradeForm;
