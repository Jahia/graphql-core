import WebSocket from 'ws';

interface HandshakeOptions {
    url: string
    origin?: string
    cookie?: string
    protocol?: string
    timeout?: number
    headers?: Record<string, string>
}

interface HandshakeResult {
    status: number
    messages: string[]
    closeCode: number | null
    error?: string
}

// Opens a WebSocket to url with the given Origin, Cookie and other headers, which page scripts cannot set, and sends
// connection_init. Resolves with the handshake status and the messages received, once connection_ack arrives (the
// connection is then closed normally), once the server closes the connection (with its close code), or at the timeout.
const wsHandshake = (options: HandshakeOptions): Promise<HandshakeResult> => new Promise(resolve => {
    const timeout = options.timeout || 5000;
    const result: HandshakeResult = {status: 0, messages: [], closeCode: null};
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

    const timer = setTimeout(() => {
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
    ws.on('open', () => ws.send(JSON.stringify({type: 'connection_init', payload: {}})));
    ws.on('message', data => {
        let type;
        try {
            type = JSON.parse(data.toString()).type;
        } catch (e) {
            type = `unparsed: ${data}`;
        }

        result.messages.push(type);
        if (type === 'connection_ack') {
            settle();
            ws.close(1000);
        }
    });
    ws.on('close', code => {
        if (!settled) {
            result.closeCode = code;
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
