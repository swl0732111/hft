# Web3 Wallet Integration - Usage Guide

## Overview

The HFT system now supports Web3 wallet integration, allowing users to authenticate and trade using their blockchain wallets (MetaMask, WalletConnect, etc.).

## Features

- **Wallet Connection**: Connect Ethereum-compatible wallets
- **Signature Verification**: Secure authentication using EIP-191 standard
- **Multi-Chain Support**: Ethereum, Polygon, BSC, Arbitrum, and more
- **Account Linking**: Link multiple wallets to a single trading account
- **Nonce-based Security**: Prevent replay attacks

## API Endpoints

### 1. Get Nonce for Signing

**Request:**
```http
GET /api/wallet/nonce/0x1234567890123456789012345678901234567890
```

**Response:**
```json
{
  "nonce": "abc123-def456-ghi789",
  "walletAddress": "0x1234567890123456789012345678901234567890"
}
```

### 2. Verify Signature

**Request:**
```http
POST /api/wallet/verify
Content-Type: application/json

{
  "walletAddress": "0x1234567890123456789012345678901234567890",
  "signature": "0x...",
  "nonce": "abc123-def456-ghi789"
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Signature verified successfully"
}
```

### 3. Link Wallet to Account

**Request:**
```http
POST /api/wallet/link
Content-Type: application/json

{
  "accountId": "user-account-123",
  "walletAddress": "0x1234567890123456789012345678901234567890",
  "signature": "0x...",
  "nonce": "abc123-def456-ghi789",
  "chain": "ethereum",
  "chainId": 1
}
```

**Response:**
```json
{
  "id": "wallet-conn-456",
  "accountId": "user-account-123",
  "walletAddress": "0x1234567890123456789012345678901234567890",
  "chain": "ethereum",
  "chainId": 1,
  "status": "ACTIVE",
  "connectedAt": 1701518400000,
  "lastActiveAt": 1701518400000,
  "isPrimary": true
}
```

### 4. Get Account Wallets

**Request:**
```http
GET /api/wallet/account/user-account-123
```

**Response:**
```json
[
  {
    "id": "wallet-conn-456",
    "accountId": "user-account-123",
    "walletAddress": "0x1234567890123456789012345678901234567890",
    "chain": "ethereum",
    "chainId": 1,
    "status": "ACTIVE",
    "connectedAt": 1701518400000,
    "lastActiveAt": 1701518400000,
    "isPrimary": true,
    "label": "My MetaMask Wallet"
  }
]
```

### 5. Disconnect Wallet

**Request:**
```http
POST /api/wallet/disconnect
Content-Type: application/json

{
  "walletAddress": "0x1234567890123456789012345678901234567890",
  "chain": "ethereum"
}
```

**Response:**
```http
200 OK
```

## Frontend Integration Example (JavaScript)

### 1. Connect Wallet and Get Nonce

```javascript
// Detect MetaMask
if (typeof window.ethereum !== 'undefined') {
  console.log('MetaMask is installed!');
}

// Request account access
const accounts = await window.ethereum.request({ 
  method: 'eth_requestAccounts' 
});
const walletAddress = accounts[0];

// Get nonce from backend
const nonceResponse = await fetch(`/api/wallet/nonce/${walletAddress}`);
const { nonce } = await nonceResponse.json();
```

### 2. Sign Message

```javascript
// Build message (must match backend format)
const message = `Welcome to HFT Trading Platform!

Sign this message to verify your wallet ownership.

Wallet: ${walletAddress}
Nonce: ${nonce}
Timestamp: ${Date.now()}

This request will not trigger a blockchain transaction or cost any gas fees.`;

// Request signature from MetaMask
const signature = await window.ethereum.request({
  method: 'personal_sign',
  params: [message, walletAddress]
});
```

### 3. Verify Signature

```javascript
// Verify signature with backend
const verifyResponse = await fetch('/api/wallet/verify', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    walletAddress,
    signature,
    nonce
  })
});

const { valid, message } = await verifyResponse.json();
if (valid) {
  console.log('Wallet verified!');
}
```

### 4. Link Wallet to Account

```javascript
// Link wallet to trading account
const linkResponse = await fetch('/api/wallet/link', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    accountId: 'user-account-123',
    walletAddress,
    signature,
    nonce,
    chain: 'ethereum',
    chainId: 1
  })
});

const walletConnection = await linkResponse.json();
console.log('Wallet linked:', walletConnection);
```

## Supported Chains

| Chain | Chain ID | Network |
|-------|----------|---------|
| Ethereum Mainnet | 1 | ethereum |
| Ethereum Sepolia | 11155111 | ethereum |
| Polygon Mainnet | 137 | polygon |
| Polygon Mumbai | 80001 | polygon |
| BSC Mainnet | 56 | bsc |
| BSC Testnet | 97 | bsc |
| Arbitrum One | 42161 | arbitrum |
| Optimism | 10 | optimism |
| Base | 8453 | base |

## Security Considerations

1. **Nonce Expiry**: Nonces expire after 5 minutes
2. **Single Use**: Each nonce can only be used once
3. **Signature Verification**: Uses EIP-191 standard
4. **Checksum Addresses**: All addresses are stored in EIP-55 checksum format
5. **Rate Limiting**: Consider implementing rate limiting on nonce generation

## Database Schema

### wallet_connections
- `id`: Unique identifier
- `account_id`: Associated trading account
- `wallet_address`: Checksummed wallet address
- `chain`: Blockchain name
- `chain_id`: Numeric chain ID
- `status`: Connection status (ACTIVE, DISCONNECTED, SUSPENDED)
- `connected_at`: Connection timestamp
- `last_active_at`: Last activity timestamp
- `is_primary`: Whether this is the primary wallet
- `label`: Optional wallet label

### wallet_nonces
- `id`: Unique identifier
- `wallet_address`: Checksummed wallet address
- `nonce`: Unique nonce value
- `created_at`: Creation timestamp
- `expires_at`: Expiration timestamp
- `used`: Whether nonce has been used
- `used_at`: Usage timestamp

## Next Steps

1. **Frontend UI**: Build wallet connection UI
2. **JWT Integration**: Generate JWT tokens after successful verification
3. **Multi-Wallet Support**: Allow users to manage multiple wallets
4. **On-Chain Integration**: Add deposit/withdrawal functionality
5. **WalletConnect**: Add WalletConnect support for mobile wallets
