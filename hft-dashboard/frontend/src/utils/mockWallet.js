/**
 * Mock Ethereum Provider for testing without MetaMask
 */
export const mockEthereum = {
    isMetaMask: true,
    selectedAddress: '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', // Hardhat test account #0

    request: async ({ method, params }) => {
        console.log(`[MockWallet] Request: ${method}`, params);

        switch (method) {
            case 'eth_requestAccounts':
                return ['0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266'];

            case 'personal_sign':
                // Return the bypass signature that the backend expects
                return 'MOCK_SIGNATURE_BYPASS';

            case 'eth_accounts':
                return ['0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266'];

            default:
                console.warn(`[MockWallet] Method ${method} not implemented`);
                return null;
        }
    },

    on: (eventName, callback) => {
        console.log(`[MockWallet] Listener added for: ${eventName}`);
    },

    removeListener: (eventName, callback) => {
        console.log(`[MockWallet] Listener removed for: ${eventName}`);
    }
};
