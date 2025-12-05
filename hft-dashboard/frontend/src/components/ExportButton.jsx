import React, { useState } from 'react';
import jsPDF from 'jspdf';
import 'jspdf-autotable';
import * as XLSX from 'xlsx';

function ExportButton({ data }) {
    const [isExporting, setIsExporting] = useState(false);
    const [showMenu, setShowMenu] = useState(false);

    const { overview, tierInfo, tradeDistribution, profitLoss, performanceMetrics } = data;

    const exportPDF = async () => {
        setIsExporting(true);
        try {
            const doc = new jsPDF();
            const today = new Date().toLocaleDateString();

            // Title
            doc.setFontSize(20);
            doc.text('HFT Trading Dashboard Report', 14, 22);
            doc.setFontSize(10);
            doc.text(`Generated on: ${today}`, 14, 30);
            doc.text(`Account: ${overview?.accountId || 'N/A'}`, 14, 35);

            // 1. Overview Section
            doc.setFontSize(16);
            doc.text('Trading Overview (30 Days)', 14, 45);

            const overviewData = [
                ['Metric', 'Value'],
                ['Total Volume', `$${overview?.stats30d?.volume.toLocaleString()}`],
                ['Trade Count', overview?.stats30d?.tradeCount],
                ['Avg Trade Size', `$${overview?.stats30d?.avgTradeSize.toFixed(2)}`],
                ['Fees Paid', `$${overview?.stats30d?.feesPaid.toFixed(2)}`],
                ['Fee Savings', `$${overview?.stats30d?.feeSavings.toFixed(2)}`]
            ];

            doc.autoTable({
                startY: 50,
                head: [['Metric', 'Value']],
                body: overviewData.slice(1),
                theme: 'striped',
                headStyles: { fillColor: [139, 92, 246] }
            });

            // 2. Performance Metrics
            let finalY = doc.lastAutoTable.finalY + 15;
            doc.text('Performance Metrics', 14, finalY);

            const perfData = [
                ['Metric', 'Value'],
                ['Win Rate', `${performanceMetrics?.winRate?.winRate.toFixed(2)}%`],
                ['Profit Factor', performanceMetrics?.winRate?.profitFactor.toFixed(2)],
                ['Sharpe Ratio', performanceMetrics?.riskMetrics?.sharpeRatio.toFixed(2)],
                ['Max Drawdown', `${performanceMetrics?.drawdown?.maxDrawdownPercent.toFixed(2)}%`]
            ];

            doc.autoTable({
                startY: finalY + 5,
                head: [['Metric', 'Value']],
                body: perfData.slice(1),
                theme: 'striped',
                headStyles: { fillColor: [16, 185, 129] }
            });

            // 3. Profit & Loss
            finalY = doc.lastAutoTable.finalY + 15;
            doc.text('Profit & Loss Analysis', 14, finalY);

            const pnlData = [
                ['Metric', 'Value'],
                ['Net Profit', `$${profitLoss?.cumulativePnL?.netProfit.toFixed(2)}`],
                ['Total Realized', `$${profitLoss?.cumulativePnL?.totalRealized.toFixed(2)}`],
                ['ROI', `${profitLoss?.cumulativePnL?.roi.toFixed(2)}%`],
                ['Profitable Days', profitLoss?.cumulativePnL?.profitableDays]
            ];

            doc.autoTable({
                startY: finalY + 5,
                head: [['Metric', 'Value']],
                body: pnlData.slice(1),
                theme: 'striped',
                headStyles: { fillColor: [59, 130, 246] }
            });

            doc.save('hft-dashboard-report.pdf');
        } catch (error) {
            console.error('PDF Export failed:', error);
        } finally {
            setIsExporting(false);
            setShowMenu(false);
        }
    };

    const exportExcel = () => {
        setIsExporting(true);
        try {
            const wb = XLSX.utils.book_new();

            // Sheet 1: Overview
            const overviewWS = XLSX.utils.json_to_sheet([
                { Metric: 'Account ID', Value: overview?.accountId },
                { Metric: 'Tier', Value: tierInfo?.currentTier },
                { Metric: '30d Volume', Value: overview?.stats30d?.volume },
                { Metric: '30d Trades', Value: overview?.stats30d?.tradeCount },
                { Metric: 'Net Profit', Value: profitLoss?.cumulativePnL?.netProfit }
            ]);
            XLSX.utils.book_append_sheet(wb, overviewWS, "Overview");

            // Sheet 2: Daily P&L
            if (profitLoss?.dailyPnL) {
                const pnlWS = XLSX.utils.json_to_sheet(profitLoss.dailyPnL);
                XLSX.utils.book_append_sheet(wb, pnlWS, "Daily P&L");
            }

            // Sheet 3: Trade Distribution
            if (tradeDistribution?.hourlyDistribution) {
                const distWS = XLSX.utils.json_to_sheet(tradeDistribution.hourlyDistribution);
                XLSX.utils.book_append_sheet(wb, distWS, "Hourly Activity");
            }

            XLSX.writeFile(wb, "hft-dashboard-data.xlsx");
        } catch (error) {
            console.error('Excel Export failed:', error);
        } finally {
            setIsExporting(false);
            setShowMenu(false);
        }
    };

    const exportCSV = () => {
        // Simple CSV export of Daily P&L
        if (!profitLoss?.dailyPnL) return;

        const headers = ['Date', 'Realized P&L', 'Unrealized P&L', 'Total P&L', 'Fees Paid', 'Net P&L'];
        const rows = profitLoss.dailyPnL.map(day => [
            day.date,
            day.realizedPnL,
            day.unrealizedPnL,
            day.totalPnL,
            day.feesPaid,
            day.netPnL
        ]);

        const csvContent = [
            headers.join(','),
            ...rows.map(row => row.join(','))
        ].join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        if (link.download !== undefined) {
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', 'daily_pnl.csv');
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        setShowMenu(false);
    };

    return (
        <div className="relative inline-block text-left">
            <div>
                <button
                    type="button"
                    className="inline-flex justify-center w-full rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white dark:bg-gray-700 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-100 focus:ring-purple-500"
                    onClick={() => setShowMenu(!showMenu)}
                    disabled={isExporting}
                >
                    {isExporting ? 'Exporting...' : '📥 Export Report'}
                    <svg className="-mr-1 ml-2 h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                        <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                    </svg>
                </button>
            </div>

            {showMenu && (
                <div className="origin-top-right absolute right-0 mt-2 w-56 rounded-md shadow-lg bg-white dark:bg-gray-800 ring-1 ring-black ring-opacity-5 focus:outline-none z-50">
                    <div className="py-1" role="menu" aria-orientation="vertical" aria-labelledby="options-menu">
                        <button
                            onClick={exportPDF}
                            className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center"
                            role="menuitem"
                        >
                            <span className="mr-2">📄</span> Export as PDF
                        </button>
                        <button
                            onClick={exportExcel}
                            className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center"
                            role="menuitem"
                        >
                            <span className="mr-2">📊</span> Export as Excel
                        </button>
                        <button
                            onClick={exportCSV}
                            className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center"
                            role="menuitem"
                        >
                            <span className="mr-2">📝</span> Export CSV (Daily P&L)
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

export default ExportButton;
