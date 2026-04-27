import { makeWASocket, useMultiFileAuthState, DisconnectReason } from '@whiskeysockets/baileys';
import localforage from 'localforage';

let sock = null;
let authState = null;
let pendingPairingCode = null;
let phoneNumber = null;

const authStore = localforage.createInstance({
    name: 'WhatsAppAuth'
});

async function loadAuthState() {
    try {
        const creds = await authStore.getItem('creds');
        const keys = await authStore.getItem('keys');
        if (creds && keys) {
            return { creds, keys };
        }
    } catch (err) {
        console.error('Error loading auth state:', err);
    }
    return { creds: null, keys: {} };
}

async function saveAuthState(state) {
    try {
        await authStore.setItem('creds', state.creds);
        await authStore.setItem('keys', state.keys);
    } catch (err) {
        console.error('Error saving auth state:', err);
    }
}

function sendToAndroid(type, data) {
    if (window.AndroidBridge) {
        window.AndroidBridge.onEvent(JSON.stringify({ type, data }));
    }
}

async function init(phone) {
    phoneNumber = phone;
    
    try {
        const loadedAuth = await loadAuthState();
        
        if (loadedAuth.creds) {
            authState = {
                creds: loadedAuth.creds,
                keys: {
                    get: (key) => loadedAuth.keys[key] || null,
                    set: (key, value) => {
                        loadedAuth.keys[key] = value;
                        saveAuthState(loadedAuth);
                    }
                }
            };
        } else {
            authState = {
                creds: null,
                keys: {
                    get: () => null,
                    set: (key, value) => {
                        loadedAuth.keys[key] = value;
                        saveAuthState(loadedAuth);
                    }
                }
            };
        }

        sock = makeWASocket({
            auth: authState,
            printQRInTerminal: false,
            browser: ['WhatsApp Scheduler', 'Android', '1.0.0'],
            logger: {
                level: 'silent',
                fatal: () => {},
                error: () => {},
                warn: () => {},
                info: () => {},
                debug: () => {},
                trace: () => {}
            }
        });

        sock.ev.on('connection.update', async (update) => {
            const { connection, lastDisconnect, qr } = update;
            
            if (connection === 'connecting') {
                sendToAndroid('connecting', { phone });
            }
            
            if (qr) {
                sendToAndroid('qr_generated', { qr });
            }
            
            if (connection === 'open') {
                const user = sock.user;
                const userPhone = user ? user.id.split(':')[0] : phone;
                sendToAndroid('connected', { phone: userPhone, name: user?.name });
                
                if (authState.creds) {
                    await saveAuthState({
                        creds: authState.creds,
                        keys: loadedAuth.keys || {}
                    });
                }
            }
            
            if (connection === 'close') {
                const statusCode = lastDisconnect?.error?.output?.statusCode;
                const shouldReconnect = statusCode !== DisconnectReason.loggedOut;
                const reason = lastDisconnect?.error?.message || 'Unknown';
                
                sendToAndroid('disconnected', { 
                    statusCode, 
                    reason,
                    shouldReconnect
                });
                
                if (shouldReconnect) {
                    setTimeout(() => init(phone), 2000);
                }
            }
        });

        sock.ev.on('creds.update', () => {
            if (sock.authState) {
                saveAuthState({
                    creds: sock.authState.creds,
                    keys: loadedAuth.keys || {}
                });
            }
        });

        return { success: true };
    } catch (err) {
        sendToAndroid('error', { message: err.message });
        return { success: false, error: err.message };
    }
}

async function requestPairingCode() {
    if (!sock || !phoneNumber) {
        return { success: false, error: 'Not initialized' };
    }
    
    try {
        const code = await sock.requestPairingCode(phoneNumber);
        pendingPairingCode = code;
        sendToAndroid('pairing_code', { code });
        return { success: true, code };
    } catch (err) {
        sendToAndroid('error', { message: err.message });
        return { success: false, error: err.message };
    }
}

async function enterPairingCode(code) {
    if (!sock) {
        return { success: false, error: 'Not initialized' };
    }
    
    try {
        if (code === pendingPairingCode) {
            sendToAndroid('pairing_code_entered', { code });
            return { success: true };
        } else {
            sendToAndroid('error', { message: 'Invalid pairing code' });
            return { success: false, error: 'Invalid pairing code' };
        }
    } catch (err) {
        sendToAndroid('error', { message: err.message });
        return { success: false, error: err.message };
    }
}

async function getChats() {
    if (!sock) {
        return { success: false, error: 'Not connected', chats: [] };
    }
    
    try {
        const chats = await sock.getChats();
        const filtered = chats
            .filter(chat => 
                !chat.id.includes('newsletter') && 
                !chat.id.includes('status@') &&
                chat.conversationTimestamp
            )
            .sort((a, b) => (b.conversationTimestamp || 0) - (a.conversationTimestamp || 0))
            .slice(0, 50)
            .map(chat => ({
                jid: chat.id,
                name: chat.name || chat.id.split('@')[0],
                isGroup: chat.id.endsWith('@g.us'),
                timestamp: chat.conversationTimestamp
            }));
        
        sendToAndroid('chats', { chats: filtered });
        return { success: true, chats: filtered };
    } catch (err) {
        sendToAndroid('error', { message: err.message });
        return { success: false, error: err.message, chats: [] };
    }
}

async function sendMessage(jid, content) {
    if (!sock) {
        return { success: false, error: 'Not connected' };
    }
    
    try {
        const result = await sock.sendMessage(jid, { text: content });
        sendToAndroid('message_sent', { jid, messageId: result.key.id });
        return { success: true, messageId: result.key.id };
    } catch (err) {
        sendToAndroid('message_failed', { jid, error: err.message });
        return { success: false, error: err.message };
    }
}

async function disconnect() {
    if (sock) {
        sock.end();
        sock = null;
    }
    await authStore.clear();
    sendToAndroid('disconnected', { statusCode: DisconnectReason.loggedOut, reason: 'User logout' });
    return { success: true };
}

function isConnected() {
    return sock && sock.ws?.readyState === 1;
}

window.WhatsApp = {
    init,
    requestPairingCode,
    enterPairingCode,
    getChats,
    sendMessage,
    disconnect,
    isConnected
};

console.log('WhatsApp client initialized');