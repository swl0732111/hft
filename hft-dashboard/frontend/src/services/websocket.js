import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.subscriptions = new Map();
    }

    connect(accountId, onConnect) {
        if (this.connected) {
            console.log('WebSocket already connected');
            return;
        }

        this.client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log('WebSocket connected');
                this.connected = true;

                // Subscribe to dashboard updates
                this.subscribeToOverview(accountId);
                this.subscribeToTier(accountId);
                this.subscribeToNotifications(accountId);

                // Send subscription message
                this.client.publish({
                    destination: `/app/dashboard/${accountId}/subscribe`,
                    body: JSON.stringify({ accountId })
                });

                if (onConnect) onConnect();
            },

            onDisconnect: () => {
                console.log('WebSocket disconnected');
                this.connected = false;
            },

            onStompError: (frame) => {
                console.error('STOMP error:', frame);
            }
        });

        this.client.activate();
    }

    disconnect(accountId) {
        if (this.client && this.connected) {
            // Send unsubscribe message
            this.client.publish({
                destination: `/app/dashboard/${accountId}/unsubscribe`,
                body: JSON.stringify({ accountId })
            });

            // Unsubscribe from all topics
            this.subscriptions.forEach((subscription) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();

            this.client.deactivate();
            this.connected = false;
        }
    }

    subscribeToOverview(accountId, callback) {
        if (!this.client || !this.connected) return;

        const subscription = this.client.subscribe(
            `/topic/dashboard/${accountId}/overview`,
            (message) => {
                const data = JSON.parse(message.body);
                console.log('Received overview update:', data);
                if (callback) callback(data);
            }
        );

        this.subscriptions.set('overview', subscription);
    }

    subscribeToTier(accountId, callback) {
        if (!this.client || !this.connected) return;

        const subscription = this.client.subscribe(
            `/topic/dashboard/${accountId}/tier`,
            (message) => {
                const data = JSON.parse(message.body);
                console.log('Received tier update:', data);
                if (callback) callback(data);
            }
        );

        this.subscriptions.set('tier', subscription);
    }

    subscribeToNotifications(accountId, callback) {
        if (!this.client || !this.connected) return;

        const subscription = this.client.subscribe(
            `/topic/dashboard/${accountId}/notifications`,
            (message) => {
                const data = JSON.parse(message.body);
                console.log('Received notification:', data);
                if (callback) callback(data);
            }
        );

        this.subscriptions.set('notifications', subscription);
    }

    setOverviewCallback(accountId, callback) {
        this.subscribeToOverview(accountId, callback);
    }

    setTierCallback(accountId, callback) {
        this.subscribeToTier(accountId, callback);
    }

    setNotificationCallback(accountId, callback) {
        this.subscribeToNotifications(accountId, callback);
    }
}

export default new WebSocketService();
