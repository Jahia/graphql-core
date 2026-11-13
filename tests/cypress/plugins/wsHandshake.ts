import WebSocket from 'ws';

interface HandshakeOptions {
    url: string
    origin?: string
    cookie?: string
    protocol?: string
    timeout?: number
    headers?: Record<string, string>
    // When false, no connection_init is sent: the connection waits for the server to close it or for the timeout.
    init?: boolean
    // Messages sent once connection_ack arrives. The connection then stays open for wait ms before it is closed normally.
    send?: object[]
    wait?: number
}

interface HandshakeResult {
    status: number
    messages: string[]
    closeCode: number | null
    closeReason?: string
    error?: string
    received: {type: string, id?: string}[]
}

// Opens a WebSocket to url with the given Origin, Cookie and other headers, which page scripts cannot set, and sends
// connection_init. Resolves with the handshake status and the messages received, once connection_ack arrives (the
// connection is then closed normally), once the server closes the connection (with its close code and reason), or at
// the timeout.
// The options init and send change what is sent, as their declarations state.
const wsHandshake = (options: HandshakeOptions): Promise<HandshakeResult> => new Promise(resolve => {
    const timeout = options.timeout || 5000;
    const result: HandshakeResult = {status: 0, messages: [], closeCode: null, received: []};
    const headers = {...options.headers, ...(options.cookie ? {Cookie: options.cookie} : {})};
    const ws = new WebSocket(options.url, options.protocol || 'graphql-ws', {origin: options.origin, headers, handshakeTimeout: timeout});
    let settled = false;
    const settle = () => {
        if (!settled) {
            settled = true;
            clearTimeout(timer);
            resolve(result);
        }
    };

    const closeNormally = () => {
        settle();
        ws.close(1000);
    };

    let timer = setTimeout(() => {
        ws.terminate();
        settle();
    }, timeout);
    ws.on('upgrade', res => {
        result.status = res.statusCode;
    });
    ws.on('unexpected-response', (req, res) => {
        result.status = res.statusCode;
        settle();
        ws.terminate();
    });
    ws.on('open', () => {
        if (options.init !== false) {
            ws.send(JSON.stringify({type: 'connection_init', payload: {}}));
        }
    });
    ws.on('message', data => {
        let message: {type: string, id?: string};
        try {
            const parsed = JSON.parse(data.toString());
            message = {type: parsed.type, id: parsed.id};
        } catch (e) {
            message = {type: `unparsed: ${data}`};
        }

        result.messages.push(message.type);
        result.received.push(message);
        if (message.type === 'connection_ack' && options.send?.length) {
            options.send.forEach(sent => ws.send(JSON.stringify(sent)));
            clearTimeout(timer);
            timer = setTimeout(closeNormally, options.wait || 2000);
        } else if (message.type === 'connection_ack') {
            closeNormally();
        }
    });
    ws.on('close', (code, reason) => {
        if (!settled) {
            result.closeCode = code;
            result.closeReason = reason.toString();
        }

        settle();
    });
    ws.on('error', error => {
        if (!settled) {
            result.error = error.message;
        }

        settle();
    });
});

module.exports = wsHandshake;
