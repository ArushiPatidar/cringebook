/**
 * Global Notification Service for CringeBook
 * Handles WebSocket connections and notifications across all pages
 */

class NotificationService {
    constructor() {
        this.socket = null;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.currentUserId = null;
        this.notificationContainer = null;
        this.audioContext = null;
        this.currentRingtoneCall = null;
        
        // Initialize when DOM is loaded
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    async init() {
        await this.getCurrentUserId();
        this.createNotificationContainer();
        this.setupAudioContext();
        this.connect();
        
        // Reconnect on page focus
        window.addEventListener('focus', () => {
            if (!this.isConnected) {
                this.connect();
            }
        });
    }

    async getCurrentUserId() {
        try {
            const token = this.getTokenFromCookie();
            if (!token) return null;

            const response = await fetch('/api/users/current-user-id', {
                method: 'GET',
                headers: {
                    'Authorization': token
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.currentUserId = data.userId;
                return this.currentUserId;
            }
        } catch (error) {
            console.error('Error getting current user ID:', error);
        }
        return null;
    }

    getTokenFromCookie() {
        const match = document.cookie.match(/(^| )Authorization=([^;]+)/);
        return match ? decodeURIComponent(match[2]) : null;
    }

    connect() {
        if (this.isConnected || !this.currentUserId) return;

        const token = this.getTokenFromCookie();
        if (!token) return;

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/messages-ws?token=${encodeURIComponent(token)}`;

        try {
            this.socket = new WebSocket(wsUrl);
            
            this.socket.onopen = () => {
                console.log('Notification WebSocket connected');
                this.isConnected = true;
                this.reconnectAttempts = 0;
            };

            this.socket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleMessage(data);
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            };

            this.socket.onclose = () => {
                console.log('Notification WebSocket disconnected');
                this.isConnected = false;
                this.attemptReconnect();
            };

            this.socket.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.isConnected = false;
            };
        } catch (error) {
            console.error('Error creating WebSocket connection:', error);
            this.attemptReconnect();
        }
    }

    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.log('Max reconnection attempts reached');
            return;
        }

        this.reconnectAttempts++;
        console.log(`Reconnecting... Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
        
        setTimeout(() => {
            this.connect();
        }, this.reconnectDelay * this.reconnectAttempts);
    }

    handleMessage(data) {
        switch (data.type) {
            case 'notification':
                this.handleNotification(data);
                break;
            case 'new_message':
                // Handle real-time message updates if needed
                break;
            default:
                console.log('Unhandled message type:', data.type);
        }
    }

    handleNotification(data) {
        switch (data.notificationType) {
            case 'new_message':
                this.showMessageNotification(data);
                break;
            case 'video_call':
                this.showVideoCallNotification(data);
                break;
            default:
                console.log('Unknown notification type:', data.notificationType);
        }
    }

    showMessageNotification(data) {
        // Check if the chat with this specific user is currently open
        if (window.currentChatFriendId && window.currentChatFriendId === data.senderId) {
            // Don't show notification if chat with this user is open
            return;
        }
        
        this.playMessageSound();
        
        const notification = this.createNotificationPopup(
            'message',
            `New message from ${data.senderName}`,
            data.messageText.length > 50 ? data.messageText.substring(0, 50) + '...' : data.messageText,
            () => {
                window.location.href = `messages.html?friendId=${data.senderId}`;
            }
        );

        this.showNotificationPopup(notification);
    }

    showVideoCallNotification(data) {
        this.startRingtone();
        
        const notification = this.createNotificationPopup(
            'video_call',
            `Video call from ${data.fromUserName}`,
            'Click to answer the call',
            () => {
                this.stopRingtone();
                window.open(`video_call.html?friendId=${data.fromUserId}&friendName=${encodeURIComponent(data.fromUserName)}`, '_blank');
            },
            true, // persistent
            () => {
                this.stopRingtone(); // decline callback
            }
        );

        this.showNotificationPopup(notification);
        this.currentRingtoneCall = data.fromUserId;
    }

    createNotificationPopup(type, title, message, onAccept, persistent = false, onDecline = null) {
        const popup = document.createElement('div');
        popup.className = `notification-popup notification-${type}`;
        popup.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
            padding: 16px;
            max-width: 300px;
            z-index: 10000;
            border-left: 4px solid ${type === 'video_call' ? '#4CAF50' : '#2196F3'};
            animation: slideInRight 0.3s ease;
        `;

        popup.innerHTML = `
            <div style="display: flex; align-items: flex-start; gap: 12px;">
                <div style="flex: 1;">
                    <div style="font-weight: 500; color: #333; margin-bottom: 4px;">${title}</div>
                    <div style="color: #666; font-size: 14px; line-height: 1.4;">${message}</div>
                </div>
                <div style="display: flex; gap: 8px; flex-shrink: 0;">
                    ${onDecline ? `<button class="decline-btn" style="background: #f44336; color: white; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 12px;">Decline</button>` : ''}
                    <button class="accept-btn" style="background: ${type === 'video_call' ? '#4CAF50' : '#2196F3'}; color: white; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 12px;">${type === 'video_call' ? 'Answer' : 'Open'}</button>
                </div>
            </div>
        `;

        popup.querySelector('.accept-btn').onclick = () => {
            onAccept();
            this.hideNotificationPopup(popup);
        };

        if (onDecline) {
            popup.querySelector('.decline-btn').onclick = () => {
                onDecline();
                this.hideNotificationPopup(popup);
            };
        }

        if (!persistent) {
            setTimeout(() => {
                this.hideNotificationPopup(popup);
            }, 5000);
        }

        return popup;
    }

    showNotificationPopup(popup) {
        this.notificationContainer.appendChild(popup);
    }

    hideNotificationPopup(popup) {
        if (popup && popup.parentNode) {
            popup.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => {
                if (popup.parentNode) {
                    popup.parentNode.removeChild(popup);
                }
            }, 300);
        }
    }

    createNotificationContainer() {
        // Add CSS for animations
        if (!document.getElementById('notification-styles')) {
            const style = document.createElement('style');
            style.id = 'notification-styles';
            style.textContent = `
                @keyframes slideInRight {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                @keyframes slideOutRight {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        // Create container for notifications
        this.notificationContainer = document.createElement('div');
        this.notificationContainer.id = 'notification-container';
        this.notificationContainer.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 10000;
            pointer-events: none;
        `;
        this.notificationContainer.style.pointerEvents = 'none';
        
        // Enable pointer events for children
        this.notificationContainer.addEventListener('click', (e) => {
            e.target.style.pointerEvents = 'auto';
        });

        document.body.appendChild(this.notificationContainer);
    }

    setupAudioContext() {
        // Create audio context for sounds (user gesture required)
        document.addEventListener('click', () => {
            if (!this.audioContext) {
                this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            }
        }, { once: true });
    }

    playMessageSound() {
        if (!this.audioContext) return;

        try {
            // Create a short beep sound
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(this.audioContext.destination);
            
            oscillator.frequency.setValueAtTime(800, this.audioContext.currentTime);
            gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.1);
            
            oscillator.start();
            oscillator.stop(this.audioContext.currentTime + 0.1);
        } catch (error) {
            console.error('Error playing message sound:', error);
        }
    }

    startRingtone() {
        if (!this.audioContext) return;

        this.stopRingtone(); // Stop any existing ringtone
        
        this.ringtoneInterval = setInterval(() => {
            try {
                // Create a ringtone pattern
                const times = [0, 0.1, 0.3, 0.4];
                const frequencies = [800, 1000, 800, 1000];
                
                times.forEach((time, index) => {
                    const oscillator = this.audioContext.createOscillator();
                    const gainNode = this.audioContext.createGain();
                    
                    oscillator.connect(gainNode);
                    gainNode.connect(this.audioContext.destination);
                    
                    oscillator.frequency.setValueAtTime(frequencies[index], this.audioContext.currentTime + time);
                    gainNode.gain.setValueAtTime(0.2, this.audioContext.currentTime + time);
                    gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + time + 0.08);
                    
                    oscillator.start(this.audioContext.currentTime + time);
                    oscillator.stop(this.audioContext.currentTime + time + 0.08);
                });
            } catch (error) {
                console.error('Error playing ringtone:', error);
            }
        }, 2000);
    }

    stopRingtone() {
        if (this.ringtoneInterval) {
            clearInterval(this.ringtoneInterval);
            this.ringtoneInterval = null;
        }
        this.currentRingtoneCall = null;
    }

    disconnect() {
        this.stopRingtone();
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
        this.isConnected = false;
    }
}

// Initialize global notification service
window.notificationService = new NotificationService();